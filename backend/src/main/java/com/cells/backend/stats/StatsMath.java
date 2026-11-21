package com.cells.backend.stats;

final class StatsMath {

    private StatsMath() {
    }

    /** shared / expected as a rounded percentage, or null when nothing was expected. */
    static Integer rate(int shared, int expected) {
        return expected == 0 ? null : (int) Math.round(shared * 100.0 / expected);
    }
}