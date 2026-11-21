package com.cells.backend.history;

import com.cells.backend.room.RoomStatus;

public record HistoryRoomInfo(Long roomId, String roomName, String churchName, String leaderName, RoomStatus roomStatus) {
}