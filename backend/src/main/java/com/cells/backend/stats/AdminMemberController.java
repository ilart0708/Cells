package com.cells.backend.stats;

import com.cells.backend.common.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administrator only. Member monitoring: participation records, never the text of an answer. */
@RestController
@RequestMapping("/api/v1/admin/members")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final AdminMemberService memberService;

    public AdminMemberController(AdminMemberService memberService) {
        this.memberService = memberService;
    }

    /** Alphabetical. Search covers name, email and church. */
    @GetMapping
    public AdminMembersResponse list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return memberService.list(q, page, size);
    }

    @GetMapping("/{memberId}")
    public AdminMemberDetailResponse detail(@PathVariable Long memberId) {
        return memberService.detail(memberId);
    }

    /** When and what the member shared, newest first. No text. */
    @GetMapping("/{memberId}/submissions")
    public PageResponse<AdminSubmissionRow> submissions(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return memberService.submissions(memberId, page, size);
    }
}