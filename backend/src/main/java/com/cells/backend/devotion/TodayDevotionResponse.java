package com.cells.backend.devotion;

import java.time.LocalDate;

/** Always answers 200: "no devotion today" is normal, so devotion is null then. today is the platform's date. */
public record TodayDevotionResponse(LocalDate today, MemberDevotionResponse devotion) {
}