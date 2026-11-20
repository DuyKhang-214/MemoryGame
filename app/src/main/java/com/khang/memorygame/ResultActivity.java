package com.khang.memorygame;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_LEVEL = "level";
    public static final String EXTRA_SCORE = "score";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_MOVES = "moves";
    public static final String EXTRA_STARS = "stars";
    public static final String EXTRA_NEW_BEST = "new_best";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_WON = "won";
    public static final String EXTRA_MAX_COMBO = "max_combo";
    public static final String EXTRA_PAIRS_FOUND = "pairs_found";
    public static final String EXTRA_PAIRS_TOTAL = "pairs_total";
    public static final String EXTRA_TWO_PLAYER = "two_player";
    public static final String EXTRA_COINS = "coins";
    public static final String EXTRA_DOUBLED = "doubled";
    public static final String EXTRA_P1 = "p1";
    public static final String EXTRA_P2 = "p2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppTheme.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });

        // Nhận dữ liệu từ GameActivity qua Intent
        Intent in = getIntent();
        final int level = in.getIntExtra(EXTRA_LEVEL, 0);
        final boolean two = in.getBooleanExtra(EXTRA_TWO_PLAYER, false);
        int score = in.getIntExtra(EXTRA_SCORE, 0);
        int time = in.getIntExtra(EXTRA_TIME, 0);
        int moves = in.getIntExtra(EXTRA_MOVES, 0);
        int stars = in.getIntExtra(EXTRA_STARS, 1);
        boolean newBest = in.getBooleanExtra(EXTRA_NEW_BEST, false);
        int mode = in.getIntExtra(EXTRA_MODE, 0);
        boolean won = in.getBooleanExtra(EXTRA_WON, true);
        int maxCombo = in.getIntExtra(EXTRA_MAX_COMBO, 0);
        int found = in.getIntExtra(EXTRA_PAIRS_FOUND, 0);
        int totalPairs = in.getIntExtra(EXTRA_PAIRS_TOTAL, 0);
        int coins = in.getIntExtra(EXTRA_COINS, 0);
        boolean doubled = in.getBooleanExtra(EXTRA_DOUBLED, false);
        int p1 = in.getIntExtra(EXTRA_P1, 0);
        int p2 = in.getIntExtra(EXTRA_P2, 0);

        TextView tvScore = findViewById(R.id.tvScore);
        TextView tvDetails = findViewById(R.id.tvDetails);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvTrophy = findViewById(R.id.tvTrophy);
        View badge = findViewById(R.id.tvNewRecord);

        // ----- Nội dung chi tiết -----
        String details;
        if (two) {
            details = "Chế độ: 2 người chơi\nĐộ khó: " + Level.NAMES[level]
                    + "\nNgười 1: " + p1 + " cặp\nNgười 2: " + p2 + " cặp"
                    + "\nSố lượt lật: " + moves;
        } else {
            details = "Chế độ: " + Level.MODES[mode] + "\nĐộ khó: " + Level.NAMES[level];
            if (won) {
                details += "\nThời gian: " + Level.formatTime(time) + "\nSố lượt lật: " + moves;
            } else {
                details += "\nĐã tìm được: " + found + "/" + totalPairs + " cặp\nSố lượt lật: " + moves;
            }
            details += "\nCombo cao nhất: x" + maxCombo;
            if (doubled) details += "\nĐã dùng: ✖2 nhân đôi điểm";
        }
        tvDetails.setText(details);

        // ----- Tiêu đề theo kết quả -----
        if (two) {
            tvTitle.setText(p1 > p2 ? "Người 1 thắng!" : p2 > p1 ? "Người 2 thắng!" : "Hòa nhau!");
            tvTrophy.setText(p1 == p2 ? "🤝" : "🏆");
            findViewById(R.id.starsRow).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvScoreLabel)).setText("TỈ SỐ");
            tvScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
            tvScore.setText(p1 + "  -  " + p2);
        } else if (!won) {
            tvTitle.setText("Hết giờ!");
            tvTrophy.setText("⏰");
            ((TextView) findViewById(R.id.btnAgain)).setText("Thử lại");
        }

        // Xu nhận được
        if (!two && coins > 0) {
            TextView tvCoins = findViewById(R.id.tvCoins);
            tvCoins.setText("💰 +" + coins + " xu" + (won ? "" : " (an ủi)"));
            tvCoins.setVisibility(View.VISIBLE);
            tvCoins.setAlpha(0f);
            tvCoins.animate().alpha(1f).setStartDelay(1200).setDuration(400).start();
        }

        // Cúp nảy lên
        tvTrophy.setScaleX(0f);
        tvTrophy.setScaleY(0f);
        tvTrophy.animate().scaleX(1f).scaleY(1f).setDuration(600)
                .setInterpolator(new OvershootInterpolator(2f)).start();

        // Điểm chạy từ 0 lên (chỉ chế độ 1 người)
        if (!two) {
            ValueAnimator counter = ValueAnimator.ofInt(0, score);
            counter.setDuration(1100);
            counter.setStartDelay(300);
            counter.addUpdateListener(a -> tvScore.setText(String.valueOf(a.getAnimatedValue())));
            counter.start();

            // Sao xuất hiện lần lượt
            int[] starIds = {R.id.star1, R.id.star2, R.id.star3};
            for (int i = 0; i < 3; i++) {
                TextView s = findViewById(starIds[i]);
                s.setTextColor(i < stars ? AppTheme.primary(this) : getColor(R.color.star_off));
                s.setScaleX(0f);
                s.setScaleY(0f);
                s.animate().scaleX(1f).scaleY(1f).setStartDelay(500 + i * 250L).setDuration(400)
                        .setInterpolator(new OvershootInterpolator(3f)).start();
            }
        }

        if (newBest) {
            badge.setVisibility(View.VISIBLE);
            badge.setAlpha(0f);
            badge.animate().alpha(1f).setStartDelay(1000).setDuration(400).start();
        }

        findViewById(R.id.btnAgain).setOnClickListener(v -> {
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra(GameActivity.EXTRA_LEVEL, level);
            i.putExtra(GameActivity.EXTRA_TWO_PLAYER, two);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
        findViewById(R.id.btnMenu).setOnClickListener(v -> backToMenu());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToMenu();
            }
        });
    }

    private void backToMenu() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }
}
