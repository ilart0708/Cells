package com.cells.backend.notification;

/** What a notification is about. These are the only four events that notify anyone. */
public enum NotificationType {
    /** Today's devotion is open, and the member has to wait for earlier members. */
    DEVOTION_OPEN,
    /** The member may submit now (devotion opened with them first, a member before them submitted, or an exemption). */
    YOUR_TURN,
    /** The leader saved the group summary. */
    SUMMARY_POSTED,
    /** The leader saved a personal reflection. */
    REFLECTION_POSTED
}
