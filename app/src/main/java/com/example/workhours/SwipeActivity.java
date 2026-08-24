package com.example.workhours;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

public abstract class SwipeActivity extends Activity {

    private GestureDetector swipeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int MIN_DISTANCE = 120;
            private static final int MIN_VELOCITY = 180;

            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) < MIN_DISTANCE || Math.abs(dx) <= Math.abs(dy) * 1.25f || Math.abs(velocityX) < MIN_VELOCITY) {
                    return false;
                }
                if (dx < 0 && SwipeActivity.this instanceof MainActivity) {
                    Intent intent = new Intent(SwipeActivity.this, WageActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
                    return true;
                }
                if (dx > 0 && SwipeActivity.this instanceof WageActivity) {
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (swipeDetector != null) swipeDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }
}
