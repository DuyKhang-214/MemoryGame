package com.khang.memorygame;

/** Thông tin các độ khó của game. */
public final class Level {
    public static final String[] NAMES = {"Dễ", "Vừa", "Khó"};
    public static final int[] COLS = {3, 4, 4};
    public static final int[] ROWS = {4, 4, 6};
    public static final String[] MODES = {"Cổ điển", "Đếm ngược", "Ghi nhớ nhanh"};
    /** Giới hạn thời gian (giây) của chế độ đếm ngược theo độ khó. */
    public static final int[] TIME_LIMIT = {60, 90, 150};
    /** Số giây được xem trước tất cả thẻ ở chế độ Ghi nhớ nhanh. */
    public static final int[] PREVIEW = {3, 5, 8};

    private Level() {}

    public static int pairs(int level) {
        return COLS[level] * ROWS[level] / 2;
    }

    public static int maxScore(int level) {
        return pairs(level) * 150;
    }

    public static String formatTime(int seconds) {
        return String.format(java.util.Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
