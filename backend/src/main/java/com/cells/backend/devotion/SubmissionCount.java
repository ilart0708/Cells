package com.cells.backend.participation;

/** How many answers one devotion has. Used to fill a whole page of devotions with one query. */
public record SubmissionCount(Long devotionId, Long count) {
}