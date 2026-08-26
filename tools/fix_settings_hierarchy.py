from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

# Add a distinct first-level menu height while keeping compact setting rows.
s=s.replace('''    private static final int UI_ROW_DP = 42;\n    private static final int UI_ACTION_DP = 46;''','''    private static final int UI_ROW_DP = 42;\n    private static final int UI_ACTION_DP = 46;\n    private static final int UI_PRIMARY_MENU_DP = 52;''')

old='''        Button header = new Button(this);\n        header.setAllCaps(false);\n        header.setTextSize(UI_LABEL_SP);\n        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);\n        header.setPadding(dp(14), 0, dp(14), 0);\n        UiStyle.button(this, header, false);\n        wrapper.addView(header, new LinearLayout.LayoutParams(-1, dp(UI_ACTION_DP)));'''
new='''        Button header = new Button(this);\n        header.setAllCaps(false);\n        header.setTextSize(UI_SECTION_TITLE_SP);\n        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);\n        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);\n        header.setPadding(dp(16), 0, dp(14), 0);\n        header.setTextColor(UiStyle.PRIMARY);\n        header.setBackground(UiStyle.roundRect(this, UiStyle.PRIMARY_SOFT, 16, UiStyle.BORDER, 1));\n        wrapper.addView(header, new LinearLayout.LayoutParams(-1, dp(UI_PRIMARY_MENU_DP)));'''
if old not in s: raise SystemExit('primary header anchor missing')
s=s.replace(old,new,1)

# Indent expanded content slightly so group titles and compact rows visually belong under the first-level menu.
s=s.replace('''        content.setPadding(dp(4), dp(5), dp(4), dp(5));''','''        content.setPadding(dp(10), dp(7), dp(6), dp(5));''',1)

# Give first-level groups a little more separation than inner rows.
s=s.replace('''        wrapperParams.topMargin = dp(6);''','''        wrapperParams.topMargin = dp(9);''',1)

p.write_text(s)
