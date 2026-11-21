package com.cells.backend.experience;

/** EXP and number of shared devotions in one room. Screens show "churchName – roomName". */
public record RoomExperienceResponse(Long roomId, String roomName, String churchName, Long points, Long submissions) {
}