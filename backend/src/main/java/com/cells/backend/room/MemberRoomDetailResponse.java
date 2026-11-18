package com.cells.backend.room;

import java.util.List;

public record MemberRoomDetailResponse(MemberRoomResponse room, List<ParticipationEntryResponse> order) {
}