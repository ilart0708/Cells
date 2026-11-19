package com.cells.backend.participation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The strict sequential rule, as one pure function (no database, no clock). The same function answers
 * "may this member submit?" and "what status do I show?", so the two can never disagree.
 */
public final class ParticipationRules {

    private ParticipationRules() {
    }

    public record Participant(Long memberId, int number, String fullName) {
    }

    /** waitingFor is set only when the status is WAITING. */
    public record Result(Participant participant, ParticipationStatus status, Participant waitingFor) {
    }

    /**
     * @param inOrder   the room's active members, by participation number
     * @param exempt    members the leader exempted for this day
     * @param submitted members who have submitted for this devotion
     */
    public static List<Result> evaluate(List<Participant> inOrder, Set<Long> exempt, Set<Long> submitted) {
        List<Result> results = new ArrayList<>(inOrder.size());
        Participant nearestActive = null; // the nearest earlier member who is not skipped

        for (Participant person : inOrder) {
            boolean hasSubmitted = submitted.contains(person.memberId());
            boolean skipped = !hasSubmitted && exempt.contains(person.memberId());

            if (hasSubmitted) {
                results.add(new Result(person, ParticipationStatus.SUBMITTED, null));
            } else if (skipped) {
                results.add(new Result(person, ParticipationStatus.EXEMPTED, null));
            } else if (nearestActive == null || submitted.contains(nearestActive.memberId())) {
                results.add(new Result(person, ParticipationStatus.ELIGIBLE, null));
            } else {
                results.add(new Result(person, ParticipationStatus.WAITING, nearestActive));
            }

            if (!skipped) {
                nearestActive = person;
            }
        }
        return results;
    }
}