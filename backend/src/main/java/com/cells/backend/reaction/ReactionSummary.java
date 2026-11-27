package com.cells.backend.reaction;

import java.util.LinkedHashMap;
import java.util.Map;

/** How many people reacted with each kind (all four kinds always present) and the caller's own choice, or null. */
public record ReactionSummary(Map<ReactionType, Long> counts, ReactionType mine) {

    public static ReactionSummary empty() {
        return of(Map.of(), null);
    }

    public static ReactionSummary of(Map<ReactionType, Long> counts, ReactionType mine) {
        Map<ReactionType, Long> all = new LinkedHashMap<>();
        for (ReactionType type : ReactionType.values()) {
            all.put(type, counts.getOrDefault(type, 0L));
        }
        return new ReactionSummary(all, mine);
    }
}
