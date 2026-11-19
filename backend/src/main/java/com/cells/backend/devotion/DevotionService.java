package com.cells.backend.devotion;

import com.cells.backend.bible.BibleBook;
import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.InvalidFieldsException;
import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.participation.DevotionSubmissionRepository;
import com.cells.backend.participation.SubmissionCount;
import com.cells.backend.room.Room;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Every method takes the leader's id from the token and only ever touches that leader's rooms. */
@Service
public class DevotionService {

    private static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_DAYS_AHEAD = 14;

    private final DevotionRepository devotionRepository;
    private final DevotionSubmissionRepository submissionRepository;
    private final RoomRepository roomRepository;
    private final Clock clock;

    public DevotionService(DevotionRepository devotionRepository,
                           DevotionSubmissionRepository submissionRepository,
                           RoomRepository roomRepository,
                           Clock clock) {
        this.devotionRepository = devotionRepository;
        this.submissionRepository = submissionRepository;
        this.roomRepository = roomRepository;
        this.clock = clock;
    }

    @Transactional
    public DevotionResponse create(Long leaderId, Long roomId, DevotionRequest request) {
        requireActive(ownRoom(leaderId, roomId));
        LocalDate today = today();
        validate(request, today);

        if (devotionRepository.existsByRoomIdAndDevotionDateAndStatus(roomId, request.date(), DevotionStatus.ACTIVE)) {
            throw new DuplicateResourceException("This room already has a devotion for that day.");
        }

        Devotion saved = devotionRepository.save(new Devotion(
                roomId, request.date(), request.book(), request.chapter(),
                request.verseStart(), normalizedEnd(request), blankToNull(request.title()),
                request.learningLevel(), blankToNull(request.instructions()), leaderId));
        return DevotionResponse.from(saved, today, 0);
    }

