package com.example.workhours;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Standalone wage entry point backed by the same WagePanel used on MainActivity.
 * Keeping WagePanel as the only calculator prevents the standalone and embedded
 * wage screens from drifting apart.
 */
public class WageActivity extends Activity {
    private static final String PREFS = "work_hours_prefs";
    private static final String WORK_START_DATE_KEY = "work_start_date";

    private WagePanel wagePanel;
    private TextView monthTitle;
    private Button previousMonthButton;
    private Button nextMonthButton;
    private YearMonth displayedMonth;
    private boolean appliedDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedDarkTheme = AppThemeManager.apply(this);
        displayedMonth = YearMonth.now();
        setContentView(buildUi());
        AppThemeManager.applySystemBars(this);
        applyDisplayedMonth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appliedDarkTheme != AppThemeManager.isDark(this)) {
            recreate();
            return;
        }
        clampDisplayedMonth();
        applyDisplayedMonth();
        if (wagePanel != null) wagePanel.refresh();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        UiStyle.page(scroll);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(root);

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);

        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(24);
        UiStyle.navButton(this, back);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(58), dp(48)));

        TextView title = new TextView(this);
        title.setText("工资统计");
        title.setTextSize(25);
        title.setTextColor(UiStyle.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1f);
        titleParams.leftMargin = dp(8);
        top.addView(title, titleParams);

        LinearLayout monthNav = horizontal();
        monthNav.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, dp(64));
        navParams.topMargin = dp(8);
        root.addView(monthNav, navParams);

        previousMonthButton = new Button(this);
        previousMonthButton.setText("‹");
        previousMonthButton.setTextSize(24);
        UiStyle.navButton(this, previousMonthButton);
        previousMonthButton.setOnClickListener(v -> {
            displayedMonth = displayedMonth.minusMonths(1);
            clampDisplayedMonth();
            applyDisplayedMonth();
        });
        monthNav.addView(previousMonthButton, new LinearLayout.LayoutParams(dp(58), dp(48)));

        monthTitle = new TextView(this);
        monthTitle.setTextSize(18);
        monthTitle.setTextColor(UiStyle.TEXT);
        monthTitle.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        monthTitle.setGravity(Gravity.CENTER);
        monthNav.addView(monthTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        nextMonthButton = new Button(this);
        nextMonthButton.setText("›");
        nextMonthButton.setTextSize(24);
        UiStyle.navButton(this, nextMonthButton);
        nextMonthButton.setOnClickListener(v -> {
            if (displayedMonth.isBefore(YearMonth.now())) displayedMonth = displayedMonth.plusMonths(1);
            clampDisplayedMonth();
            applyDisplayedMonth();
        });
        monthNav.addView(nextMonthButton, new LinearLayout.LayoutParams(dp(58), dp(48)));

        wagePanel = new WagePanel(this);
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, -2);
        panelParams.topMargin = dp(4);
        root.addView(wagePanel, panelParams);
        return scroll;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private void applyDisplayedMonth() {
        if (displayedMonth == null) displayedMonth = YearMonth.now();
        if (monthTitle != null) {
            monthTitle.setText(displayedMonth.getYear() + "年" + displayedMonth.getMonthValue() + "月");
        }
        YearMonth first = firstAllowedMonth();
        if (previousMonthButton != null) {
            previousMonthButton.setEnabled(first == null || displayedMonth.isAfter(first));
        }
        if (nextMonthButton != null) {
            nextMonthButton.setEnabled(displayedMonth.isBefore(YearMonth.now()));
        }
        if (wagePanel != null) wagePanel.setDisplayedMonth(displayedMonth);
    }

    private void clampDisplayedMonth() {
        YearMonth now = YearMonth.now();
        if (displayedMonth == null || displayedMonth.isAfter(now)) displayedMonth = now;
        YearMonth first = firstAllowedMonth();
        if (first != null && displayedMonth.isBefore(first)) displayedMonth = first;
    }

    private YearMonth firstAllowedMonth() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(WORK_START_DATE_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return YearMonth.from(LocalDate.parse(raw.trim()));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
