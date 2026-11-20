package com.khang.memorygame;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private static final int[] LEVEL_BUTTONS = {R.id.btnEasy, R.id.btnMedium, R.id.btnHard};
    private static final int[] MODE_BUTTONS = {R.id.btnClassic, R.id.btnCountdown, R.id.btnMemorize};
    private static final int[] DOTS = {R.id.dot0, R.id.dot1, R.id.dot2, R.id.dot3};
    private static final int[] THEME_BUTTONS = {R.id.btnTheme0, R.id.btnTheme1, R.id.btnTheme2, R.id.btnTheme3};

    private Prefs prefs;
    private MaterialToolbar toolbar;
    private MaterialButtonToggleGroup toggleLevel, toggleMode, toggleTheme;
    private TextView tvLevelInfo, tvBestHome, tvLogo, tvThemeLabel;
    private TextView[] tvHs;
    private View panelHome, panelScores, panelGuide, panelShop;
    private ObjectAnimator logoAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppTheme.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Bottom inset do BottomNavigationView tự xử lý
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        prefs = new Prefs(this);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        panelHome = findViewById(R.id.panelHome);
        panelScores = findViewById(R.id.panelScores);
        panelGuide = findViewById(R.id.panelGuide);
        panelShop = findViewById(R.id.panelShop);
        tvLevelInfo = findViewById(R.id.tvLevelInfo);
        tvBestHome = findViewById(R.id.tvBestHome);
        tvLogo = findViewById(R.id.tvLogo);
        tvHs = new TextView[]{findViewById(R.id.tvHsEasy), findViewById(R.id.tvHsMedium), findViewById(R.id.tvHsHard)};
        toggleLevel = findViewById(R.id.toggleLevel);

        toggleLevel.check(LEVEL_BUTTONS[prefs.getDifficulty()]);
        toggleLevel.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < LEVEL_BUTTONS.length; i++) {
                if (LEVEL_BUTTONS[i] == checkedId) prefs.setDifficulty(i);
            }
            updateHome();
            invalidateOptionsMenu();
        });

        toggleMode = findViewById(R.id.toggleMode);
        tvThemeLabel = findViewById(R.id.tvThemeLabel);
        toggleTheme = findViewById(R.id.toggleTheme);
        toggleMode.check(MODE_BUTTONS[prefs.getMode()]);
        toggleTheme.check(THEME_BUTTONS[prefs.getTheme()]);
        toggleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < MODE_BUTTONS.length; i++) {
                if (MODE_BUTTONS[i] == checkedId) prefs.setMode(i);
            }
            updateHome();
            invalidateOptionsMenu();
        });
        toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < THEME_BUTTONS.length; i++) {
                if (THEME_BUTTONS[i] == checkedId) {
                    if (!prefs.isOwned("theme_" + i)) {      // chủ đề chưa mua
                        Toast.makeText(this, "Chủ đề này cần mua ở tab Cửa hàng 🔒", Toast.LENGTH_SHORT).show();
                        toggleTheme.check(THEME_BUTTONS[prefs.getTheme()]);
                        return;
                    }
                    prefs.setTheme(i);
                }
            }
            updateHome();
            invalidateOptionsMenu();
        });

        // Chọn màu chủ đạo: lưu rồi tạo lại Activity để áp dụng theme mới
        for (int i = 0; i < DOTS.length; i++) {
            final int idx = i;
            findViewById(DOTS[i]).setOnClickListener(v -> changeColor(idx));
        }
        updateDots();

        findViewById(R.id.btnTwoPlayer).setOnClickListener(v -> {
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra(GameActivity.EXTRA_LEVEL, prefs.getDifficulty());
            i.putExtra(GameActivity.EXTRA_TWO_PLAYER, true);
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra(GameActivity.EXTRA_LEVEL, prefs.getDifficulty());
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                show(panelHome, getString(R.string.app_name));
            } else if (id == R.id.nav_shop) {
                buildShop();
                show(panelShop, "Cửa hàng");
            } else if (id == R.id.nav_scores) {
                show(panelScores, getString(R.string.title_scores));
            } else {
                show(panelGuide, getString(R.string.title_guide));
            }
            return true;
        });

        if (savedInstanceState != null) {
            nav.setSelectedItemId(savedInstanceState.getInt("tab", R.id.nav_home));
        }

        // Logo nhún nhảy
        logoAnim = ObjectAnimator.ofFloat(tvLogo, "translationY", 0f, -18f);
        logoAnim.setDuration(900);
        logoAnim.setRepeatCount(ObjectAnimator.INFINITE);
        logoAnim.setRepeatMode(ObjectAnimator.REVERSE);
        logoAnim.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        outState.putInt("tab", nav.getSelectedItemId());
    }

    private void changeColor(int idx) {
        if (idx == prefs.getColorTheme()) return;
        if (!prefs.isOwned("color_" + idx)) {
            Toast.makeText(this, "Màu này cần mua ở tab Cửa hàng 🔒", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.setColorTheme(idx);
        recreate();
    }

    private void updateDots() {
        for (int i = 0; i < DOTS.length; i++) {
            ((TextView) findViewById(DOTS[i])).setText(i == prefs.getColorTheme() ? "✓"
                    : prefs.isOwned("color_" + i) ? "" : "🔒");
        }
    }

    private void show(View target, String title) {
        for (View p : new View[]{panelHome, panelScores, panelGuide, panelShop}) {
            p.setVisibility(p == target ? View.VISIBLE : View.GONE);
        }
        getSupportActionBar().setTitle(title);
        target.setAlpha(0f);
        target.animate().alpha(1f).setDuration(250).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHome();
        updateScores();
        buildShop();
        invalidateOptionsMenu();            // cập nhật số xu trên thanh công cụ
        if (!logoAnim.isStarted()) logoAnim.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        logoAnim.cancel();
    }

    private void updateHome() {
        int d = prefs.getDifficulty();
        int mode = prefs.getMode();
        String info = Level.NAMES[d] + " · " + Level.pairs(d) + " cặp thẻ · lưới "
                + Level.COLS[d] + "×" + Level.ROWS[d];
        if (mode == 1) info += "\n⏳ Giới hạn " + Level.TIME_LIMIT[d] + " giây, lật đúng liên tiếp được cộng giờ";
        else if (mode == 2) info += "\n👀 Xem trước thẻ " + Level.PREVIEW[d] + " giây rồi thẻ úp lại, không có gợi ý";
        tvLevelInfo.setText(info);
        int best = prefs.getBestScore(mode, d);
        tvBestHome.setText(best == 0 ? "Chưa có kỷ lục, hãy chơi thử nào!"
                : "Kỷ lục " + Level.MODES[mode] + ": " + best + " điểm");
        tvThemeLabel.setText("Chủ đề thẻ: " + Themes.NAMES[prefs.getTheme()]);
        for (int i = 0; i < THEME_BUTTONS.length; i++) {
            ((MaterialButton) findViewById(THEME_BUTTONS[i]))
                    .setText(prefs.isOwned("theme_" + i) ? Themes.icon(i) : "🔒");
        }
        updateDots();
    }

    private void updateScores() {
        for (int d = 0; d < 3; d++) {
            StringBuilder sb = new StringBuilder();
            for (int m = 0; m < 3; m++) {
                if (m > 0) sb.append("\n");
                sb.append(Level.MODES[m]).append(": ");
                int best = prefs.getBestScore(m, d);
                if (best == 0) {
                    sb.append("chưa có kỷ lục");
                } else {
                    sb.append(best).append(" điểm  (")
                            .append(Level.formatTime(prefs.getBestTime(m, d))).append(" · ")
                            .append(prefs.getBestMoves(m, d)).append(" lượt · ")
                            .append(prefs.getPlayed(m, d)).append(" ván thắng)");
                }
            }
            tvHs[d].setText(sb.toString());
        }
    }

    // ---------- Cửa hàng ----------
    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void addSection(LinearLayout list, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(AppTheme.primary(this));
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(dp(4), dp(14), 0, dp(8));
        list.addView(t);
    }

    /** Thêm một dòng hàng vào danh sách (icon, tên, mô tả, nút). */
    private void addRow(LinearLayout list, String icon, String title, String sub,
                        String btnText, boolean btnEnabled, Runnable onClick) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_shop, list, false);
        ((TextView) row.findViewById(R.id.shopIcon)).setText(icon);
        ((TextView) row.findViewById(R.id.shopTitle)).setText(title);
        ((TextView) row.findViewById(R.id.shopSub)).setText(sub);
        MaterialButton btn = row.findViewById(R.id.shopBtn);
        btn.setText(btnText);
        btn.setEnabled(btnEnabled);
        btn.setOnClickListener(v -> onClick.run());
        list.addView(row);
    }

    /** Hộp thoại xác nhận mua. */
    private void confirmBuy(String name, int price, Runnable onBought) {
        if (prefs.getCoins() < price) {
            Toast.makeText(this, "Không đủ xu! Chơi thêm vài ván để kiếm xu nhé 💰", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Mua " + name + "?")
                .setMessage("Bạn sẽ trả 💰 " + price + " xu (còn lại " + (prefs.getCoins() - price) + " xu).")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Mua", (d, w) -> {
                    if (prefs.spendCoins(price)) {
                        onBought.run();
                        invalidateOptionsMenu();
                    }
                })
                .show();
    }

    private void buildShop() {
        LinearLayout list = findViewById(R.id.shopList);
        list.removeAllViews();

        TextView balance = new TextView(this);
        balance.setText("Số dư: 💰 " + prefs.getCoins() + " xu");
        balance.setTextSize(20);
        balance.setTypeface(null, android.graphics.Typeface.BOLD);
        balance.setTextColor(AppTheme.primary(this));
        balance.setBackgroundResource(R.drawable.bg_badge);
        balance.setGravity(android.view.Gravity.CENTER);
        balance.setPadding(dp(16), dp(14), dp(16), dp(14));
        list.addView(balance);

        // --- Chủ đề thẻ ---
        addSection(list, "🎴 Chủ đề thẻ");
        for (int i = 0; i < Themes.NAMES.length; i++) {
            final int idx = i;
            boolean owned = prefs.isOwned("theme_" + i);
            boolean active = owned && prefs.getTheme() == i;
            addRow(list, Themes.icon(i), Themes.NAMES[i], owned ? "Đã sở hữu" : "Chưa mua",
                    active ? "Đang dùng" : owned ? "Dùng" : "Mua · 💰 " + Shop.THEME_PRICE[i],
                    !active, () -> {
                        if (prefs.isOwned("theme_" + idx)) {
                            prefs.setTheme(idx);
                            toggleTheme.check(THEME_BUTTONS[idx]);
                            updateHome();
                            buildShop();
                        } else {
                            confirmBuy("chủ đề " + Themes.NAMES[idx], Shop.THEME_PRICE[idx], () -> {
                                prefs.setOwned("theme_" + idx);
                                prefs.setTheme(idx);
                                toggleTheme.check(THEME_BUTTONS[idx]);
                                updateHome();
                                buildShop();
                            });
                        }
                    });
        }

        // --- Màu chủ đạo ---
        addSection(list, "🎨 Màu chủ đạo");
        String[] colorIcons = {"●", "●", "●", "●"};
        for (int i = 0; i < AppTheme.NAMES.length; i++) {
            final int idx = i;
            boolean owned = prefs.isOwned("color_" + i);
            boolean active = owned && prefs.getColorTheme() == i;
            addRow(list, colorIcons[i], AppTheme.NAMES[i], owned ? "Đã sở hữu" : "Chưa mua",
                    active ? "Đang dùng" : owned ? "Dùng" : "Mua · 💰 " + Shop.COLOR_PRICE[i],
                    !active, () -> {
                        if (prefs.isOwned("color_" + idx)) {
                            changeColor(idx);
                        } else {
                            confirmBuy("màu " + AppTheme.NAMES[idx], Shop.COLOR_PRICE[idx], () -> {
                                prefs.setOwned("color_" + idx);
                                changeColor(idx);
                            });
                        }
                    });
        }

        // --- Mặt sau thẻ ---
        addSection(list, "🃏 Mặt sau của thẻ");
        for (int i = 0; i < Themes.BACK_NAMES.length; i++) {
            final int idx = i;
            boolean owned = prefs.isOwned("back_" + i);
            boolean active = owned && prefs.getBackStyle() == i;
            addRow(list, Themes.back(i), Themes.BACK_NAMES[i], owned ? "Đã sở hữu" : "Chưa mua",
                    active ? "Đang dùng" : owned ? "Dùng" : "Mua · 💰 " + Shop.BACK_PRICE[i],
                    !active, () -> {
                        if (prefs.isOwned("back_" + idx)) {
                            prefs.setBackStyle(idx);
                            buildShop();
                        } else {
                            confirmBuy("mặt sau " + Themes.BACK_NAMES[idx], Shop.BACK_PRICE[idx], () -> {
                                prefs.setOwned("back_" + idx);
                                prefs.setBackStyle(idx);
                                buildShop();
                            });
                        }
                    });
        }

        // --- Vật phẩm dùng trong ván ---
        addSection(list, "🎒 Vật phẩm");
        for (int i = 0; i < Shop.ITEM_KEYS.length; i++) {
            final int idx = i;
            addRow(list, Shop.ITEM_ICONS[i], Shop.ITEM_NAMES[i],
                    Shop.ITEM_DESC[i] + "\nĐang có: " + prefs.getItemCount(Shop.ITEM_KEYS[i]),
                    "Mua · 💰 " + Shop.ITEM_PRICE[i], true, () ->
                            confirmBuy(Shop.ITEM_NAMES[idx], Shop.ITEM_PRICE[idx], () -> {
                                prefs.addItem(Shop.ITEM_KEYS[idx], 1);
                                Toast.makeText(this, "Đã mua " + Shop.ITEM_NAMES[idx], Toast.LENGTH_SHORT).show();
                                buildShop();
                            }));
        }
    }

    // ---------- Options Menu (có SubMenu) ----------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_coins).setTitle("💰 " + prefs.getCoins());
        menu.findItem(R.id.action_sound).setChecked(prefs.isSound());
        menu.findItem(R.id.action_vibrate).setChecked(prefs.isVibrate());
        int[] ids = {R.id.diff_easy, R.id.diff_medium, R.id.diff_hard};
        menu.findItem(ids[prefs.getDifficulty()]).setChecked(true);
        int[] modeIds = {R.id.mode_classic, R.id.mode_countdown, R.id.mode_memorize};
        menu.findItem(modeIds[prefs.getMode()]).setChecked(true);
        int[] colorIds = {R.id.color_0, R.id.color_1, R.id.color_2, R.id.color_3};
        menu.findItem(colorIds[prefs.getColorTheme()]).setChecked(true);
        int[] themeIds = {R.id.theme_0, R.id.theme_1, R.id.theme_2, R.id.theme_3};
        menu.findItem(themeIds[prefs.getTheme()]).setChecked(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_coins) {
            ((BottomNavigationView) findViewById(R.id.bottomNav)).setSelectedItemId(R.id.nav_shop);
        } else if (id == R.id.action_sound) {
            prefs.setSound(!prefs.isSound());
        } else if (id == R.id.action_vibrate) {
            prefs.setVibrate(!prefs.isVibrate());
        } else if (id == R.id.diff_easy) {
            toggleLevel.check(R.id.btnEasy);
        } else if (id == R.id.diff_medium) {
            toggleLevel.check(R.id.btnMedium);
        } else if (id == R.id.diff_hard) {
            toggleLevel.check(R.id.btnHard);
        } else if (id == R.id.mode_classic) {
            toggleMode.check(R.id.btnClassic);
        } else if (id == R.id.mode_countdown) {
            toggleMode.check(R.id.btnCountdown);
        } else if (id == R.id.mode_memorize) {
            toggleMode.check(R.id.btnMemorize);
        } else if (id == R.id.color_0) {
            changeColor(0);
        } else if (id == R.id.color_1) {
            changeColor(1);
        } else if (id == R.id.color_2) {
            changeColor(2);
        } else if (id == R.id.color_3) {
            changeColor(3);
        } else if (id == R.id.theme_0) {
            toggleTheme.check(R.id.btnTheme0);
        } else if (id == R.id.theme_1) {
            toggleTheme.check(R.id.btnTheme1);
        } else if (id == R.id.theme_2) {
            toggleTheme.check(R.id.btnTheme2);
        } else if (id == R.id.theme_3) {
            toggleTheme.check(R.id.btnTheme3);
        } else if (id == R.id.action_clear) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Xóa điểm cao?")
                    .setMessage("Toàn bộ kỷ lục sẽ bị xóa và không thể khôi phục.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        prefs.clearHighScores();
                        updateHome();
                        updateScores();
                        Toast.makeText(this, "Đã xóa điểm cao", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        } else if (id == R.id.action_exit) {
            finishAffinity();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
