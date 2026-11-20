package com.khang.memorygame;

import android.app.Activity;
import android.content.Context;

import com.google.android.material.color.MaterialColors;

/** Quản lý màu chủ đạo của ứng dụng (Cam, Xanh dương, Hồng, Xanh lá). */
public final class AppTheme {
    public static final String[] NAMES = {"Cam", "Xanh dương", "Hồng", "Xanh lá"};
    private static final int[] STYLES = {
            R.style.Theme_MemoryGame,
            R.style.Theme_MemoryGame_Blue,
            R.style.Theme_MemoryGame_Pink,
            R.style.Theme_MemoryGame_Green};

    private AppTheme() {}

    /** Gọi ở đầu onCreate (trước super.onCreate) của mỗi Activity. */
    public static void apply(Activity activity) {
        activity.setTheme(STYLES[new Prefs(activity).getColorTheme()]);
    }

    public static int primary(Context c) {
        return MaterialColors.getColor(c, androidx.appcompat.R.attr.colorPrimary, 0xFFFF6D00);
    }

    public static int muted(Context c) {
        return MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF888888);
    }
}
