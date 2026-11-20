package com.khang.memorygame;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Lưu cài đặt và điểm cao bằng SharedPreferences. */
public class Prefs {
    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getSharedPreferences("memory_game", Context.MODE_PRIVATE);
    }

    public boolean isSound() { return sp.getBoolean("sound", true); }
    public void setSound(boolean v) { sp.edit().putBoolean("sound", v).apply(); }

    public boolean isVibrate() { return sp.getBoolean("vibrate", true); }
    public void setVibrate(boolean v) { sp.edit().putBoolean("vibrate", v).apply(); }

    public int getDifficulty() { return sp.getInt("difficulty", 0); }
    public void setDifficulty(int d) { sp.edit().putInt("difficulty", d).apply(); }

    /** 0 = Cổ điển, 1 = Đếm ngược, 2 = Ghi nhớ nhanh. */
    public int getMode() { return sp.getInt("mode", 0); }
    public void setMode(int m) { sp.edit().putInt("mode", m).apply(); }

    /** 0 = Cam, 1 = Xanh dương, 2 = Hồng, 3 = Xanh lá. */
    public int getColorTheme() {
        int c = sp.getInt("color_theme", 0);
        return isOwned("color_" + c) ? c : 0;     // chưa mua thì dùng màu mặc định
    }
    public void setColorTheme(int c) { sp.edit().putInt("color_theme", c).apply(); }

    public int getTheme() {
        int t = sp.getInt("theme", 0);
        return isOwned("theme_" + t) ? t : 0;
    }
    public void setTheme(int t) { sp.edit().putInt("theme", t).apply(); }

    // ---------- Xu, cửa hàng, vật phẩm ----------
    public int getCoins() { return sp.getInt("coins", 5000); }   // tặng 100 xu khi mới cài

    public void addCoins(int n) { sp.edit().putInt("coins", Math.max(0, getCoins() + n)).apply(); }

    public boolean spendCoins(int n) {
        int c = getCoins();
        if (c < n) return false;
        sp.edit().putInt("coins", c - n).apply();
        return true;
    }

    /** Các món số 0 (theme_0, color_0, back_0) luôn miễn phí. */
    public boolean isOwned(String key) {
        if (key.endsWith("_0")) return true;
        return sp.getStringSet("owned", new HashSet<>()).contains(key);
    }

    public void setOwned(String key) {
        Set<String> s = new HashSet<>(sp.getStringSet("owned", new HashSet<>()));
        s.add(key);
        sp.edit().putStringSet("owned", s).apply();
    }

    public int getBackStyle() {
        int b = sp.getInt("back_style", 0);
        return isOwned("back_" + b) ? b : 0;
    }
    public void setBackStyle(int b) { sp.edit().putInt("back_style", b).apply(); }

    public int getItemCount(String key) { return sp.getInt("item_" + key, 0); }
    public void addItem(String key, int n) { sp.edit().putInt("item_" + key, getItemCount(key) + n).apply(); }

    public boolean useItem(String key) {
        int c = getItemCount(key);
        if (c <= 0) return false;
        sp.edit().putInt("item_" + key, c - 1).apply();
        return true;
    }

    // Mỗi cặp (chế độ, độ khó) có một ô điểm cao riêng
    private static int slot(int mode, int d) { return mode * 3 + d; }

    public int getBestScore(int mode, int d) { return sp.getInt("best_score_" + slot(mode, d), 0); }
    public int getBestTime(int mode, int d) { return sp.getInt("best_time_" + slot(mode, d), 0); }
    public int getBestMoves(int mode, int d) { return sp.getInt("best_moves_" + slot(mode, d), 0); }
    public int getPlayed(int mode, int d) { return sp.getInt("played_" + slot(mode, d), 0); }

    /** Lưu kết quả ván thắng. Trả về true nếu đây là điểm cao mới. */
    public boolean saveResult(int mode, int d, int score, int time, int moves) {
        int s = slot(mode, d);
        SharedPreferences.Editor e = sp.edit();
        e.putInt("played_" + s, getPlayed(mode, d) + 1);
        boolean isNew = score > getBestScore(mode, d);
        if (isNew) {
            e.putInt("best_score_" + s, score);
            e.putInt("best_time_" + s, time);
            e.putInt("best_moves_" + s, moves);
        }
        e.apply();
        return isNew;
    }

    public void clearHighScores() {
        SharedPreferences.Editor e = sp.edit();
        for (int s = 0; s < 9; s++) {
            e.remove("best_score_" + s).remove("best_time_" + s)
             .remove("best_moves_" + s).remove("played_" + s);
        }
        e.apply();
    }
}
