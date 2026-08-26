from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

# Theme: replace three inline buttons with one current-value row opening a dialog.
s=s.replace('''    private Button themeSystemButton;\n    private Button themeLightButton;\n    private Button themeDarkButton;''','''    private Button themeModeButton;''')
old='''        LinearLayout themeRow = new LinearLayout(this);\n        themeRow.setOrientation(LinearLayout.HORIZONTAL);\n        appearanceSection.addView(themeRow);\n        themeSystemButton = new Button(this); themeSystemButton.setText("跟随系统");\n        themeLightButton = new Button(this); themeLightButton.setText("浅色");\n        themeDarkButton = new Button(this); themeDarkButton.setText("深色");\n        themeSystemButton.setOnClickListener(v -> setAppTheme(AppThemeManager.SYSTEM));\n        themeLightButton.setOnClickListener(v -> setAppTheme(AppThemeManager.LIGHT));\n        themeDarkButton.setOnClickListener(v -> setAppTheme(AppThemeManager.DARK));\n        themeRow.addView(themeSystemButton, new LinearLayout.LayoutParams(0, dp(46), 1f));\n        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(46), 1f); tlp.leftMargin=dp(6); themeRow.addView(themeLightButton, tlp);\n        LinearLayout.LayoutParams tdp = new LinearLayout.LayoutParams(0, dp(46), 1f); tdp.leftMargin=dp(6); themeRow.addView(themeDarkButton, tdp);\n        refreshThemeButtons();'''
new='''        themeModeButton = optionButton();\n        themeModeButton.setOnClickListener(v -> chooseThemeMode());\n        LinearLayout themeRow = settingInputRow("主题模式", dp(180));\n        themeRow.addView(themeModeButton, compactInputParams(dp(180)));\n        appearanceSection.addView(themeRow);\n        refreshThemeButton();'''
if old not in s: raise SystemExit('theme ui anchor missing')
s=s.replace(old,new,1)

# Rest rule: replace two prominent mode buttons with a single current-value row/dialog.
old='''        LinearLayout restModeRow = new LinearLayout(this);\n        restModeRow.setOrientation(LinearLayout.HORIZONTAL);\n        restSection.addView(restModeRow);\n        weeklyRestModeButton = new Button(this);\n        weeklyRestModeButton.setText("每周休息日");\n        weeklyRestModeButton.setOnClickListener(v -> setRestRuleMode("weekly"));\n        restModeRow.addView(weeklyRestModeButton, new LinearLayout.LayoutParams(0, dp(48), 1f));\n        monthlyRestModeButton = new Button(this);\n        monthlyRestModeButton.setText("每月固定休息日");\n        monthlyRestModeButton.setOnClickListener(v -> setRestRuleMode("monthly"));\n        LinearLayout.LayoutParams mrmp = new LinearLayout.LayoutParams(0, dp(48), 1f);\n        mrmp.leftMargin = dp(8);\n        restModeRow.addView(monthlyRestModeButton, mrmp);'''
new='''        weeklyRestModeButton = optionButton();\n        weeklyRestModeButton.setOnClickListener(v -> chooseRestRuleMode());\n        LinearLayout restModeRow = settingInputRow("规则类型", dp(190));\n        restModeRow.addView(weeklyRestModeButton, compactInputParams(dp(190)));\n        restSection.addView(restModeRow);\n        monthlyRestModeButton = null;'''
if old not in s: raise SystemExit('rest ui anchor missing')
s=s.replace(old,new,1)

# Rest mode state: one choice button instead of two selected buttons.
old='''        if (weeklyRestModeButton != null) { weeklyRestModeButton.setSelected(!monthly); updateRestModeButtonStyle(weeklyRestModeButton, !monthly); }\n        if (monthlyRestModeButton != null) { monthlyRestModeButton.setSelected(monthly); updateRestModeButtonStyle(monthlyRestModeButton, monthly); }'''
new='''        if (weeklyRestModeButton != null) {\n            weeklyRestModeButton.setTag(restRuleMode);\n            weeklyRestModeButton.setText((monthly ? "每月固定休息日" : "每周休息日") + "  ›");\n        }'''
if old not in s: raise SystemExit('rest state anchor missing')
s=s.replace(old,new,1)

# Make enum choice buttons look like right-side current-value affordances.
old='''    private Button optionButton() {\n        Button b=new Button(this); UiStyle.button(this,b,false); return b;\n    }'''
new='''    private Button optionButton() {\n        Button b=new Button(this);\n        UiStyle.button(this,b,false);\n        b.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);\n        b.setTextSize(14);\n        b.setPadding(dp(10),0,dp(12),0);\n        return b;\n    }'''
if old not in s: raise SystemExit('optionButton anchor missing')
s=s.replace(old,new,1)

# Append chevrons to current values and dialog selections.
s=s.replace('''button.setTag(values[w]); button.setText(labels[w]);''','''button.setTag(values[w]); button.setText(labels[w] + "  ›");''')
s=s.replace('''button.setTag(values[w]);button.setText(labels[w]);prefs.edit().putInt(key,values[w]).apply();''','''button.setTag(values[w]);button.setText(labels[w] + "  ›");prefs.edit().putInt(key,values[w]).apply();''')
s=s.replace('''button.setTag(values[found]);button.setText(labels[found]);''','''button.setTag(values[found]);button.setText(labels[found] + "  ›");''')
# Above replacement occurs in both string/int helper methods if exact formatting matches.

