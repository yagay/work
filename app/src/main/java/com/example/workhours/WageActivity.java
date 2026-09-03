package com.example.workhours;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Legacy standalone wage entry point.
 *
 * Wage calculations used to be duplicated here and in {@link WagePanel}, which
 * made the two screens disagree after wage-history and monthly-rest support was
 * added. This Activity is now only a lightweight host for WagePanel so there is
 * exactly one wage-calculation implementation.
 */
public class WageActivity extends Activity {
    private WagePanel wagePanel;
    private boolean appliedDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedDarkTheme = AppThemeManager.apply(this);
        setContentView(buildUi());
        AppThemeManager.applySystemBars(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appliedDarkTheme != AppThemeManager.isDark(this)) {
            recreate();
            return;
        }
        if (wagePanel != null) wagePanel.refresh();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        UiStyle.page(scroll);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
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

        wagePanel = new WagePanel(this);
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, -2);
        panelParams.topMargin = dp(4);
        root.addView(wagePanel, panelParams);
        return scroll;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
