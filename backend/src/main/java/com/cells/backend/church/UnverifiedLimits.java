package com.cells.backend.church;

/**
 * Limits for a self-registered church until an administrator verifies it. A real cell group never reaches them;
 * they only stop someone from using an unverified sign-up to scale up.
 */
public final class UnverifiedLimits {

    public static final int MAX_ROOMS = 3;
    public static final int MAX_MEMBERS = 30;

    private UnverifiedLimits() {
    }

    public static String roomsMessage() {
        return "Your church is not verified yet, so it is limited to " + MAX_ROOMS
                + " active rooms. Ask the administrator to verify your church.";
    }

    public static String membersMessage() {
        return "Your church is not verified yet, so it is limited to " + MAX_MEMBERS
                + " members. Ask the administrator to verify your church.";
    }
}
