package com.cells.backend.board;

import jakarta.persistence.*;

import java.time.Instant;

/** A member's choice to appear on room boards. No row means "shown". */
@Entity
@Table(name = "member_board_settings")
public class MemberBoardSetting {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "show_on_board", nullable = false)
    private boolean showOnBoard = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected MemberBoardSetting() {
        // required by JPA
    }

    public MemberBoardSetting(Long memberId, boolean showOnBoard) {
        this.memberId = memberId;
        this.showOnBoard = showOnBoard;
    }

    public Long getMemberId() { return memberId; }
    public boolean isShowOnBoard() { return showOnBoard; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void change(boolean showOnBoard, Instant now) {
        this.showOnBoard = showOnBoard;
        this.updatedAt = now;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberBoardSetting other)) return false;
        return memberId != null && memberId.equals(other.memberId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}