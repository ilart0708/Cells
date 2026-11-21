package com.cells.backend.stats;

import com.cells.backend.bible.BibleBook;
import com.cells.backend.church.ChurchStatus;
import com.cells.backend.stats.StatsRepository.LeaderRow;
import com.cells.backend.stats.StatsRepository.MemberCounts;
import com.cells.backend.stats.StatsRepository.RoomRow;
import com.cells.backend.user.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only queries behind the administrator's statistics. Every date decision uses the platform timezone. */
@Repository
public class AdminStatsRepository {

    public record Paged<T>(List<T> items, long total) {
    }

    public record PlatformCounts(int churches, int pastors, int disabledPastors, int leaders, int disabledLeaders) {
    }

    public record ChurchRow(long churchId, String name, ChurchStatus status, int pastors, int leaders) {
    }

    public record PastorRow(long pastorId, String fullName, String email, UserStatus status,
                            String churchName, int leaders) {
    }

    public record MemberRow(long memberId, String fullName, String email, UserStatus status,
                            String churchName, Instant memberSince) {
    }

    public record MemberTotals(long memberId, int points, int shared) {
    }

    /** devotionId is null when the room has no devotion today. */
    public record MemberRoomRow(long memberId, long roomId, String roomName, String churchName, String leaderName,
                                int number, Long devotionId, boolean shared, boolean exempt) {
    }

    private static final String ROOM_SELECT = """
            SELECT r.id AS room_id, r.name AS room_name, c.id AS church_id, c.name AS church_name,
                   r.leader_id, lu.full_name AS leader_name,
                   CAST((r.created_at AT TIME ZONE CAST(:zone AS text)) AS date) AS created_on
            FROM rooms r
            JOIN churches c ON c.id = r.church_id
            JOIN users lu ON lu.id = r.leader_id
            WHERE r.status = 'ACTIVE'
            """;

    private static final String ROOM_ORDER = " ORDER BY lower(c.name), lower(r.name), r.id";

    private static final String ROOM_SEARCH =
            " AND (lower(r.name) LIKE :pattern OR lower(c.name) LIKE :pattern OR lower(lu.full_name) LIKE :pattern)";

    // Distinct people per church or per pastor. A member belongs to one church, so the church figure never double counts.
    private static final String MEMBERS_BY_CHURCH = """
            SELECT r.church_id AS group_id,
                   count(DISTINCT rm.member_id) FILTER (WHERE u.status = 'ACTIVE') AS members,
                   count(DISTINCT rm.member_id) FILTER (WHERE u.status <> 'ACTIVE') AS disabled
            FROM rooms r
            JOIN room_members rm ON rm.room_id = r.id AND rm.removed_at IS NULL
            JOIN users u ON u.id = rm.member_id
            WHERE r.id IN (:roomIds)
            GROUP BY r.church_id
            """;

    private static final String MEMBERS_BY_PASTOR = """
            SELECT pl.pastor_id AS group_id,
                   count(DISTINCT rm.member_id) FILTER (WHERE u.status = 'ACTIVE') AS members,
                   count(DISTINCT rm.member_id) FILTER (WHERE u.status <> 'ACTIVE') AS disabled
            FROM rooms r
            JOIN pastor_leaders pl ON pl.leader_id = r.leader_id
            JOIN room_members rm ON rm.room_id = r.id AND rm.removed_at IS NULL
            JOIN users u ON u.id = rm.member_id
            WHERE r.id IN (:roomIds)
            GROUP BY pl.pastor_id
            """;

    private static final String LEADER_FROM = """
            FROM cell_group_leaders l
            JOIN users u ON u.id = l.user_id
            JOIN churches c ON c.id = l.church_id
            """;

    private static final String LEADER_SEARCH =
            " WHERE (lower(u.full_name) LIKE :pattern OR lower(u.email) LIKE :pattern OR lower(c.name) LIKE :pattern)";

    private static final String MEMBER_FROM = """
            FROM members m
            JOIN users u ON u.id = m.user_id
            JOIN churches c ON c.id = m.church_id
            """;

    private static final String MEMBER_SEARCH =
            " WHERE (lower(u.full_name) LIKE :pattern OR lower(u.email) LIKE :pattern OR lower(c.name) LIKE :pattern)";

    private final NamedParameterJdbcTemplate jdbc;
    private final String zone;

