-- A member can mark key sentences inside their own answer and add a one-line takeaway.
-- Both are written once with the answer and never edited. highlights is "start-end,start-end" (character offsets
-- into content, at most 5 ranges); a leader removing a reported answer clears both.
ALTER TABLE devotion_submissions
    ADD COLUMN takeaway   VARCHAR(200),
    ADD COLUMN highlights VARCHAR(100);
