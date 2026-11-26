package com.cells.backend.participation;

import com.cells.backend.common.InvalidFieldsException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Checks, stores and reads the marked passages of an answer. Stored as text: "0-12,30-55". */
public final class Highlights {

    public static final int MAX_RANGES = 5;

    private Highlights() {
    }

    /** Sorted ranges that lie inside the text and do not overlap; otherwise a 400 on the highlights field. */
    public static List<HighlightRange> validated(List<HighlightRange> ranges, int contentLength) {
        if (ranges == null || ranges.isEmpty()) {
            return List.of();
        }
        if (ranges.size() > MAX_RANGES) {
            throw bad("You can highlight at most " + MAX_RANGES + " passages.");
        }
        List<HighlightRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(HighlightRange::start));
        int previousEnd = 0;
        for (HighlightRange range : sorted) {
            if (range.start() >= range.end() || range.end() > contentLength || range.start() < previousEnd) {
                throw bad("Your highlights do not match your answer. Please mark them again.");
            }
            previousEnd = range.end();
        }
        return sorted;
    }

    public static String encode(List<HighlightRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (HighlightRange range : ranges) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append(range.start()).append('-').append(range.end());
        }
        return text.toString();
    }

    public static List<HighlightRange> decode(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        List<HighlightRange> ranges = new ArrayList<>();
        for (String part : stored.split(",")) {
            String[] bounds = part.split("-");
            if (bounds.length == 2) {
                ranges.add(new HighlightRange(Integer.parseInt(bounds[0]), Integer.parseInt(bounds[1])));
            }
        }
        return ranges;
    }

    private static InvalidFieldsException bad(String message) {
        return new InvalidFieldsException(Map.of("highlights", message));
    }
}