# Ringtone/holiday current-value buttons also use chevrons consistently.
s=s.replace('''alarmRingtoneButton.setText(title);''','''alarmRingtoneButton.setText(title + "  ›");''')
s=s.replace('''alarmRingtoneButton.setText("系统默认");''','''alarmRingtoneButton.setText("系统默认  ›");''')

# Replace theme button painting helpers with single choice dialog.
old='''    private void refreshThemeButtons() {\n        if (themeSystemButton == null) return;\n        String mode = AppThemeManager.mode(this);\n        paintThemeButton(themeSystemButton, AppThemeManager.SYSTEM.equals(mode));\n        paintThemeButton(themeLightButton, AppThemeManager.LIGHT.equals(mode));\n        paintThemeButton(themeDarkButton, AppThemeManager.DARK.equals(mode));\n    }\n\n    private void paintThemeButton(Button button, boolean selected) {\n        UiStyle.button(this, button, selected);\n    }'''
new='''    private void refreshThemeButton() {\n        if (themeModeButton == null) return;\n        themeModeButton.setText(AppThemeManager.label(AppThemeManager.mode(this)) + "  ›");\n    }\n\n    private void chooseThemeMode() {\n        String[] labels = {"跟随系统", "浅色", "深色"};\n        String[] values = {AppThemeManager.SYSTEM, AppThemeManager.LIGHT, AppThemeManager.DARK};\n        String current = AppThemeManager.mode(this);\n        int checked = AppThemeManager.LIGHT.equals(current) ? 1 : AppThemeManager.DARK.equals(current) ? 2 : 0;\n        new AlertDialog.Builder(this)\n                .setTitle("主题模式")\n                .setSingleChoiceItems(labels, checked, (dialog, which) -> {\n                    dialog.dismiss();\n                    setAppTheme(values[which]);\n                })\n                .setNegativeButton("取消", null)\n                .show();\n    }\n\n    private void chooseRestRuleMode() {\n        String[] labels = {"每周休息日", "每月固定休息日"};\n        int checked = "monthly".equals(restRuleMode) ? 1 : 0;\n        new AlertDialog.Builder(this)\n                .setTitle("休息规则")\n                .setSingleChoiceItems(labels, checked, (dialog, which) -> {\n                    dialog.dismiss();\n                    setRestRuleMode(which == 1 ? "monthly" : "weekly");\n                })\n                .setNegativeButton("取消", null)\n                .show();\n    }'''
if old not in s: raise SystemExit('theme helpers anchor missing')
s=s.replace(old,new,1)

# Ensure load path calls the new theme refresh helper if any old call remains.
s=s.replace('refreshThemeButtons();','refreshThemeButton();')

# Improve choice dialogs to show current selection rather than a plain item list.
old='''    private void chooseOption(String title, Button button, String[] labels, String[] values) {\n        new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,w)->{\n            button.setTag(values[w]); button.setText(labels[w] + "  ›");\n            String key = button==vibrationPatternButton ? WorkAlarmOptions.VIBRATE_PATTERN_KEY : button==alarmKeyActionButton ? WorkAlarmOptions.KEY_ACTION_KEY : WorkAlarmOptions.BACK_ACTION_KEY;\n            prefs.edit().putString(key,values[w]).apply();\n        }).show();\n    }'''
new='''    private void chooseOption(String title, Button button, String[] labels, String[] values) {\n        String current = button.getTag() == null ? "" : String.valueOf(button.getTag());\n        int checked = 0; for (int i=0;i<values.length;i++) if (values[i].equals(current)) { checked=i; break; }\n        new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,checked,(d,w)->{\n            d.dismiss();\n            button.setTag(values[w]); button.setText(labels[w] + "  ›");\n            String key = button==vibrationPatternButton ? WorkAlarmOptions.VIBRATE_PATTERN_KEY : button==alarmKeyActionButton ? WorkAlarmOptions.KEY_ACTION_KEY : WorkAlarmOptions.BACK_ACTION_KEY;\n            prefs.edit().putString(key,values[w]).apply();\n        }).setNegativeButton("取消",null).show();\n    }'''
if old not in s: raise SystemExit('chooseOption anchor missing')
s=s.replace(old,new,1)

old='''    private void chooseIntOption(String title, Button button, String[] labels, int[] values, String key) {\n        new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,w)->{button.setTag(values[w]);button.setText(labels[w] + "  ›");prefs.edit().putInt(key,values[w]).apply();}).show();\n    }'''
new='''    private void chooseIntOption(String title, Button button, String[] labels, int[] values, String key) {\n        int current = button.getTag() instanceof Integer ? (Integer)button.getTag() : values[0];\n        int checked = 0; for (int i=0;i<values.length;i++) if (values[i]==current) { checked=i; break; }\n        new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,checked,(d,w)->{\n            d.dismiss();\n            button.setTag(values[w]);button.setText(labels[w] + "  ›");prefs.edit().putInt(key,values[w]).apply();\n        }).setNegativeButton("取消",null).show();\n    }'''
if old not in s: raise SystemExit('chooseIntOption anchor missing')
s=s.replace(old,new,1)

p.write_text(s)
