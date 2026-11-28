package com.cells.backend.notification;

import java.util.List;
import java.util.Map;

/**
 * Writes the words of each notification. Messages say WHAT happened and link to the app; they never
 * contain anyone's answer or the leader's summary text.
 */
final class NotificationMessages {

    record Message(String subject, String heading, List<String> paragraphs, String buttonLabel,
                   String path, PushMessage push) {
    }

    private NotificationMessages() {
    }

    static Message of(NotificationDetails d) {
        String passage = d.passage();
        String what = (d.devotionTitle() == null || d.devotionTitle().isBlank())
                ? passage : passage + " (" + d.devotionTitle() + ")";
        String room = d.roomLabel();
        String roomPath = "/member/rooms/" + d.roomId() + "/devotion";
        String notePath = "/member/rooms/" + d.roomId() + "/devotions/" + d.devotionId();

        return switch (d.kind()) {
            case DEVOTION_OPEN -> new Message(
                    "Today’s devotion is open in " + room,
                    "Today’s devotion is open",
                    List.of(d.leaderName() + " shared " + what + " in " + room + ".",
                            "Read the passage now. Your turn comes after the members before you have shared, "
                                    + "and we’ll let you know."),
                    "Read the passage", roomPath,
                    push(d, "Today’s devotion is open", room + ": " + passage, roomPath));
            case YOUR_TURN -> new Message(
                    "It’s your turn in " + room,
                    "It’s your turn",
                    List.of("Today’s devotion in " + room + " is " + what + ".",
                            "You can share your understanding now."),
                    "Share your understanding", roomPath,
                    push(d, "It’s your turn", room + ": " + passage, roomPath));
            case SUMMARY_POSTED -> new Message(
                    "Group summary posted in " + room,
                    "Your leader posted a group summary",
                    List.of(d.leaderName() + " summarized what the group shared on " + what + " in " + room + "."),
                    "Read the summary", notePath,
                    push(d, "Group summary posted", room + ": " + passage, notePath));
            case REFLECTION_POSTED -> new Message(
                    "Personal reflection posted in " + room,
                    "Your leader shared a reflection",
                    List.of(d.leaderName() + " shared a personal reflection on " + what + " in " + room + "."),
                    "Read the reflection", notePath,
                    push(d, "Personal reflection posted", room + ": " + passage, notePath));
            // Phone push only (the dispatcher sends no email for this type). Who reacted is never named.
            case ANSWER_REACTED -> new Message(
                    "Someone reacted to your understanding in " + room,
                    "Someone reacted to your understanding",
                    List.of("Someone in " + room + " reacted to what you shared on " + what + "."),
                    "See the reaction", notePath,
                    push(d, "Someone reacted to your understanding", room + ": " + passage, notePath));
        };
    }

    private static PushMessage push(NotificationDetails d, String title, String body, String path) {
        return new PushMessage(title, body, Map.of(
                "type", d.kind().name(),
                "roomId", String.valueOf(d.roomId()),
                "devotionId", String.valueOf(d.devotionId()),
                "path", path));
    }
}
