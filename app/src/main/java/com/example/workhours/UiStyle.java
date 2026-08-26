package com.example.workhours;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class UiStyle {
    static int PAGE_BG = 0xFFF5F7FB;
    static int CARD_BG = 0xFFFFFFFF;
    static int PRIMARY = 0xFF3157D5;
    static int PRIMARY_SOFT = 0xFFEAF0FF;
    static int TEXT = 0xFF172033;
    static int TEXT_MUTED = 0xFF6B7280;
    static int BORDER = 0xFFE2E7F0;
    static int DANGER = 0xFFC5221F;
    static int INPUT_BG = 0xFFF9FAFC;
    static int HINT = 0xFF9AA3B2;
    static int CAL_TODAY_BG = 0xFFE8F0FE;
    static int CAL_TODAY_BORDER = 0xFFB8C8FF;
    static int CAL_LEAVE_BG = 0xFFFFF0F0;
    static int CAL_LEAVE_BORDER = 0xFFF2CACA;
    static int CAL_OVERTIME_BG = 0xFFF1F8F3;
    static int CAL_OVERTIME_BORDER = 0xFFCDE7D4;
    static int CAL_OVERRIDE_BG = 0xFFFFF7E8;
    static int CAL_OVERRIDE_BORDER = 0xFFF1D8A6;
    static int CAL_HOLIDAY_BG = 0xFFFFF2F4;
    static int CAL_HOLIDAY_BORDER = 0xFFF3CDD3;
    static int CAL_REST_BG = 0xFFF4F6F9;
    static int CAL_REST_BORDER = 0xFFE1E6EE;
    static int CAL_DISABLED_TEXT = 0xFF9AA0A6;
    static int WEEKEND_TEXT = 0xFF5F6368;
    static int CAL_WEEKEND_BG = 0xFFFFF4F4;

    private UiStyle() { }

    static void applyDark(boolean dark) {
        if (dark) {
            PAGE_BG = 0xFF0F141C;
            CARD_BG = 0xFF171E29;
            PRIMARY = 0xFF6F8FFF;
            PRIMARY_SOFT = 0xFF202A44;
            TEXT = 0xFFF1F5FF;
            TEXT_MUTED = 0xFF9DA8BA;
            BORDER = 0xFF2C3545;
            DANGER = 0xFFFF7A76;
            INPUT_BG = 0xFF121925;
            HINT = 0xFF778297;
            CAL_TODAY_BG = 0xFF1D2A49;
            CAL_TODAY_BORDER = 0xFF5876D8;
            CAL_LEAVE_BG = 0xFF3A2025;
            CAL_LEAVE_BORDER = 0xFF84434B;
            CAL_OVERTIME_BG = 0xFF173126;
            CAL_OVERTIME_BORDER = 0xFF3F7458;
            CAL_OVERRIDE_BG = 0xFF352B18;
            CAL_OVERRIDE_BORDER = 0xFF806836;
            CAL_HOLIDAY_BG = 0xFF3A2028;
            CAL_HOLIDAY_BORDER = 0xFF844653;
            CAL_REST_BG = 0xFF1B222D;
            CAL_REST_BORDER = 0xFF374252;
            CAL_DISABLED_TEXT = 0xFF697487;
            WEEKEND_TEXT = 0xFF7F899A;
            CAL_WEEKEND_BG = 0xFF2B2023;
        } else {
            PAGE_BG = 0xFFF5F7FB;
            CARD_BG = 0xFFFFFFFF;
            PRIMARY = 0xFF3157D5;
            PRIMARY_SOFT = 0xFFEAF0FF;
            TEXT = 0xFF172033;
            TEXT_MUTED = 0xFF6B7280;
            BORDER = 0xFFE2E7F0;
            DANGER = 0xFFC5221F;
            INPUT_BG = 0xFFF9FAFC;
            HINT = 0xFF9AA3B2;
            CAL_TODAY_BG = 0xFFE8F0FE;
            CAL_TODAY_BORDER = 0xFFB8C8FF;
            CAL_LEAVE_BG = 0xFFFFF0F0;
            CAL_LEAVE_BORDER = 0xFFF2CACA;
            CAL_OVERTIME_BG = 0xFFF1F8F3;
            CAL_OVERTIME_BORDER = 0xFFCDE7D4;
            CAL_OVERRIDE_BG = 0xFFFFF7E8;
            CAL_OVERRIDE_BORDER = 0xFFF1D8A6;
            CAL_HOLIDAY_BG = 0xFFFFF2F4;
            CAL_HOLIDAY_BORDER = 0xFFF3CDD3;
            CAL_REST_BG = 0xFFF4F6F9;
            CAL_REST_BORDER = 0xFFE1E6EE;
            CAL_DISABLED_TEXT = 0xFF9AA0A6;
            WEEKEND_TEXT = 0xFF5F6368;
            CAL_WEEKEND_BG = 0xFFFFF4F4;
        }
    }

    static void page(View view) {
        view.setBackgroundColor(PAGE_BG);
    }

    static void card(Context context, LinearLayout view) {
        view.setElevation(dp(context, 2));
        view.setBackground(roundRect(context, CARD_BG, 18, BORDER, 1));
    }

    static void input(Context context, EditText view) {
        view.setTextColor(TEXT);
        view.setHintTextColor(HINT);
        view.setBackground(roundRect(context, INPUT_BG, 14, BORDER, 1));
        view.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));
    }

    static void button(Context context, Button view, boolean primary) {
        int fill = primary ? PRIMARY : CARD_BG;
        int text = primary ? Color.WHITE : TEXT;
        int stroke = primary ? PRIMARY : BORDER;
        GradientDrawable shape = roundRect(context, fill, 14, stroke, 1);
        RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(primary ? 0x33FFFFFF : 0x143157D5), shape, null);
        view.setBackground(ripple);
        view.setTextColor(text);
        view.setAllCaps(false);
        view.setMinHeight(dp(context, 46));
        view.setPadding(dp(context, 14), 0, dp(context, 14), 0);
    }

    static void navButton(Context context, Button view) {
        button(context, view, false);
        view.setTextColor(PRIMARY);
    }

    static void title(TextView view) {
        view.setTextColor(TEXT);
        view.setLetterSpacing(-0.01f);
    }

    static void muted(TextView view) {
        view.setTextColor(TEXT_MUTED);
    }

    static GradientDrawable roundRect(Context context, int fill, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(context, radiusDp));
        if (strokeDp > 0) d.setStroke(dp(context, strokeDp), strokeColor);
        return d;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
