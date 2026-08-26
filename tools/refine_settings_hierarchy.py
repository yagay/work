from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

s=s.replace('''    private static final int UI_ROW_DP = 42;\n    private static final int UI_ACTION_DP = 46;''','''    private static final int UI_ROW_DP = 40;\n    private static final int UI_ACTION_DP = 46;\n    private static final int UI_SECTION_HEADER_DP = 54;''')

old='''        header.setAllCaps(false);\n        header.setTextSize(UI_LABEL_SP);\n        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);\n        header.setPadding(dp(14), 0, dp(14), 0);\n        UiStyle.button(this, header, false);\n        wrapper.addView(header, new LinearLayout.LayoutParams(-1, dp(UI_ACTION_DP)));'''
new='''        header.setAllCaps(false);\n        header.setTextSize(UI_SECTION_TITLE_SP);\n        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);\n        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);\n        header.setPadding(dp(16), 0, dp(16), 0);\n        header.setTextColor(UiStyle.PRIMARY);\n        header.setBackground(UiStyle.roundRect(this, UiStyle.PRIMARY_SOFT, 16, UiStyle.BORDER, 1));\n        header.setElevation(dp(1));\n        wrapper.addView(header, new LinearLayout.LayoutParams(-1, dp(UI_SECTION_HEADER_DP)));'''
if old not in s: raise SystemExit('header anchor missing')
s=s.replace(old,new,1)

old='''        TextView title = text(label, UI_LABEL_SP, true);'''
new='''        TextView title = text(label, UI_LABEL_SP, false);'''
if old not in s: raise SystemExit('row label anchor missing')
s=s.replace(old,new,1)

s=s.replace('''            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(40));''','''            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(38));''')

# Make right-side value boxes visually lighter than main section headers.
old='''        UiStyle.button(this,b,false);\n        b.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);'''
new='''        UiStyle.button(this,b,false);\n        b.setBackground(UiStyle.roundRect(this, UiStyle.INPUT_BG, 12, UiStyle.BORDER, 1));\n        b.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);'''
if old not in s: raise SystemExit('option button anchor missing')
s=s.replace(old,new,1)

# Ringtone choice was created manually rather than optionButton; align it too.
old='''        alarmRingtoneButton = new Button(this);\n        UiStyle.button(this, alarmRingtoneButton, false);'''
new='''        alarmRingtoneButton = new Button(this);\n        UiStyle.button(this, alarmRingtoneButton, false);\n        alarmRingtoneButton.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);\n        alarmRingtoneButton.setTextSize(UI_VALUE_SP);\n        alarmRingtoneButton.setBackground(UiStyle.roundRect(this, UiStyle.INPUT_BG, 12, UiStyle.BORDER, 1));'''
if old not in s: raise SystemExit('ringtone anchor missing')
s=s.replace(old,new,1)

# Slightly increase separation between major sections while keeping inner rows compact.
s=s.replace('''        wrapperParams.topMargin = dp(6);''','''        wrapperParams.topMargin = dp(10);''')
s=s.replace('''        content.setPadding(dp(4), dp(5), dp(4), dp(5));''','''        content.setPadding(dp(6), dp(6), dp(6), dp(6));''')

p.write_text(s)