    public AdminStatsRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.zone = clock.getZone().getId();
    }

    // ---------- platform ----------

    public PlatformCounts platformCounts() {
        String sql = """
                SELECT (SELECT count(*) FROM churches) AS churches,
                       (SELECT count(*) FROM pastors) AS pastors,
                       (SELECT count(*) FROM pastors p JOIN users u ON u.id = p.user_id
                         WHERE u.status <> 'ACTIVE') AS disabled_pastors,
                       (SELECT count(*) FROM cell_group_leaders) AS leaders,
                       (SELECT count(*) FROM cell_group_leaders l JOIN users u ON u.id = l.user_id
                         WHERE u.status <> 'ACTIVE') AS disabled_leaders
                """;
        return jdbc.queryForObject(sql, params(), (rs, rowNum) -> new PlatformCounts(
                rs.getInt("churches"), rs.getInt("pastors"), rs.getInt("disabled_pastors"),
                rs.getInt("leaders"), rs.getInt("disabled_leaders")));
    }

    // ---------- rooms ----------

    public List<RoomRow> allActiveRooms() {
        return jdbc.query(ROOM_SELECT + ROOM_ORDER, params(), AdminStatsRepository::roomRow);
    }

    public Optional<RoomRow> findActiveRoom(long roomId) {
        return jdbc.query(ROOM_SELECT + " AND r.id = :roomId", params().addValue("roomId", roomId),
                AdminStatsRepository::roomRow).stream().findFirst();
    }

    public List<RoomRow> roomsOfChurches(Collection<Long> churchIds) {
        if (churchIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(ROOM_SELECT + " AND r.church_id IN (:ids)" + ROOM_ORDER,
                params().addValue("ids", churchIds), AdminStatsRepository::roomRow);
    }

    public List<RoomRow> roomsOfLeaders(Collection<Long> leaderIds) {
        if (leaderIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(ROOM_SELECT + " AND r.leader_id IN (:ids)" + ROOM_ORDER,
                params().addValue("ids", leaderIds), AdminStatsRepository::roomRow);
    }

    public Paged<RoomRow> searchRooms(String pattern, int limit, long offset) {
        String countSql = """
                SELECT count(*) FROM rooms r
                JOIN churches c ON c.id = r.church_id
                JOIN users lu ON lu.id = r.leader_id
                WHERE r.status = 'ACTIVE'
                """ + ROOM_SEARCH;
        return page(countSql, ROOM_SELECT + ROOM_SEARCH + ROOM_ORDER,
                params().addValue("pattern", pattern), limit, offset, AdminStatsRepository::roomRow);
    }

    // ---------- churches, pastors, leaders ----------

    public Paged<ChurchRow> searchChurches(String pattern, int limit, long offset) {
        String items = """
                SELECT c.id, c.name, c.status,
                       (SELECT count(*) FROM pastors p WHERE p.church_id = c.id) AS pastors,
                       (SELECT count(*) FROM cell_group_leaders l WHERE l.church_id = c.id) AS leaders
                FROM churches c
                WHERE lower(c.name) LIKE :pattern
                ORDER BY lower(c.name), c.id
                """;
        return page("SELECT count(*) FROM churches c WHERE lower(c.name) LIKE :pattern", items,
                params().addValue("pattern", pattern), limit, offset, AdminStatsRepository::churchRow);
    }

    public Paged<PastorRow> searchPastors(String pattern, int limit, long offset) {
        String from = """
                FROM pastors p
                JOIN users u ON u.id = p.user_id
                JOIN churches c ON c.id = p.church_id
                WHERE (lower(u.full_name) LIKE :pattern OR lower(u.email) LIKE :pattern OR lower(c.name) LIKE :pattern)
                """;
        String items = """
                SELECT p.user_id, u.full_name, u.email, u.status, c.name AS church_name,
                       (SELECT count(*) FROM pastor_leaders pl WHERE pl.pastor_id = p.user_id) AS leaders
                """ + from + " ORDER BY lower(u.full_name), u.id";
        return page("SELECT count(*) " + from, items,
                params().addValue("pattern", pattern), limit, offset, AdminStatsRepository::pastorRow);
    }

    /** leader id -> pastor id, for the given pastors. A leader has exactly one pastor. */
    public Map<Long, Long> pastorIdByLeader(Collection<Long> pastorIds) {
        Map<Long, Long> result = new HashMap<>();
        if (pastorIds.isEmpty()) {
            return result;
        }
        jdbc.query("SELECT leader_id, pastor_id FROM pastor_leaders WHERE pastor_id IN (:ids)",
                params().addValue("ids", pastorIds),
                (ResultSet rs) -> {
                    result.put(rs.getLong("leader_id"), rs.getLong("pastor_id"));
                });
        return result;
    }

    public Paged<LeaderRow> searchLeaders(String pattern, int limit, long offset) {
        String items = "SELECT u.id AS leader_id, u.full_name, u.email, u.status, c.name AS church_name "
                + LEADER_FROM + LEADER_SEARCH + " ORDER BY lower(u.full_name), u.id";
        return page("SELECT count(*) " + LEADER_FROM + LEADER_SEARCH, items,
                params().addValue("pattern", pattern), limit, offset, AdminStatsRepository::leaderRow);
    }

    public Optional<LeaderRow> findLeader(long leaderId) {
        String sql = "SELECT u.id AS leader_id, u.full_name, u.email, u.status, c.name AS church_name "
                + LEADER_FROM + " WHERE u.id = :leaderId";
        return jdbc.query(sql, params().addValue("leaderId", leaderId), AdminStatsRepository::leaderRow)
                .stream().findFirst();
    }

    public Map<Long, MemberCounts> memberCountsByChurch(Collection<Long> roomIds) {
        return memberCountsGrouped(MEMBERS_BY_CHURCH, roomIds);
    }

    public Map<Long, MemberCounts> memberCountsByPastor(Collection<Long> roomIds) {
        return memberCountsGrouped(MEMBERS_BY_PASTOR, roomIds);
    }

    // ---------- members ----------

    public Paged<MemberRow> searchMembers(String pattern, int limit, long offset) {
        String items = "SELECT m.user_id AS member_id, u.full_name, u.email, u.status, c.name AS church_name, "
                + "m.created_at AS member_since " + MEMBER_FROM + MEMBER_SEARCH + " ORDER BY lower(u.full_name), u.id";
        return page("SELECT count(*) " + MEMBER_FROM + MEMBER_SEARCH, items,
                params().addValue("pattern", pattern), limit, offset, AdminStatsRepository::memberRow);
    }

    public Optional<MemberRow> findMember(long memberId) {
        String sql = "SELECT m.user_id AS member_id, u.full_name, u.email, u.status, c.name AS church_name, "
                + "m.created_at AS member_since " + MEMBER_FROM + " WHERE m.user_id = :memberId";
        return jdbc.query(sql, params().addValue("memberId", memberId), AdminStatsRepository::memberRow)
                .stream().findFirst();
    }

    /** Lifetime EXP and shares. They stay when a member leaves a room or a room is archived. */
    public List<MemberTotals> memberTotals(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT m.user_id AS member_id,
                       COALESCE((SELECT sum(e.points) FROM experience_records e WHERE e.member_id = m.user_id), 0) AS points,
                       (SELECT count(*) FROM devotion_submissions s WHERE s.member_id = m.user_id) AS shared
                FROM members m
                WHERE m.user_id IN (:ids)
                """;
        return jdbc.query(sql, params().addValue("ids", memberIds), (rs, rowNum) ->
                new MemberTotals(rs.getLong("member_id"), rs.getInt("points"), rs.getInt("shared")));
    }

    /** Each member's current, active rooms, with whether they shared or were exempt on the given day. */
    public List<MemberRoomRow> memberRoomsToday(Collection<Long> memberIds, LocalDate today) {
        if (memberIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT rm.member_id, r.id AS room_id, r.name AS room_name, c.name AS church_name,
                       lu.full_name AS leader_name, rm.participation_number,
                       d.id AS devotion_id,
                       (s.id IS NOT NULL) AS shared,
                       (s.id IS NULL AND ex.id IS NOT NULL) AS exempt
                FROM room_members rm
                JOIN rooms r ON r.id = rm.room_id AND r.status = 'ACTIVE'
                JOIN churches c ON c.id = r.church_id
                JOIN users lu ON lu.id = r.leader_id
                LEFT JOIN devotions d ON d.room_id = r.id AND d.devotion_date = :today AND d.status = 'ACTIVE'
                LEFT JOIN devotion_submissions s ON s.devotion_id = d.id AND s.member_id = rm.member_id
                LEFT JOIN daily_exemptions ex ON ex.room_id = r.id AND ex.member_id = rm.member_id
                     AND ex.exemption_date = :today AND ex.revoked_at IS NULL
                WHERE rm.member_id IN (:ids) AND rm.removed_at IS NULL
                ORDER BY lower(c.name), lower(r.name), r.id
                """;
        return jdbc.query(sql, params().addValue("ids", memberIds).addValue("today", today), (rs, rowNum) ->
                new MemberRoomRow(
                        rs.getLong("member_id"), rs.getLong("room_id"), rs.getString("room_name"),
                        rs.getString("church_name"), rs.getString("leader_name"), rs.getInt("participation_number"),
                        rs.getObject("devotion_id", Long.class), rs.getBoolean("shared"), rs.getBoolean("exempt")));
    }

    /** What a member shared and when. The answer's text is never selected. */
    public Paged<AdminSubmissionRow> submissionsOf(long memberId, int limit, long offset) {
        String items = """
                SELECT s.id, s.submitted_at, d.devotion_date, d.bible_book, d.chapter, d.verse_start, d.verse_end,
                       d.title, r.id AS room_id, r.name AS room_name, c.name AS church_name,
                       COALESCE(e.points, 0) AS points
                FROM devotion_submissions s
                JOIN devotions d ON d.id = s.devotion_id
                JOIN rooms r ON r.id = s.room_id
                JOIN churches c ON c.id = r.church_id
                LEFT JOIN experience_records e ON e.submission_id = s.id
                WHERE s.member_id = :memberId
                ORDER BY s.submitted_at DESC, s.id DESC
                """;
        return page("SELECT count(*) FROM devotion_submissions s WHERE s.member_id = :memberId", items,
                params().addValue("memberId", memberId), limit, offset, AdminStatsRepository::submissionRow);
    }

    // ---------- helpers ----------

    private Map<Long, MemberCounts> memberCountsGrouped(String sql, Collection<Long> roomIds) {
        Map<Long, MemberCounts> result = new HashMap<>();
        if (roomIds.isEmpty()) {
            return result;
        }
        jdbc.query(sql, params().addValue("roomIds", roomIds), (ResultSet rs) -> {
            result.put(rs.getLong("group_id"), new MemberCounts(rs.getInt("members"), rs.getInt("disabled")));
        });
        return result;
    }

    private <T> Paged<T> page(String countSql, String itemsSql, MapSqlParameterSource parameters,
                              int limit, long offset, RowMapper<T> mapper) {
        Long total = jdbc.queryForObject(countSql, parameters, Long.class);
        List<T> items = jdbc.query(itemsSql + " LIMIT :limit OFFSET :offset",
                parameters.addValue("limit", limit).addValue("offset", offset), mapper);
        return new Paged<>(items, total == null ? 0 : total);
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource().addValue("zone", zone);
    }

    private static RoomRow roomRow(ResultSet rs, int rowNum) throws SQLException {
        return new RoomRow(
                rs.getLong("room_id"), rs.getString("room_name"), rs.getLong("church_id"), rs.getString("church_name"),
                rs.getLong("leader_id"), rs.getString("leader_name"), rs.getObject("created_on", LocalDate.class));
    }

    private static ChurchRow churchRow(ResultSet rs, int rowNum) throws SQLException {
        return new ChurchRow(
                rs.getLong("id"), rs.getString("name"), ChurchStatus.valueOf(rs.getString("status")),
                rs.getInt("pastors"), rs.getInt("leaders"));
    }

    private static PastorRow pastorRow(ResultSet rs, int rowNum) throws SQLException {
        return new PastorRow(
                rs.getLong("user_id"), rs.getString("full_name"), rs.getString("email"),
                UserStatus.valueOf(rs.getString("status")), rs.getString("church_name"), rs.getInt("leaders"));
    }

    private static LeaderRow leaderRow(ResultSet rs, int rowNum) throws SQLException {
        return new LeaderRow(
                rs.getLong("leader_id"), rs.getString("full_name"), rs.getString("email"),
                UserStatus.valueOf(rs.getString("status")), rs.getString("church_name"));
    }

    private static MemberRow memberRow(ResultSet rs, int rowNum) throws SQLException {
        return new MemberRow(
                rs.getLong("member_id"), rs.getString("full_name"), rs.getString("email"),
                UserStatus.valueOf(rs.getString("status")), rs.getString("church_name"),
                rs.getObject("member_since", OffsetDateTime.class).toInstant());
    }

    private static AdminSubmissionRow submissionRow(ResultSet rs, int rowNum) throws SQLException {
        String reference = BibleBook.valueOf(rs.getString("bible_book")).reference(
                rs.getInt("chapter"), nullableInt(rs, "verse_start"), nullableInt(rs, "verse_end"));
        return new AdminSubmissionRow(
                rs.getLong("id"), rs.getObject("submitted_at", OffsetDateTime.class).toInstant(),
                rs.getObject("devotion_date", LocalDate.class),
                rs.getLong("room_id"), rs.getString("room_name"), rs.getString("church_name"),
                reference, rs.getString("title"), rs.getInt("points"));
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}