from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

# Compact metrics while keeping readable text sizes.
s=s.replace('private static final int UI_ROW_DP = 48;', 'private static final int UI_ROW_DP = 42;')
s=s.replace('private static final int UI_ACTION_DP = 50;', 'private static final int UI_ACTION_DP = 46;')

# Tighten setting-row spacing/minimum visual height.
s=s.replace('''        row.setMinimumHeight(dp(UI_ROW_DP));\n        row.setPadding(0, dp(4), 0, dp(4));''', '''        row.setMinimumHeight(dp(UI_ROW_DP));\n        row.setPadding(0, dp(1), 0, dp(1));''')

# Tighten input internal padding; preserve horizontal breathing room.
s=s.replace('''        view.setTextSize(UI_LABEL_SP);\n        view.setHint(hint);\n        UiStyle.input(this, view);''', '''        view.setTextSize(UI_LABEL_SP);\n        view.setHint(hint);\n        UiStyle.input(this, view);\n        view.setPadding(dp(12), dp(4), dp(12), dp(4));''')

# Compact option buttons.
s=s.replace('''        b.setMinHeight(dp(UI_ROW_DP));\n        b.setPadding(dp(10),0,dp(12),0);''', '''        b.setMinHeight(0);\n        b.setMinimumHeight(0);\n        b.setPadding(dp(8),0,dp(10),0);''')

# Smaller spacing between section content and rows.
s=s.replace('content.setPadding(dp(4), dp(8), dp(4), dp(8));', 'content.setPadding(dp(4), dp(5), dp(4), dp(5));')
s=s.replace('wrapperParams.topMargin = dp(8);', 'wrapperParams.topMargin = dp(6);')

# Add compact single-choice dialog helper before chooseOption.
anchor='''    private void chooseOption(String title, Button button, String[] labels, String[] values) {'''
helper='''    private void showCompactSingleChoice(String title, String[] labels, int checked, android.content.DialogInterface.OnClickListener listener) {\n        LinearLayout box = new LinearLayout(this);\n        box.setOrientation(LinearLayout.VERTICAL);\n        box.setPadding(dp(8), dp(2), dp(8), dp(4));\n        final AlertDialog[] holder = new AlertDialog[1];\n        for (int i = 0; i < labels.length; i++) {\n            final int index = i;\n            RadioButton item = new RadioButton(this);\n            item.setText(labels[i]);\n            item.setTextSize(UI_LABEL_SP);\n            item.setTextColor(UiStyle.TEXT);\n            item.setChecked(i == checked);\n            item.setGravity(Gravity.CENTER_VERTICAL);\n            item.setPadding(dp(8), 0, dp(8), 0);\n            item.setMinHeight(0);\n            item.setMinimumHeight(0);\n            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(40));\n            box.addView(item, lp);\n            item.setOnClickListener(v -> {\n                if (holder[0] != null) {\n                    listener.onClick(holder[0], index);\n                }\n            });\n        }\n        AlertDialog dialog = new AlertDialog.Builder(this)\n                .setTitle(title)\n                .setView(box)\n                .setNegativeButton("取消", null)\n                .create();\n        holder[0] = dialog;\n        dialog.show();\n    }\n\n'''+anchor
if anchor not in s: raise SystemExit('chooseOption anchor missing')
s=s.replace(anchor, helper, 1)

# Replace theme dialog with compact helper.
old='''        new AlertDialog.Builder(this)\n                .setTitle("主题模式")\n                .setSingleChoiceItems(labels, checked, (dialog, which) -> {\n                    dialog.dismiss();\n                    setAppTheme(values[which]);\n                })\n                .setNegativeButton("取消", null)\n                .show();'''
new='''        showCompactSingleChoice("主题模式", labels, checked, (dialog, which) -> {\n            dialog.dismiss();\n            setAppTheme(values[which]);\n        });'''
if old not in s: raise SystemExit('theme dialog anchor missing')
s=s.replace(old,new,1)

# Replace rest-rule dialog.
old='''        new AlertDialog.Builder(this)\n                .setTitle("休息规则")\n                .setSingleChoiceItems(labels, checked, (dialog, which) -> {\n                    dialog.dismiss();\n                    setRestRuleMode(which == 1 ? "monthly" : "weekly");\n                })\n                .setNegativeButton("取消", null)\n                .show();'''
new='''        showCompactSingleChoice("休息规则", labels, checked, (dialog, which) -> {\n            dialog.dismiss();\n            setRestRuleMode(which == 1 ? "monthly" : "weekly");\n        });'''
if old not in s: raise SystemExit('rest dialog anchor missing')
s=s.replace(old,new,1)

# Replace generic string choice dialog.
old='''        new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,checked,(d,w)->{\n            d.dismiss();\n            button.setTag(values[w]); button.setText(labels[w] + "  ›");\n            String key = button==vibrationPatternButton ? WorkAlarmOptions.VIBRATE_PATTERN_KEY : button==alarmKeyActionButton ? WorkAlarmOptions.KEY_ACTION_KEY : WorkAlarmOptions.BACK_ACTION_KEY;\n            prefs.edit().putString(key,values[w]).apply();\n        }).setNegativeButton("取消",null).show();'''
new='''        showCompactSingleChoice(title, labels, checked, (d,w)->{\n            d.dismiss();\n            button.setTag(values[w]); button.setText(labels[w] + "  ›");\n            String key = button==vibrationPatternButton ? WorkAlarmOptions.VIBRATE_PATTERN_KEY : button==alarmKeyActionButton ? WorkAlarmOptions.KEY_ACTION_KEY : WorkAlarmOptions.BACK_ACTION_KEY;\n            prefs.edit().putString(key,values[w]).apply();\n        });'''
if old not in s: raise SystemExit('generic dialog anchor missing')
s=s.replace(old,new,1)

# Replace generic int choice dialog.
old='''        new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,checked,(d,w)->{\n            d.dismiss();\n            button.setTag(values[w]);button.setText(labels[w] + "  ›");prefs.edit().putInt(key,values[w]).apply();\n        }).setNegativeButton("取消",null).show();'''
new='''        showCompactSingleChoice(title, labels, checked, (d,w)->{\n            d.dismiss();\n            button.setTag(values[w]);button.setText(labels[w] + "  ›");prefs.edit().putInt(key,values[w]).apply();\n        });'''
if old not in s: raise SystemExit('int dialog anchor missing')
s=s.replace(old,new,1)

p.write_text(s)
