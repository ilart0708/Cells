package com.cells.backend.account;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.sql.Timestamp;

/**
 * The deletes behind "delete my account". Every statement runs inside the caller's transaction, so the account is
 * either removed completely or not at all. It only touches rows that belong to the given person.
 */
@Repository
public class AccountDataRepository {

    private final JdbcTemplate jdbc;

    public AccountDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** What a member wrote and earned. EXP first: it points at the submission. */
    public void deleteMemberContent(Long memberId) {
        jdbc.update("delete from experience_records where member_id = ?", memberId);
        jdbc.update("delete from devotion_submissions where member_id = ?", memberId);
        jdbc.update("delete from daily_exemptions where member_id = ?", memberId);
        jdbc.update("delete from member_board_settings where member_id = ?", memberId);
    }

    /** Group summaries and personal reflections the person wrote as a leader. */
    public void deleteWrittenSummaries(Long userId) {
        jdbc.update("delete from devotion_summaries where created_by = ?", userId);
    }

    /** A leader who is gone no longer belongs to a pastor's list. */
    public void deleteLeaderAssignment(Long leaderId) {
        jdbc.update("delete from pastor_leaders where leader_id = ?", leaderId);
    }

    public void deleteSignInAndDeviceData(Long userId) {
        jdbc.update("delete from notifications where user_id = ?", userId);
        jdbc.update("delete from device_tokens where user_id = ?", userId);
        jdbc.update("delete from refresh_tokens where user_id = ?", userId);
    }

    /** The invitation this person accepted holds their email address. */
    public void deleteOwnInvitations(String email) {
        jdbc.update("delete from invitations where lower(email) = lower(?)", email);
    }

    /** Invitation links this person sent that nobody has used yet stop working. */
    public void revokeOpenInvitationsSentBy(Long userId, Instant now) {
        jdbc.update("update invitations set revoked_at = ? where invited_by = ? and accepted_at is null and revoked_at is null",
                Timestamp.from(now), userId);
    }

    public boolean pastorHasLeaders(Long pastorId) {
        Integer count = jdbc.queryForObject("select count(*) from pastor_leaders where pastor_id = ?", Integer.class, pastorId);
        return count != null && count > 0;
    }

    public boolean leaderHasActiveRooms(Long leaderId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from rooms where leader_id = ? and status = 'ACTIVE'", Integer.class, leaderId);
        return count != null && count > 0;
    }
}
