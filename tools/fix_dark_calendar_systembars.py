from pathlib import Path

# 1) UiStyle: add semantic calendar colors with light/dark variants.
p=Path('app/src/main/java/com/example/workhours/UiStyle.java')
s=p.read_text()
old='''    static int HINT = 0xFF9AA3B2;\n'''
new='''    static int HINT = 0xFF9AA3B2;\n    static int CAL_TODAY_BG = 0xFFE8F0FE;\n    static int CAL_TODAY_BORDER = 0xFFB8C8FF;\n    static int CAL_LEAVE_BG = 0xFFFFF0F0;\n    static int CAL_LEAVE_BORDER = 0xFFF2CACA;\n    static int CAL_OVERTIME_BG = 0xFFF1F8F3;\n    static int CAL_OVERTIME_BORDER = 0xFFCDE7D4;\n    static int CAL_OVERRIDE_BG = 0xFFFFF7E8;\n    static int CAL_OVERRIDE_BORDER = 0xFFF1D8A6;\n    static int CAL_HOLIDAY_BG = 0xFFFFF2F4;\n    static int CAL_HOLIDAY_BORDER = 0xFFF3CDD3;\n    static int CAL_REST_BG = 0xFFF4F6F9;\n    static int CAL_REST_BORDER = 0xFFE1E6EE;\n    static int CAL_DISABLED_TEXT = 0xFF9AA0A6;\n    static int WEEKEND_TEXT = 0xFF5F6368;\n    static int CAL_WEEKEND_BG = 0xFFFFF4F4;\n'''
if old not in s: raise SystemExit('UiStyle field anchor missing')
s=s.replace(old,new,1)
old='''            HINT = 0xFF778297;\n'''
new='''            HINT = 0xFF778297;\n            CAL_TODAY_BG = 0xFF1D2A49;\n            CAL_TODAY_BORDER = 0xFF5876D8;\n            CAL_LEAVE_BG = 0xFF3A2025;\n            CAL_LEAVE_BORDER = 0xFF84434B;\n            CAL_OVERTIME_BG = 0xFF173126;\n            CAL_OVERTIME_BORDER = 0xFF3F7458;\n            CAL_OVERRIDE_BG = 0xFF352B18;\n            CAL_OVERRIDE_BORDER = 0xFF806836;\n            CAL_HOLIDAY_BG = 0xFF3A2028;\n            CAL_HOLIDAY_BORDER = 0xFF844653;\n            CAL_REST_BG = 0xFF1B222D;\n            CAL_REST_BORDER = 0xFF374252;\n            CAL_DISABLED_TEXT = 0xFF697487;\n            WEEKEND_TEXT = 0xFF7F899A;\n            CAL_WEEKEND_BG = 0xFF2B2023;\n'''
if old not in s: raise SystemExit('UiStyle dark anchor missing')
s=s.replace(old,new,1)
old='''            HINT = 0xFF9AA3B2;\n        }\n'''
new='''            HINT = 0xFF9AA3B2;\n            CAL_TODAY_BG = 0xFFE8F0FE;\n            CAL_TODAY_BORDER = 0xFFB8C8FF;\n            CAL_LEAVE_BG = 0xFFFFF0F0;\n            CAL_LEAVE_BORDER = 0xFFF2CACA;\n            CAL_OVERTIME_BG = 0xFFF1F8F3;\n            CAL_OVERTIME_BORDER = 0xFFCDE7D4;\n            CAL_OVERRIDE_BG = 0xFFFFF7E8;\n            CAL_OVERRIDE_BORDER = 0xFFF1D8A6;\n            CAL_HOLIDAY_BG = 0xFFFFF2F4;\n            CAL_HOLIDAY_BORDER = 0xFFF3CDD3;\n            CAL_REST_BG = 0xFFF4F6F9;\n            CAL_REST_BORDER = 0xFFE1E6EE;\n            CAL_DISABLED_TEXT = 0xFF9AA0A6;\n            WEEKEND_TEXT = 0xFF5F6368;\n            CAL_WEEKEND_BG = 0xFFFFF4F4;\n        }\n'''
if old not in s: raise SystemExit('UiStyle light anchor missing')
s=s.replace(old,new,1)
p.write_text(s)

