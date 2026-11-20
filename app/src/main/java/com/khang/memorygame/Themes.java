package com.khang.memorygame;

/** Các chủ đề hình trên thẻ (dùng code point emoji). */
public final class Themes {
    public static final String[] NAMES = {"Trái cây", "Động vật", "Phương tiện", "Đồ ăn"};

    public static final int[][] CARDS = {
            // Trái cây
            {0x1F34E, 0x1F34A, 0x1F34B, 0x1F347, 0x1F353, 0x1F351, 0x1F352,
                    0x1F95D, 0x1F349, 0x1F951, 0x1F34D, 0x1F34C, 0x1F350},
            // Động vật
            {0x1F436, 0x1F431, 0x1F42D, 0x1F439, 0x1F430, 0x1F98A, 0x1F43B,
                    0x1F43C, 0x1F428, 0x1F42F, 0x1F981, 0x1F42E, 0x1F437, 0x1F438},
            // Phương tiện
            {0x1F697, 0x1F695, 0x1F68C, 0x1F691, 0x1F692, 0x1F693, 0x1F69C,
                    0x1F6B2, 0x1F680, 0x1F681, 0x1F682, 0x1F6A2, 0x1F6A4, 0x1F6F5},
            // Đồ ăn
            {0x1F355, 0x1F354, 0x1F35F, 0x1F32D, 0x1F369, 0x1F36A, 0x1F366,
                    0x1F370, 0x1F36B, 0x1F37F, 0x1F950, 0x1F363, 0x1F35C, 0x1F359}
    };

    /** Mặt sau của thẻ. */
    public static final String[] BACK_NAMES = {"Dấu hỏi", "Ngôi sao", "Trái tim", "Mặt trăng", "Xúc xắc"};
    private static final int[] BACK_CODES = {0x3F, 0x2B50, 0x2764, 0x1F319, 0x1F3B2};

    private Themes() {}

    public static String back(int i) {
        return new String(Character.toChars(BACK_CODES[i]));
    }

    public static String icon(int theme) {
        return new String(Character.toChars(CARDS[theme][0]));
    }
}
