package com.cells.backend.stats;

/** totals.leaders counts every cell group leader. totals.rooms and totals.members count active rooms and distinct people in them. */
public record AdminDashboardResponse(
        StatsPeriods periods, int churches, int pastors, int disabledPastors, DashboardTotalsResponse totals) {
}