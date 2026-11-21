package com.cells.backend.stats;

import com.cells.backend.bible.BibleBook;
import com.cells.backend.user.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only statistics queries. Every date decision uses the platform timezone (app.clock.zone). */
@Repository
public class StatsRepository {

    public record RoomRow(long roomId, String roomName, long churchId, String churchName,
                          long leaderId, String leaderName, LocalDate createdOn) {
    }

    public record LeaderRow(long leaderId, String fullName, String email, UserStatus status, String churchName) {
    }

    /** devotionId is null when the room has no devotion today. members and disabled count current members. */
    public record TodayRow(long roomId, Long devotionId, int members, int disabled, int shared, int exempt) {
    }

    /** present counts member-days: members whose stay contains each devotion day. */
    public record WindowRow(long roomId, int devotionDays, int present, int shared, int exempt) {
    }

    public record DayRow(LocalDate date, String reference, String title, int present, int shared, int exempt) {
    }

    public record MemberCounts(int members, int disabled) {
        public static final MemberCounts NONE = new MemberCounts(0, 0);
    }

    // The members present on a devotion day (their stay contains it, and the account is active),
    // with whether they shared and whether they were exempt that day.
    private static final String PRESENT_JOINS = """
            LEFT JOIN room_members rm ON rm.room_id = d.room_id
                 AND CAST((rm.joined_at AT TIME ZONE CAST(:zone AS text)) AS date) + 1 <= d.devotion_date
                 AND (rm.removed_at IS NULL
                      OR d.devotion_date < CAST((rm.removed_at AT TIME ZONE CAST(:zone AS text)) AS date))
            LEFT JOIN users u ON u.id = rm.member_id AND u.status = 'ACTIVE'
            LEFT JOIN devotion_submissions s ON s.devotion_id = d.id AND s.member_id = u.id
            LEFT JOIN daily_exemptions ex ON ex.room_id = d.room_id AND ex.member_id = u.id
                 AND ex.exemption_date = d.devotion_date AND ex.revoked_at IS NULL
            """;

    private static final String ROOMS_OF_PASTOR = """
            SELECT r.id AS room_id, r.name AS room_name, c.id AS church_id, c.name AS church_name,
                   r.leader_id, lu.full_name AS leader_name,
                   CAST((r.created_at AT TIME ZONE CAST(:zone AS text)) AS date) AS created_on
            FROM pastor_leaders pl
            JOIN rooms r ON r.leader_id = pl.leader_id AND r.status = 'ACTIVE'
            JOIN churches c ON c.id = r.church_id
            JOIN users lu ON lu.id = r.leader_id
            WHERE pl.pastor_id = :pastorId
            """;