    @Transactional(readOnly = true)
    public PageResponse<DevotionResponse> list(Long leaderId, Long roomId, DevotionFilter filter, int page, int size) {
        ownRoom(leaderId, roomId);
        LocalDate today = today();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "devotionDate").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Devotion> found = devotionRepository.findByRoomIdAndStatusIn(roomId, filter.statuses(), pageable);
        Map<Long, Long> answers = answerCounts(found.getContent());
        return PageResponse.from(found.map(devotion -> DevotionResponse.from(
                devotion, today, answers.getOrDefault(devotion.getId(), 0L).intValue())));
    }

    @Transactional(readOnly = true)
    public DevotionResponse get(Long leaderId, Long roomId, Long devotionId) {
        ownRoom(leaderId, roomId);
        Devotion devotion = find(roomId, devotionId);
        return DevotionResponse.from(devotion, today(), answerCount(devotionId));
    }

    @Transactional
    public DevotionResponse update(Long leaderId, Long roomId, Long devotionId, DevotionRequest request) {
        // The room lock is the one a member's submission takes, so "has anyone answered?" below is
        // true at this very moment and cannot change before we are done.
        requireActive(lockOwnRoom(leaderId, roomId));
        Devotion devotion = find(roomId, devotionId);
        LocalDate today = today();

        if (!isEditable(devotion, today)) {
            throw new BusinessRuleException("This devotion can no longer be changed.");
        }
        if (answerCount(devotionId) > 0) {
            throw new BusinessRuleException(
                    "Members have already shared their understanding of this devotion, so it can no longer be changed.");
        }
        validate(request, today);

        if (!request.date().equals(devotion.getDevotionDate())) {
            if (!devotion.getDevotionDate().isAfter(today)) {
                throw new InvalidFieldsException(Map.of("date",
                        "Today\u2019s devotion cannot be moved to another day. Cancel it and create a new one."));
            }
            if (devotionRepository.existsByRoomIdAndDevotionDateAndStatusAndIdNot(
                    roomId, request.date(), DevotionStatus.ACTIVE, devotionId)) {
                throw new DuplicateResourceException("This room already has a devotion for that day.");
            }
        }

        devotion.update(
                request.date(), request.book(), request.chapter(),
                request.verseStart(), normalizedEnd(request), blankToNull(request.title()),
                request.learningLevel(), blankToNull(request.instructions()), Instant.now());
        return DevotionResponse.from(devotion, today, 0);
    }

    @Transactional
    public DevotionResponse setStatus(Long leaderId, Long roomId, Long devotionId, DevotionStatus status) {
        requireActive(lockOwnRoom(leaderId, roomId));
        Devotion devotion = find(roomId, devotionId);
        LocalDate today = today();
        Instant now = Instant.now();
        int answers = answerCount(devotionId);

        if (status == DevotionStatus.CANCELLED) {
            if (devotion.getStatus() == DevotionStatus.ACTIVE) {
                if (!isEditable(devotion, today)) {
                    throw new BusinessRuleException(
                            "Devotions from earlier days are part of the history and cannot be cancelled.");
                }
                if (answers > 0) {
                    throw new BusinessRuleException(
                            "Members have already shared their understanding of this devotion, so it can no longer be cancelled.");
                }
                devotion.cancel(now);
            }
        } else if (devotion.getStatus() == DevotionStatus.CANCELLED) {
            if (devotion.getDevotionDate().isBefore(today)) {
                throw new BusinessRuleException("A devotion from an earlier day cannot be restored.");
            }
            if (devotionRepository.existsByRoomIdAndDevotionDateAndStatus(
                    roomId, devotion.getDevotionDate(), DevotionStatus.ACTIVE)) {
                throw new DuplicateResourceException(
                        "Another devotion already exists for that day. Cancel it first, then restore this one.");
            }
            devotion.restore(now);
        }
        return DevotionResponse.from(devotion, today, answers);
    }

    // ---------- rules ----------

    private void validate(DevotionRequest request, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.date().isBefore(today)) {
            errors.put("date", "Choose today or a later day.");
        } else if (request.date().isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            errors.put("date", "You can prepare devotions up to " + MAX_DAYS_AHEAD + " days ahead.");
        }

        BibleBook book = request.book();
        if (request.chapter() > book.chapters()) {
            errors.put("chapter", book.displayName() + " has " + book.chapters()
                    + (book.chapters() == 1 ? " chapter." : " chapters."));
        }

        // Verse numbers are only capped at 176: verse counts differ between translations.
        if (request.verseStart() == null && request.verseEnd() != null) {
            errors.put("verseStart", "Enter the first verse too, or clear the last verse.");
        } else if (request.verseStart() != null && request.verseEnd() != null
                && request.verseEnd() < request.verseStart()) {
            errors.put("verseEnd", "The last verse cannot come before the first.");
        }

        if (!errors.isEmpty()) {
            throw new InvalidFieldsException(errors);
        }
    }

    private static boolean isEditable(Devotion devotion, LocalDate today) {
        return devotion.getStatus() == DevotionStatus.ACTIVE && !devotion.getDevotionDate().isBefore(today);
    }

    // "John 3:16-16" is just "John 3:16".
    private static Integer normalizedEnd(DevotionRequest request) {
        boolean sameVerse = request.verseStart() != null && request.verseStart().equals(request.verseEnd());
        return sameVerse ? null : request.verseEnd();
    }

    private static String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    // ---------- lookups ----------

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private int answerCount(Long devotionId) {
        return (int) submissionRepository.countByDevotionId(devotionId);
    }

    // One query for a whole page, so listing does not ask once per devotion.
    private Map<Long, Long> answerCounts(List<Devotion> devotions) {
        if (devotions.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = devotions.stream().map(Devotion::getId).toList();
        return submissionRepository.countPerDevotion(ids).stream()
                .collect(Collectors.toMap(SubmissionCount::devotionId, SubmissionCount::count, Long::sum));
    }

    // Another leader's room looks exactly like a room that does not exist.
    private Room ownRoom(Long leaderId, Long roomId) {
        return roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
    }

    // The same row lock a member's submission takes, so the two always take turns.
    private Room lockOwnRoom(Long leaderId, Long roomId) {
        return roomRepository.findOwnForUpdate(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
    }

    private static void requireActive(Room room) {
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BusinessRuleException("Archived rooms cannot be changed.");
        }
    }

    private Devotion find(Long roomId, Long devotionId) {
        return devotionRepository.findByIdAndRoomId(devotionId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Devotion not found."));
    }
}