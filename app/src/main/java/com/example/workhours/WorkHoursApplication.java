package com.example.workhours;

import android.app.ActionMode;
import android.app.Activity;
import android.app.Application;
import android.app.SearchEvent;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

public class WorkHoursApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPostCreated(Activity activity, Bundle savedInstanceState) {
                applySystemBarInsets(activity);
                installPageSwipe(activity);
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }

    private void applySystemBarInsets(Activity activity) {
        activity.getWindow().setDecorFitsSystemWindows(false);
        View content = activity.findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        content.requestApplyInsets();
    }

    private void installPageSwipe(Activity activity) {
        if (!(activity instanceof MainActivity) && !(activity instanceof WageActivity)) return;

        Window window = activity.getWindow();
        Window.Callback original = window.getCallback();
        if (original instanceof SwipeWindowCallback) return;
        window.setCallback(new SwipeWindowCallback(activity, original));
    }

    private static class SwipeWindowCallback implements Window.Callback {
        private final Activity activity;
        private final Window.Callback delegate;
        private final GestureDetector detector;
        private boolean switching;

        SwipeWindowCallback(Activity activity, Window.Callback delegate) {
            this.activity = activity;
            this.delegate = delegate;
            detector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
                private static final int MIN_DISTANCE = 120;
                private static final int MIN_VELOCITY = 180;

                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (switching || e1 == null || e2 == null) return false;
                    float dx = e2.getX() - e1.getX();
                    float dy = e2.getY() - e1.getY();
                    if (Math.abs(dx) < MIN_DISTANCE
                            || Math.abs(dx) <= Math.abs(dy) * 1.25f
                            || Math.abs(velocityX) < MIN_VELOCITY) {
                        return false;
                    }

                    if (dx < 0 && activity instanceof MainActivity) {
                        switching = true;
                        activity.startActivity(new Intent(activity, WageActivity.class));
                        activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
                        return true;
                    }

                    if (dx > 0 && activity instanceof WageActivity) {
                        switching = true;
                        activity.finish();
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
                        return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) switching = false;
            detector.onTouchEvent(event);
            return delegate.dispatchTouchEvent(event);
        }

        @Override public boolean dispatchKeyEvent(KeyEvent event) { return delegate.dispatchKeyEvent(event); }
        @Override public boolean dispatchKeyShortcutEvent(KeyEvent event) { return delegate.dispatchKeyShortcutEvent(event); }
        @Override public boolean dispatchTrackballEvent(MotionEvent event) { return delegate.dispatchTrackballEvent(event); }
        @Override public boolean dispatchGenericMotionEvent(MotionEvent event) { return delegate.dispatchGenericMotionEvent(event); }
        @Override public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) { return delegate.dispatchPopulateAccessibilityEvent(event); }
        @Override public View onCreatePanelView(int featureId) { return delegate.onCreatePanelView(featureId); }
        @Override public boolean onCreatePanelMenu(int featureId, Menu menu) { return delegate.onCreatePanelMenu(featureId, menu); }
        @Override public boolean onPreparePanel(int featureId, View view, Menu menu) { return delegate.onPreparePanel(featureId, view, menu); }
        @Override public boolean onMenuOpened(int featureId, Menu menu) { return delegate.onMenuOpened(featureId, menu); }
        @Override public boolean onMenuItemSelected(int featureId, MenuItem item) { return delegate.onMenuItemSelected(featureId, item); }
        @Override public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) { delegate.onWindowAttributesChanged(attrs); }
        @Override public void onContentChanged() { delegate.onContentChanged(); }
        @Override public void onWindowFocusChanged(boolean hasFocus) { delegate.onWindowFocusChanged(hasFocus); }
        @Override public void onAttachedToWindow() { delegate.onAttachedToWindow(); }
        @Override public void onDetachedFromWindow() { delegate.onDetachedFromWindow(); }
        @Override public void onPanelClosed(int featureId, Menu menu) { delegate.onPanelClosed(featureId, menu); }
        @Override public boolean onSearchRequested() { return delegate.onSearchRequested(); }
        @Override public boolean onSearchRequested(SearchEvent searchEvent) { return delegate.onSearchRequested(searchEvent); }
        @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) { return delegate.onWindowStartingActionMode(callback); }
        @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) { return delegate.onWindowStartingActionMode(callback, type); }
        @Override public void onActionModeStarted(ActionMode mode) { delegate.onActionModeStarted(mode); }
        @Override public void onActionModeFinished(ActionMode mode) { delegate.onActionModeFinished(mode); }
        @Override public void onProvideKeyboardShortcuts(List<android.view.KeyboardShortcutGroup> data, Menu menu, int deviceId) { delegate.onProvideKeyboardShortcuts(data, menu, deviceId); }
        @Override public void onPointerCaptureChanged(boolean hasCapture) { delegate.onPointerCaptureChanged(hasCapture); }
    }
}