# 2) AppThemeManager: make transparent/edge-to-edge system areas inherit app background and remove forced contrast scrim.
p=Path('app/src/main/java/com/example/workhours/AppThemeManager.java')
s=p.read_text()
old='''        Window window = activity.getWindow();\n        window.setStatusBarColor(UiStyle.PAGE_BG);\n        window.setNavigationBarColor(UiStyle.PAGE_BG);\n        try {\n'''
new='''        Window window = activity.getWindow();\n        window.getDecorView().setBackgroundColor(UiStyle.PAGE_BG);\n        window.setStatusBarColor(UiStyle.PAGE_BG);\n        window.setNavigationBarColor(UiStyle.PAGE_BG);\n        try {\n            window.setStatusBarContrastEnforced(false);\n            window.setNavigationBarContrastEnforced(false);\n'''
if old not in s: raise SystemExit('AppThemeManager anchor missing')
s=s.replace(old,new,1)
p.write_text(s)

# 3) MainActivity: replace hard-coded light calendar palette everywhere with semantic theme colors.
p=Path('app/src/main/java/com/example/workhours/MainActivity.java')
s=p.read_text()
repls={
'if(i>=5)h.setTextColor(0xFF5F6368)':'if(i>=5)h.setTextColor(UiStyle.WEEKEND_TEXT)',
'UiStyle.roundRect(this,0xFFE8F0FE,12,0xFFB8C8FF,1)':'UiStyle.roundRect(this,UiStyle.CAL_TODAY_BG,12,UiStyle.CAL_TODAY_BORDER,1)',
'UiStyle.roundRect(this,0xFFFFF0F0,12,0xFFF2CACA,1)':'UiStyle.roundRect(this,UiStyle.CAL_LEAVE_BG,12,UiStyle.CAL_LEAVE_BORDER,1)',
'UiStyle.roundRect(this,0xFFF1F8F3,12,0xFFCDE7D4,1)':'UiStyle.roundRect(this,UiStyle.CAL_OVERTIME_BG,12,UiStyle.CAL_OVERTIME_BORDER,1)',
'UiStyle.roundRect(this,0xFFFFF7E8,12,0xFFF1D8A6,1)':'UiStyle.roundRect(this,UiStyle.CAL_OVERRIDE_BG,12,UiStyle.CAL_OVERRIDE_BORDER,1)',
'UiStyle.roundRect(this,0xFFFFF2F4,12,0xFFF3CDD3,1)':'UiStyle.roundRect(this,UiStyle.CAL_HOLIDAY_BG,12,UiStyle.CAL_HOLIDAY_BORDER,1)',
'UiStyle.roundRect(this,0xFFF4F6F9,12,0xFFE1E6EE,1)':'UiStyle.roundRect(this,UiStyle.CAL_REST_BG,12,UiStyle.CAL_REST_BORDER,1)',
'dt.setTextColor(0xFF9AA0A6)':'dt.setTextColor(UiStyle.CAL_DISABLED_TEXT)'
}
for a,b in repls.items():
    s=s.replace(a,b)
p.write_text(s)

# 4) Settings monthly-rest calendar: remove hard-coded light weekend card/text colors.
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
s=s.replace('if (i>=5) h.setTextColor(0xFFB14A4A);','if (i>=5) h.setTextColor(UiStyle.WEEKEND_TEXT);')
s=s.replace('(weekend ? 0xFFB14A4A : UiStyle.TEXT)', '(weekend ? UiStyle.WEEKEND_TEXT : UiStyle.TEXT)')
s=s.replace('(weekend ? 0xFFFFF4F4 : UiStyle.CARD_BG)', '(weekend ? UiStyle.CAL_WEEKEND_BG : UiStyle.CARD_BG)')
p.write_text(s)
