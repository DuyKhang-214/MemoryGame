package com.khang.memorygame;

/** Danh mục và giá của cửa hàng. */
public final class Shop {
    public static final int[] THEME_PRICE = {0, 100, 100, 100};
    public static final int[] COLOR_PRICE = {0, 80, 80, 80};
    public static final int[] BACK_PRICE = {0, 60, 60, 60, 60};

    // Vật phẩm dùng trong ván chơi
    public static final String HINT = "hint";
    public static final String TIME = "time";
    public static final String FREEZE = "freeze";
    public static final String PAIR = "pair";
    public static final String DOUBLE = "double";

    public static final String[] ITEM_KEYS = {HINT, TIME, FREEZE, PAIR, DOUBLE};
    public static final String[] ITEM_ICONS = {"💡", "⏳", "❄", "🎯", "✖2"};
    public static final String[] ITEM_NAMES = {
            "Lượt gợi ý", "Cộng thêm giờ", "Đóng băng thời gian", "Gợi ý 1 cặp", "Nhân đôi điểm"};
    public static final String[] ITEM_DESC = {
            "Thêm 1 lượt Gợi ý xem nhanh toàn bàn",
            "+15 giây, chỉ dùng ở chế độ Đếm ngược",
            "Dừng đồng hồ 10 giây (chơi 1 người)",
            "Làm lộ 1 cặp thẻ giống nhau trong 1,5 giây",
            "Nhân đôi điểm của ván đang chơi"};
    public static final int[] ITEM_PRICE = {20, 25, 30, 30, 50};

    public static final int TIME_BONUS = 15;     // giây
    public static final int FREEZE_SECONDS = 10; // giây

    private Shop() {}

    /** Xu nhận được khi thắng ván. */
    public static int winReward(int stars, int maxCombo, int mode) {
        return 10 + stars * 5 + maxCombo * 2 + (mode != 0 ? 5 : 0);
    }
}
