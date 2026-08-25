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
