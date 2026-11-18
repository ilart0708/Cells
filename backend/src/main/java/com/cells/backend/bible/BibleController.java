package com.cells.backend.bible;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** The book list lives in one place (here), so screens never keep their own copy. */
@RestController
@RequestMapping("/api/v1/bible")
@PreAuthorize("hasRole('LEADER')")
public class BibleController {

    @GetMapping("/books")
    public List<BibleBookResponse> books() {
        return Arrays.stream(BibleBook.values())
                .map(book -> new BibleBookResponse(
                        book.name(), book.displayName(), book.chapters(), book.testament().name()))
                .toList();
    }
}