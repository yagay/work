from pathlib import Path

# AppThemeManager: add safe post-content system bar styling.
p=Path('app/src/main/java/com/example/workhours/AppThemeManager.java')
s=p.read_text()
s=s.replace('import android.content.res.Configuration;','import android.content.res.Configuration;\nimport android.view.Window;\nimport android.view.WindowInsetsController;')
anchor='''    static boolean apply(Activity activity) {\n        boolean dark = isDark(activity);\n        UiStyle.applyDark(dark);\n        return dark;\n    }\n'''
insert='''    static boolean apply(Activity activity) {\n        boolean dark = isDark(activity);\n        UiStyle.applyDark(dark);\n        return dark;\n    }\n\n    static void applySystemBars(Activity activity) {\n        boolean dark = isDark(activity);\n        Window window = activity.getWindow();\n        window.setStatusBarColor(UiStyle.PAGE_BG);\n        window.setNavigationBarColor(UiStyle.PAGE_BG);\n        try {\n            WindowInsetsController controller = window.getInsetsController();\n            if (controller != null) {\n                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS\n                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;\n                controller.setSystemBarsAppearance(dark ? 0 : mask, mask);\n            }\n        } catch (Throwable ignored) { }\n    }\n'''
if anchor not in s: raise SystemExit('AppThemeManager anchor missing')
s=s.replace(anchor,insert,1)
p.write_text(s)

# MainActivity: call after setContentView.
p=Path('app/src/main/java/com/example/workhours/MainActivity.java')
s=p.read_text()
old='''        setContentView(buildUi());\n        refreshAll();'''
new='''        setContentView(buildUi());\n        AppThemeManager.applySystemBars(this);\n        refreshAll();'''
if old not in s: raise SystemExit('MainActivity anchor missing')
s=s.replace(old,new,1)
p.write_text(s)

# SettingsActivity: call after setContentView.
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
old='''        setContentView(buildUi());\n        loadSettings();'''
new='''        setContentView(buildUi());\n        AppThemeManager.applySystemBars(this);\n        loadSettings();'''
if old not in s: raise SystemExit('SettingsActivity anchor missing')
s=s.replace(old,new,1)
p.write_text(s)

# WageActivity legacy screen: call after setContentView if present.
p=Path('app/src/main/java/com/example/workhours/WageActivity.java')
s=p.read_text()
idx=s.find('setContentView(')
if idx>=0:
    end=s.find(';',idx)
    if end>=0 and 'AppThemeManager.applySystemBars(this);' not in s[end:end+120]:
        s=s[:end+1]+'\n        AppThemeManager.applySystemBars(this);'+s[end+1:]
p.write_text(s)