    private static final String LEADERS_OF_PASTOR = """
            SELECT u.id AS leader_id, u.full_name, u.email, u.status, c.name AS church_name
            FROM pastor_leaders pl
            JOIN cell_group_leaders l ON l.user_id = pl.leader_id
            JOIN users u ON u.id = l.user_id
            JOIN churches c ON c.id = l.church_id
            WHERE pl.pastor_id = :pastorId
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final String zone;

    public StatsRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.zone = clock.getZone().getId();
    }

    // ---------- who the pastor oversees (the ownership rule lives here) ----------

    public List<LeaderRow> findLeadersOfPastor(long pastorId) {
        return jdbc.query(LEADERS_OF_PASTOR + " ORDER BY lower(u.full_name), u.id",
                params().addValue("pastorId", pastorId), StatsRepository::leaderRow);
    }

    public Optional<LeaderRow> findLeaderOfPastor(long pastorId, long leaderId) {
        return jdbc.query(LEADERS_OF_PASTOR + " AND u.id = :leaderId",
                params().addValue("pastorId", pastorId).addValue("leaderId", leaderId),
                StatsRepository::leaderRow).stream().findFirst();
    }

    public List<RoomRow> findRoomsOfPastor(long pastorId) {
        return jdbc.query(ROOMS_OF_PASTOR + " ORDER BY lower(c.name), lower(r.name), r.id",
                params().addValue("pastorId", pastorId), StatsRepository::roomRow);
    }

    public Optional<RoomRow> findRoomOfPastor(long pastorId, long roomId) {
        return jdbc.query(ROOMS_OF_PASTOR + " AND r.id = :roomId",
                params().addValue("pastorId", pastorId).addValue("roomId", roomId),
                StatsRepository::roomRow).stream().findFirst();
    }

    // ---------- today ----------

    public List<TodayRow> todayRows(Collection<Long> roomIds, LocalDate today) {
        if (roomIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT r.id AS room_id,
                       d.id AS devotion_id,
                       count(rm.id) FILTER (WHERE u.status = 'ACTIVE') AS members,
                       count(rm.id) FILTER (WHERE u.status <> 'ACTIVE') AS disabled,
                       count(rm.id) FILTER (WHERE u.status = 'ACTIVE' AND s.id IS NOT NULL) AS shared,
                       count(rm.id) FILTER (WHERE u.status = 'ACTIVE' AND s.id IS NULL AND ex.id IS NOT NULL) AS exempt
                FROM rooms r
                LEFT JOIN devotions d ON d.room_id = r.id AND d.devotion_date = :today AND d.status = 'ACTIVE'
                LEFT JOIN room_members rm ON rm.room_id = r.id AND rm.removed_at IS NULL
                LEFT JOIN users u ON u.id = rm.member_id
                LEFT JOIN devotion_submissions s ON s.devotion_id = d.id AND s.member_id = rm.member_id
                LEFT JOIN daily_exemptions ex ON ex.room_id = r.id AND ex.member_id = rm.member_id
                     AND ex.exemption_date = :today AND ex.revoked_at IS NULL
                WHERE r.id IN (:roomIds)
                GROUP BY r.id, d.id
                """;
        return jdbc.query(sql, params().addValue("roomIds", roomIds).addValue("today", today),
                StatsRepository::todayRow);
    }

    // ---------- completed days ----------

    public List<WindowRow> windowRows(Collection<Long> roomIds, LocalDate fromDate, LocalDate toDate) {
        if (roomIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT d.room_id,
                       count(DISTINCT d.id) AS devotion_days,
                       count(u.id) AS present,
                       count(u.id) FILTER (WHERE s.id IS NOT NULL) AS shared,
                       count(u.id) FILTER (WHERE s.id IS NULL AND ex.id IS NOT NULL) AS exempt
                FROM devotions d
                """ + PRESENT_JOINS + """
                WHERE d.room_id IN (:roomIds) AND d.status = 'ACTIVE'
                  AND d.devotion_date BETWEEN :fromDate AND :toDate
                GROUP BY d.room_id
                """;
        return jdbc.query(sql,
                params().addValue("roomIds", roomIds).addValue("fromDate", fromDate).addValue("toDate", toDate),
                StatsRepository::windowRow);
    }

    public List<DayRow> dayRows(long roomId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
                SELECT d.devotion_date, d.bible_book, d.chapter, d.verse_start, d.verse_end, d.title,
                       count(u.id) AS present,
                       count(u.id) FILTER (WHERE s.id IS NOT NULL) AS shared,
                       count(u.id) FILTER (WHERE s.id IS NULL AND ex.id IS NOT NULL) AS exempt
                FROM devotions d
                """ + PRESENT_JOINS + """
                WHERE d.room_id = :roomId AND d.status = 'ACTIVE'
                  AND d.devotion_date BETWEEN :fromDate AND :toDate
                GROUP BY d.id, d.devotion_date, d.bible_book, d.chapter, d.verse_start, d.verse_end, d.title
                ORDER BY d.devotion_date DESC, d.id DESC
                """;
        return jdbc.query(sql,
                params().addValue("roomId", roomId).addValue("fromDate", fromDate).addValue("toDate", toDate),
                StatsRepository::dayRow);
    }

    // ---------- distinct people (a person in two rooms counts once) ----------

    public Map<Long, MemberCounts> memberCountsByLeader(Collection<Long> roomIds) {
        Map<Long, MemberCounts> result = new HashMap<>();
        if (roomIds.isEmpty()) {
            return result;
        }
        String sql = """
                SELECT r.leader_id,
                       count(DISTINCT rm.member_id) FILTER (WHERE u.status = 'ACTIVE') AS members,
                       count(DISTINCT rm.member_id) FILTER (WHERE u.status <> 'ACTIVE') AS disabled
                FROM rooms r
                JOIN room_members rm ON rm.room_id = r.id AND rm.removed_at IS NULL
                JOIN users u ON u.id = rm.member_id
                WHERE r.id IN (:roomIds)
                GROUP BY r.leader_id
                """;
        jdbc.query(sql, params().addValue("roomIds", roomIds), (ResultSet rs) -> {
            result.put(rs.getLong("leader_id"), new MemberCounts(rs.getInt("members"), rs.getInt("disabled")));
        });
        return result;
    }

    public MemberCounts memberCounts(Collection<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return MemberCounts.NONE;
        }
        String sql = """
                SELECT count(DISTINCT rm.member_id) FILTER (WHERE u.status = 'ACTIVE') AS members,
                       count(DISTINCT rm.member_id) FILTER (WHERE u.status <> 'ACTIVE') AS disabled
                FROM room_members rm
                JOIN users u ON u.id = rm.member_id
                WHERE rm.room_id IN (:roomIds) AND rm.removed_at IS NULL
                """;
        return jdbc.queryForObject(sql, params().addValue("roomIds", roomIds),
                (rs, rowNum) -> new MemberCounts(rs.getInt("members"), rs.getInt("disabled")));
    }

    // ---------- helpers ----------

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource().addValue("zone", zone);
    }

    private static RoomRow roomRow(ResultSet rs, int rowNum) throws SQLException {
        return new RoomRow(
                rs.getLong("room_id"), rs.getString("room_name"), rs.getLong("church_id"), rs.getString("church_name"),
                rs.getLong("leader_id"), rs.getString("leader_name"), rs.getObject("created_on", LocalDate.class));
    }

    private static LeaderRow leaderRow(ResultSet rs, int rowNum) throws SQLException {
        return new LeaderRow(
                rs.getLong("leader_id"), rs.getString("full_name"), rs.getString("email"),
                UserStatus.valueOf(rs.getString("status")), rs.getString("church_name"));
    }

    private static TodayRow todayRow(ResultSet rs, int rowNum) throws SQLException {
        return new TodayRow(
                rs.getLong("room_id"), rs.getObject("devotion_id", Long.class),
                rs.getInt("members"), rs.getInt("disabled"), rs.getInt("shared"), rs.getInt("exempt"));
    }

    private static WindowRow windowRow(ResultSet rs, int rowNum) throws SQLException {
        return new WindowRow(
                rs.getLong("room_id"), rs.getInt("devotion_days"),
                rs.getInt("present"), rs.getInt("shared"), rs.getInt("exempt"));
    }

    private static DayRow dayRow(ResultSet rs, int rowNum) throws SQLException {
        String reference = BibleBook.valueOf(rs.getString("bible_book")).reference(
                rs.getInt("chapter"), nullableInt(rs, "verse_start"), nullableInt(rs, "verse_end"));
        return new DayRow(
                rs.getObject("devotion_date", LocalDate.class), reference, rs.getString("title"),
                rs.getInt("present"), rs.getInt("shared"), rs.getInt("exempt"));
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}