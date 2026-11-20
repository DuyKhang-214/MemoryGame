package com.khang.memorygame;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_LEVEL = "level";
    public static final String EXTRA_TWO_PLAYER = "two_player";

    private static final int MAX_HINTS = 2;
    private static final int COMBO_TIME_BONUS = 2;   // giây cộng thêm khi combo (đếm ngược)
    private static final int HINT_PENALTY = 10;      // giây bị trừ/cộng khi dùng gợi ý

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private Feedback feedback;

    private MaterialToolbar toolbar;
    private FrameLayout boardHost;
    private View pauseOverlay;
    private TextView tvTime, tvTimeLabel, tvMoves, tvMovesLabel, tvPairs, tvPairsLabel, tvBest, tvCombo;
    private MaterialButton btnHint;

    private int level, mode, theme, rows, cols, total, pairs;
    private TextView[] cards;
    private String[] faces;
    private boolean[] matched, faceUp;
    private int first = -1;
    private boolean busy, started, finished, paused;
    private int moves, matchedPairs, seconds, timeLeft, hintsLeft;
    private int combo, maxCombo, comboBonus;
    private boolean twoPlayer;
    private int freezeLeft;               // số giây còn lại của hiệu ứng đóng băng
    private boolean doubleActive;         // ván này đã bật nhân đôi điểm
    private String infoBase = "";
    private String backSymbol = "?";
    private View itemsBar;
    private MaterialButton btnItemTime, btnItemFreeze, btnItemPair, btnItemDouble;
    private int turn, p1, p2;     // chế độ 2 người: lượt hiện tại (0/1) và số cặp của mỗi người

    /** Đồng hồ chạy mỗi giây: đếm lên (Cổ điển) hoặc đếm xuống (Đếm ngược). */
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (freezeLeft > 0) {           // đang đóng băng: đồng hồ đứng yên
                freezeLeft--;
                refreshTime();
                handler.postDelayed(this, 1000);
                return;
            }
            seconds++;
            if (mode == 1) {
                timeLeft--;
                refreshTime();
                if (timeLeft <= 0) {
                    timeUp();
                    return;
                }
            } else {
                refreshTime();
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppTheme.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });

        prefs = new Prefs(this);
        feedback = new Feedback(this, prefs);
        level = getIntent().getIntExtra(EXTRA_LEVEL, prefs.getDifficulty());
        mode = prefs.getMode();
        twoPlayer = getIntent().getBooleanExtra(EXTRA_TWO_PLAYER, false);
        theme = prefs.getTheme();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        boardHost = findViewById(R.id.boardHost);
        pauseOverlay = findViewById(R.id.pauseOverlay);
        tvTime = findViewById(R.id.tvTime);
        tvTimeLabel = findViewById(R.id.tvTimeLabel);
        tvMoves = findViewById(R.id.tvMoves);
        tvMovesLabel = findViewById(R.id.tvMovesLabel);
        tvPairsLabel = findViewById(R.id.tvPairsLabel);
        tvPairs = findViewById(R.id.tvPairs);
        tvBest = findViewById(R.id.tvBest);
        tvCombo = findViewById(R.id.tvCombo);
        btnHint = findViewById(R.id.btnHint);

        findViewById(R.id.btnRestart).setOnClickListener(v -> startGame());
        btnHint.setOnClickListener(v -> useHint());

        itemsBar = findViewById(R.id.itemsBar);
        btnItemTime = findViewById(R.id.btnItemTime);
        btnItemFreeze = findViewById(R.id.btnItemFreeze);
        btnItemPair = findViewById(R.id.btnItemPair);
        btnItemDouble = findViewById(R.id.btnItemDouble);
        btnItemTime.setOnClickListener(v -> useTimeItem());
        btnItemFreeze.setOnClickListener(v -> useFreezeItem());
        btnItemPair.setOnClickListener(v -> usePairItem());
        btnItemDouble.setOnClickListener(v -> useDoubleItem());
        itemsBar.setVisibility(twoPlayer ? View.GONE : View.VISIBLE);

        // Các nút trong màn hình Tạm dừng
        findViewById(R.id.btnResume).setOnClickListener(v -> resumeGame());
        findViewById(R.id.btnPauseRestart).setOnClickListener(v -> startGame());
        findViewById(R.id.btnPauseExit).setOnClickListener(v -> exit());

        // Context Menu: nhấn giữ vào bảng thống kê hoặc khu vực chơi
        registerForContextMenu(findViewById(R.id.statsCard));
        registerForContextMenu(boardHost);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (paused) {
                    resumeGame();
                } else if (started && !finished) {
                    pauseGame();
                    new MaterialAlertDialogBuilder(GameActivity.this)
                            .setTitle("Thoát ván chơi?")
                            .setMessage("Tiến trình của ván này sẽ bị mất.")
                            .setNegativeButton("Chơi tiếp", (d, w) -> resumeGame())
                            .setPositiveButton("Thoát", (d, w) -> exit())
                            .setOnCancelListener(d -> resumeGame())
                            .show();
                } else {
                    exit();
                }
            }
        });

        startGame();
    }

    private void exit() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    // ---------- Khởi tạo ván chơi ----------
    private void startGame() {
        handler.removeCallbacksAndMessages(null);
        cols = Level.COLS[level];
        rows = Level.ROWS[level];
        total = cols * rows;
        pairs = total / 2;

        moves = 0;
        matchedPairs = 0;
        seconds = 0;
        timeLeft = Level.TIME_LIMIT[level];
        combo = 0;
        maxCombo = 0;
        comboBonus = 0;
        first = -1;
        started = false;
        finished = false;
        paused = false;
        busy = true;
        turn = 0;
        p1 = 0;
        p2 = 0;
        hintsLeft = (twoPlayer || mode == 2) ? 0 : MAX_HINTS;
        freezeLeft = 0;
        doubleActive = false;
        backSymbol = Themes.back(prefs.getBackStyle());
        matched = new boolean[total];
        faceUp = new boolean[total];

        pauseOverlay.setVisibility(View.GONE);
        boardHost.setVisibility(View.VISIBLE);
        tvCombo.animate().cancel();
        tvCombo.setVisibility(View.GONE);

        // Chọn ngẫu nhiên các hình theo chủ đề rồi nhân đôi
        List<Integer> pool = new ArrayList<>();
        for (int code : Themes.CARDS[theme]) pool.add(code);
        Collections.shuffle(pool);
        List<String> deck = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            String s = new String(Character.toChars(pool.get(i)));
            deck.add(s);
            deck.add(s);
        }
        Collections.shuffle(deck);
        faces = deck.toArray(new String[0]);

        if (twoPlayer) {
            getSupportActionBar().setTitle("2 người · " + Level.NAMES[level]);
            tvTimeLabel.setText("👤 Người 1");
            tvMovesLabel.setText("Đến lượt");
            tvPairsLabel.setText("👤 Người 2");
            tvBest.setText("Ai tìm được nhiều cặp hơn sẽ thắng  ·  " + Themes.NAMES[theme]);
        } else {
            getSupportActionBar().setTitle(Level.MODES[mode] + " · " + Level.NAMES[level]);
            tvTimeLabel.setText(mode == 1 ? "⏳ Còn lại" : "⏱ Thời gian");
            tvMovesLabel.setText("👆 Lượt lật");
            tvPairsLabel.setText("✅ Cặp");
            int best = prefs.getBestScore(mode, level);
            String bestText = best == 0 ? "Chưa có kỷ lục ở chế độ này" : "🏆 Kỷ lục: " + best + " điểm";
            if (mode == 1) bestText += "  ·  Giới hạn " + Level.TIME_LIMIT[level] + "s";
            tvBest.setText(bestText + "  ·  " + Themes.NAMES[theme]);
        }
        infoBase = tvBest.getText().toString();
        refreshInfoLine();
        refreshTime();
        updateStats();
        updateHintButton();
        refreshItems();

        buildBoard();
    }

    /** Dựng lưới thẻ hoàn toàn bằng mã Java. */
    private void buildBoard() {
        boardHost.removeAllViews();
        int w = boardHost.getWidth(), h = boardHost.getHeight();
        if (w == 0 || h == 0) {            // chưa layout xong thì thử lại
            boardHost.post(this::buildBoard);
            return;
        }
        int margin = dp(5);
        int size = Math.min(w / cols, h / rows) - 2 * margin;

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(cols);
        grid.setRowCount(rows);
        boardHost.addView(grid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        cards = new TextView[total];
        for (int i = 0; i < total; i++) {
            TextView tv = new TextView(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(i / cols), GridLayout.spec(i % cols));
            lp.width = size;
            lp.height = size;
            lp.setMargins(margin, margin, margin, margin);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setIncludeFontPadding(false);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * 0.5f);
            tv.setTextColor(0xFFFFFFFF);
            tv.setCameraDistance(getResources().getDisplayMetrics().density * 8000);
            cards[i] = tv;
            showBack(i);

            final int idx = i;
            tv.setOnClickListener(v -> onCardClicked(idx));

            // Hiệu ứng xuất hiện lần lượt
            tv.setScaleX(0f);
            tv.setScaleY(0f);
            tv.animate().setStartDelay(i * 30L).setDuration(250)
                    .setInterpolator(new OvershootInterpolator()).scaleX(1f).scaleY(1f).start();
            grid.addView(tv);
        }
        handler.postDelayed(this::afterBuild, total * 30L + 300);
    }

    /** Sau hiệu ứng xuất hiện: chế độ Ghi nhớ nhanh cho xem trước các thẻ. */
    private void afterBuild() {
        if (mode == 2 && !twoPlayer) {
            for (int i = 0; i < total; i++) flip(i, true, null);
            previewTick(Level.PREVIEW[level]);
        } else {
            busy = false;
        }
    }

    private void previewTick(final int left) {
        if (left <= 0) {
            hideBanner();
            for (int i = 0; i < total; i++) flip(i, false, null);
            handler.postDelayed(() -> busy = false, 300);
            return;
        }
        showBanner("👀 Ghi nhớ! " + left);
        handler.postDelayed(() -> previewTick(left - 1), 1000);
    }

    private void showBanner(String text) {
        tvCombo.animate().cancel();
        tvCombo.setText(text);
        tvCombo.setAlpha(1f);
        tvCombo.setScaleX(1f);
        tvCombo.setScaleY(1f);
        tvCombo.setTranslationY(0f);
        tvCombo.setVisibility(View.VISIBLE);
    }

    private void hideBanner() {
        tvCombo.animate().cancel();
        tvCombo.setVisibility(View.GONE);
    }

    // ---------- Xử lý sự kiện chạm vào thẻ ----------
    private void onCardClicked(int i) {
        if (busy || finished || paused || matched[i] || faceUp[i]) return;
        if (!started) {
            started = true;
            if (!twoPlayer) handler.postDelayed(tick, 1000);
        }
        feedback.click();
        faceUp[i] = true;
        flip(i, true, null);

        if (first == -1) {
            first = i;
            return;
        }
        final int a = first, b = i;
        first = -1;
        moves++;
        updateStats();
        busy = true;

        if (faces[a].equals(faces[b])) {
            handler.postDelayed(() -> {
                if (finished) return;
                matched[a] = true;
                matched[b] = true;
                markMatched(a);
                markMatched(b);
                matchedPairs++;
                onMatch();
                updateStats();
                feedback.match();
                busy = false;
                if (matchedPairs == pairs) finishGame();
            }, 380);
        } else {
            combo = 0;                      // lật sai là mất combo
            handler.postDelayed(() -> {
                feedback.miss();
                shake(cards[a]);
                shake(cards[b]);
                flip(a, false, null);
                flip(b, false, null);
                faceUp[a] = false;
                faceUp[b] = false;
                if (twoPlayer) {            // sai thì đổi lượt cho người kia
                    turn = 1 - turn;
                    updateStats();
                    showCombo("👤 Lượt Người " + (turn + 1));
                }
                handler.postDelayed(() -> busy = false, 300);
            }, 750);
        }
    }

    /** Lật đúng: tăng combo, cộng điểm thưởng (và cộng giờ ở chế độ đếm ngược). */
    private void onMatch() {
        if (twoPlayer) {                    // đúng thì được lật tiếp
            if (turn == 0) p1++; else p2++;
            return;
        }
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        if (combo >= 2) {
            comboBonus += (combo - 1) * 25;
            String text = "🔥 COMBO x" + combo;
            if (mode == 1 && matchedPairs < pairs) {
                timeLeft += COMBO_TIME_BONUS;
                refreshTime();
                text += "  +" + COMBO_TIME_BONUS + "s";
            }
            showCombo(text);
        }
    }

    private void showCombo(String text) {
        tvCombo.animate().cancel();
        tvCombo.setText(text);
        tvCombo.setVisibility(View.VISIBLE);
        tvCombo.setAlpha(1f);
        tvCombo.setTranslationY(0f);
        tvCombo.setScaleX(0.5f);
        tvCombo.setScaleY(0.5f);
        tvCombo.animate().setStartDelay(0).setInterpolator(new OvershootInterpolator())
                .setDuration(300).scaleX(1.1f).scaleY(1.1f).withEndAction(() ->
                        tvCombo.animate().setStartDelay(350).setInterpolator(new AccelerateInterpolator())
                                .setDuration(500).alpha(0f).translationY(-dp(60))
                                .withEndAction(() -> tvCombo.setVisibility(View.GONE)).start()).start();
    }

    // ---------- Gợi ý ----------
    private void useHint() {
        if (busy || finished || paused || twoPlayer || mode == 2) return;
        if (hintsLeft > 0) {
            hintsLeft--;                                  // lượt miễn phí của ván
        } else if (prefs.useItem(Shop.HINT)) {
            Toast.makeText(this, "Đã dùng 1 lượt gợi ý đã mua", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Hết lượt gợi ý. Mua thêm ở Cửa hàng nhé!", Toast.LENGTH_SHORT).show();
            return;
        }
        updateHintButton();
        if (started) {
            seconds += HINT_PENALTY;
            if (mode == 1) {
                timeLeft -= HINT_PENALTY;
                if (timeLeft <= 0) {
                    refreshTime();
                    timeUp();
                    return;
                }
            }
            refreshTime();
            Toast.makeText(this, mode == 1 ? "Gợi ý: -" + HINT_PENALTY + " giây" : "Gợi ý: +" + HINT_PENALTY + " giây",
                    Toast.LENGTH_SHORT).show();
        }
        busy = true;
        final List<Integer> revealed = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            if (!matched[i] && !faceUp[i]) {
                revealed.add(i);
                flip(i, true, null);
            }
        }
        handler.postDelayed(() -> {
            for (int i : revealed) flip(i, false, null);
            handler.postDelayed(() -> busy = false, 300);
        }, 1300);
    }

    private void updateHintButton() {
        int stock = prefs.getItemCount(Shop.HINT);
        btnHint.setText((twoPlayer || mode == 2) ? "💡 Không dùng"
                : "💡 Gợi ý (" + hintsLeft + (stock > 0 ? " +" + stock : "") + ")");
        btnHint.setEnabled(!(twoPlayer || mode == 2));
    }

    // ---------- Vật phẩm mua bằng xu ----------
    private void refreshInfoLine() {
        tvBest.setText(doubleActive ? infoBase + "  ·  ✖2 điểm" : infoBase);
    }

    private void refreshItems() {
        int time = prefs.getItemCount(Shop.TIME);
        int freeze = prefs.getItemCount(Shop.FREEZE);
        int pair = prefs.getItemCount(Shop.PAIR);
        int dbl = prefs.getItemCount(Shop.DOUBLE);
        btnItemTime.setText("⏳ +" + Shop.TIME_BONUS + "s\n×" + time);
        btnItemFreeze.setText("❄ Đóng băng\n×" + freeze);
        btnItemPair.setText("🎯 1 cặp\n×" + pair);
        btnItemDouble.setText(doubleActive ? "✖2 đang bật" : "✖2 điểm\n×" + dbl);
        btnItemTime.setAlpha(mode == 1 && time > 0 ? 1f : 0.45f);
        btnItemFreeze.setAlpha(freeze > 0 ? 1f : 0.45f);
        btnItemPair.setAlpha(pair > 0 ? 1f : 0.45f);
        btnItemDouble.setAlpha(doubleActive || dbl > 0 ? 1f : 0.45f);
    }

    private boolean canUseItem(String key) {
        if (finished || paused || busy) return false;
        if (prefs.getItemCount(key) <= 0) {
            Toast.makeText(this, "Hết vật phẩm này. Mua thêm ở tab Cửa hàng!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /** +15 giây (chỉ chế độ Đếm ngược). */
    private void useTimeItem() {
        if (mode != 1) {
            Toast.makeText(this, "Chỉ dùng được ở chế độ Đếm ngược", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canUseItem(Shop.TIME)) return;
        prefs.useItem(Shop.TIME);
        timeLeft += Shop.TIME_BONUS;
        refreshTime();
        showCombo("⏳ +" + Shop.TIME_BONUS + " giây");
        refreshItems();
    }

    /** Dừng đồng hồ 10 giây. */
    private void useFreezeItem() {
        if (!started) {
            Toast.makeText(this, "Hãy lật thẻ đầu tiên rồi mới dùng nhé", Toast.LENGTH_SHORT).show();
            return;
        }
        if (freezeLeft > 0) {
            Toast.makeText(this, "Đồng hồ đang đóng băng rồi", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canUseItem(Shop.FREEZE)) return;
        prefs.useItem(Shop.FREEZE);
        freezeLeft = Shop.FREEZE_SECONDS;
        refreshTime();
        showCombo("❄ Đóng băng " + Shop.FREEZE_SECONDS + " giây");
        refreshItems();
    }

    /** Làm lộ một cặp thẻ giống nhau trong 1,5 giây. */
    private void usePairItem() {
        if (!canUseItem(Shop.PAIR)) return;
        Map<String, List<Integer>> groups = new HashMap<>();
        for (int i = 0; i < total; i++) {
            if (!matched[i] && !faceUp[i]) {
                if (!groups.containsKey(faces[i])) groups.put(faces[i], new ArrayList<Integer>());
                groups.get(faces[i]).add(i);
            }
        }
        List<List<Integer>> available = new ArrayList<>();
        for (List<Integer> g : groups.values()) {
            if (g.size() >= 2) available.add(g);
        }
        if (available.isEmpty()) {
            Toast.makeText(this, "Không còn cặp nào để gợi ý", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Integer> pick = available.get(new Random().nextInt(available.size()));
        final int a = pick.get(0), b = pick.get(1);
        prefs.useItem(Shop.PAIR);
        refreshItems();
        busy = true;
        flip(a, true, null);
        flip(b, true, null);
        handler.postDelayed(() -> {
            flip(a, false, null);
            flip(b, false, null);
            handler.postDelayed(() -> busy = false, 300);
        }, 1500);
    }

    /** Nhân đôi điểm của ván đang chơi. */
    private void useDoubleItem() {
        if (twoPlayer) return;
        if (doubleActive) {
            Toast.makeText(this, "Ván này đã bật nhân đôi điểm", Toast.LENGTH_SHORT).show();
            return;
        }
        if (finished || paused) return;
        if (prefs.getItemCount(Shop.DOUBLE) <= 0) {
            Toast.makeText(this, "Hết vật phẩm này. Mua thêm ở tab Cửa hàng!", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.useItem(Shop.DOUBLE);
        doubleActive = true;
        refreshInfoLine();
        refreshItems();
        showCombo("✖2 Điểm ván này!");
    }

    // ---------- Tạm dừng / Tiếp tục ----------
    private void pauseGame() {
        if (paused || finished) return;
        paused = true;
        handler.removeCallbacks(tick);
        boardHost.setVisibility(View.INVISIBLE);   // che thẻ để không "ăn gian"
        pauseOverlay.setAlpha(0f);
        pauseOverlay.setVisibility(View.VISIBLE);
        pauseOverlay.animate().alpha(1f).setDuration(200).start();
    }

    private void resumeGame() {
        if (!paused) return;
        paused = false;
        pauseOverlay.setVisibility(View.GONE);
        boardHost.setVisibility(View.VISIBLE);
        if (started && !finished && !twoPlayer) {
            handler.removeCallbacks(tick);
            handler.postDelayed(tick, 1000);
        }
    }

    // ---------- Kết thúc ván ----------
    private void finishGame() {
        if (finished) return;
        finished = true;
        handler.removeCallbacks(tick);
        if (twoPlayer) {
            feedback.win();
            final Intent r = new Intent(this, ResultActivity.class);
            r.putExtra(ResultActivity.EXTRA_LEVEL, level);
            r.putExtra(ResultActivity.EXTRA_TWO_PLAYER, true);
            r.putExtra(ResultActivity.EXTRA_P1, p1);
            r.putExtra(ResultActivity.EXTRA_P2, p2);
            r.putExtra(ResultActivity.EXTRA_MOVES, moves);
            openResult(r);
            return;
        }
        int base = Math.max(0, Level.maxScore(level) - (moves - pairs) * 10 - seconds * 2 + comboBonus);
        float ratio = base / (float) Level.maxScore(level);
        int stars = ratio >= 0.7f ? 3 : ratio >= 0.45f ? 2 : 1;   // sao tính trước khi nhân đôi
        int score = doubleActive ? base * 2 : base;
        int coins = Shop.winReward(stars, maxCombo, mode);
        prefs.addCoins(coins);
        boolean newBest = prefs.saveResult(mode, level, score, seconds, moves);
        feedback.win();

        final Intent i = new Intent(this, ResultActivity.class);
        i.putExtra(ResultActivity.EXTRA_LEVEL, level);
        i.putExtra(ResultActivity.EXTRA_MODE, mode);
        i.putExtra(ResultActivity.EXTRA_WON, true);
        i.putExtra(ResultActivity.EXTRA_SCORE, score);
        i.putExtra(ResultActivity.EXTRA_TIME, seconds);
        i.putExtra(ResultActivity.EXTRA_MOVES, moves);
        i.putExtra(ResultActivity.EXTRA_STARS, stars);
        i.putExtra(ResultActivity.EXTRA_NEW_BEST, newBest);
        i.putExtra(ResultActivity.EXTRA_MAX_COMBO, maxCombo);
        i.putExtra(ResultActivity.EXTRA_PAIRS_FOUND, matchedPairs);
        i.putExtra(ResultActivity.EXTRA_PAIRS_TOTAL, pairs);
        i.putExtra(ResultActivity.EXTRA_COINS, coins);
        i.putExtra(ResultActivity.EXTRA_DOUBLED, doubleActive);
        openResult(i);
    }

    /** Hết giờ ở chế độ đếm ngược. */
    private void timeUp() {
        if (finished) return;
        finished = true;
        handler.removeCallbacks(tick);
        timeLeft = 0;
        refreshTime();
        prefs.addCoins(matchedPairs);       // an ủi: mỗi cặp tìm được = 1 xu
        feedback.miss();

        final Intent i = new Intent(this, ResultActivity.class);
        i.putExtra(ResultActivity.EXTRA_LEVEL, level);
        i.putExtra(ResultActivity.EXTRA_MODE, mode);
        i.putExtra(ResultActivity.EXTRA_WON, false);
        i.putExtra(ResultActivity.EXTRA_SCORE, 0);
        i.putExtra(ResultActivity.EXTRA_TIME, seconds);
        i.putExtra(ResultActivity.EXTRA_MOVES, moves);
        i.putExtra(ResultActivity.EXTRA_STARS, 0);
        i.putExtra(ResultActivity.EXTRA_NEW_BEST, false);
        i.putExtra(ResultActivity.EXTRA_MAX_COMBO, maxCombo);
        i.putExtra(ResultActivity.EXTRA_PAIRS_FOUND, matchedPairs);
        i.putExtra(ResultActivity.EXTRA_PAIRS_TOTAL, pairs);
        i.putExtra(ResultActivity.EXTRA_COINS, matchedPairs);
        i.putExtra(ResultActivity.EXTRA_DOUBLED, false);
        openResult(i);
    }

    private void openResult(final Intent i) {
        handler.postDelayed(() -> {
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            finish();
        }, 800);
    }

    // ---------- Hiệu ứng thẻ ----------
    private void showFront(int i) {
        cards[i].setText(faces[i]);
        cards[i].setBackgroundResource(matched[i] ? R.drawable.card_matched : R.drawable.card_front);
    }

    private void showBack(int i) {
        cards[i].setText(backSymbol);
        cards[i].setBackgroundResource(R.drawable.card_back);
        cards[i].setTypeface(null, android.graphics.Typeface.BOLD);
    }

    /** Lật thẻ bằng animation xoay quanh trục Y. */
    private void flip(final int i, final boolean toFront, final Runnable after) {
        final TextView tv = cards[i];
        tv.animate().setStartDelay(0).setInterpolator(new AccelerateInterpolator())
                .setDuration(120).rotationY(90f).withEndAction(() -> {
                    if (toFront) showFront(i); else showBack(i);
                    tv.setRotationY(-90f);
                    tv.animate().setStartDelay(0).setInterpolator(new DecelerateInterpolator())
                            .setDuration(120).rotationY(0f).withEndAction(after).start();
                }).start();
    }

    private void markMatched(int i) {
        final TextView tv = cards[i];
        tv.setBackgroundResource(R.drawable.card_matched);
        tv.animate().setStartDelay(0).setInterpolator(new OvershootInterpolator())
                .setDuration(160).scaleX(1.15f).scaleY(1.15f).withEndAction(() ->
                        tv.animate().setStartDelay(0).setDuration(160).scaleX(1f).scaleY(1f).start()).start();
    }

    private void shake(View v) {
        float d = dp(8);
        ObjectAnimator.ofFloat(v, "translationX", 0, -d, d, -d, d, 0).setDuration(350).start();
    }

    private void refreshTime() {
        if (twoPlayer) return;
        int shown = mode == 1 ? Math.max(0, timeLeft) : seconds;
        tvTime.setText(Level.formatTime(shown));
        boolean danger = mode == 1 && shown <= 10;
        int color = danger ? 0xFFD32F2F : AppTheme.primary(this);
        if (freezeLeft > 0) color = 0xFF1E88E5;          // xanh dương khi đóng băng
        tvTime.setTextColor(color);
    }

    private void updateStats() {
        int primary = AppTheme.primary(this);
        if (twoPlayer) {
            tvTime.setText(String.valueOf(p1));
            tvPairs.setText(String.valueOf(p2));
            tvMoves.setText("Người " + (turn + 1));
            tvTime.setTextColor(turn == 0 ? primary : AppTheme.muted(this));
            tvPairs.setTextColor(turn == 1 ? primary : AppTheme.muted(this));
            tvMoves.setTextColor(primary);
        } else {
            tvMoves.setText(String.valueOf(moves));
            tvPairs.setText(matchedPairs + "/" + pairs);
            tvMoves.setTextColor(primary);
            tvPairs.setTextColor(primary);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---------- Options Menu (nút Tạm dừng trên thanh công cụ) ----------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_pause) {
            pauseGame();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------- Context Menu ----------
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        menu.setHeaderTitle("Tùy chọn ván chơi");
        getMenuInflater().inflate(R.menu.context_game, menu);
        if (twoPlayer) menu.removeItem(R.id.ctx_mode);   // 2 người không có chế độ khác
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.ctx_restart) {
            startGame();
        } else if (id == R.id.ctx_mode) {
            mode = (mode + 1) % Level.MODES.length;
            prefs.setMode(mode);
            startGame();
        } else if (id == R.id.ctx_theme) {
            int t = theme;
            do {
                t = (t + 1) % Themes.NAMES.length;
            } while (!prefs.isOwned("theme_" + t) && t != theme);   // bỏ qua chủ đề chưa mua
            theme = t;
            prefs.setTheme(theme);
            startGame();
        } else if (id == R.id.ctx_easy) {
            changeLevel(0);
        } else if (id == R.id.ctx_medium) {
            changeLevel(1);
        } else if (id == R.id.ctx_hard) {
            changeLevel(2);
        } else if (id == R.id.ctx_sound) {
            prefs.setSound(!prefs.isSound());
            Toast.makeText(this, prefs.isSound() ? "Đã bật âm thanh" : "Đã tắt âm thanh",
                    Toast.LENGTH_SHORT).show();
        } else {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    private void changeLevel(int l) {
        level = l;
        prefs.setDifficulty(l);
        startGame();
    }

    // ---------- Vòng đời ----------
    @Override
    protected void onPause() {
        super.onPause();
        if (started && !finished) {
            pauseGame();                    // rời app thì tự động tạm dừng
        } else {
            handler.removeCallbacks(tick);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        feedback.release();
    }
}
