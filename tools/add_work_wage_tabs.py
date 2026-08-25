from pathlib import Path

# MainActivity: add quick segmented switch below header.
p = Path('app/src/main/java/com/example/workhours/MainActivity.java')
s = p.read_text()
old = '''        top.addView(settings, new LinearLayout.LayoutParams(dp(86), dp(48)));\n\n        buildMonthSection(root);\n'''
new = '''        top.addView(settings, new LinearLayout.LayoutParams(dp(86), dp(48)));\n\n        LinearLayout quickSwitch = horizontal();\n        LinearLayout.LayoutParams quickSwitchParams = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));\n        quickSwitchParams.topMargin = dp(14);\n        root.addView(quickSwitch, quickSwitchParams);\n        Button workTab = button("工时统计");\n        UiStyle.button(this, workTab, true);\n        workTab.setEnabled(false);\n        quickSwitch.addView(workTab, new LinearLayout.LayoutParams(0, dp(48), 1f));\n        Button wageTab = button("工资统计");\n        UiStyle.button(this, wageTab, false);\n        wageTab.setOnClickListener(v -> {\n            Intent intent = new Intent(this, WageActivity.class);\n            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);\n            startActivity(intent);\n        });\n        LinearLayout.LayoutParams wageTabParams = new LinearLayout.LayoutParams(0, dp(48), 1f);\n        wageTabParams.leftMargin = dp(8);\n        quickSwitch.addView(wageTab, wageTabParams);\n\n        buildMonthSection(root);\n'''
if old not in s:
    raise SystemExit('MainActivity header anchor not found')
s = s.replace(old, new, 1)
p.write_text(s)

# WageActivity: add matching quick segmented switch below header.
p = Path('app/src/main/java/com/example/workhours/WageActivity.java')
s = p.read_text()
old = '''        top.addView(text("工资统计", 25, true), new LinearLayout.LayoutParams(0, -2, 1f));\n\n        TextView settingsTitle = text("工资设置", 19, true);\n'''
new = '''        top.addView(text("工资统计", 25, true), new LinearLayout.LayoutParams(0, -2, 1f));\n\n        LinearLayout quickSwitch = horizontal();\n        LinearLayout.LayoutParams quickSwitchParams = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));\n        quickSwitchParams.topMargin = dp(14);\n        root.addView(quickSwitch, quickSwitchParams);\n        Button workTab = button("工时统计");\n        UiStyle.button(this, workTab, false);\n        workTab.setOnClickListener(v -> {\n            Intent intent = new Intent(this, MainActivity.class);\n            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);\n            startActivity(intent);\n        });\n        quickSwitch.addView(workTab, new LinearLayout.LayoutParams(0, dp(48), 1f));\n        Button wageTab = button("工资统计");\n        UiStyle.button(this, wageTab, true);\n        wageTab.setEnabled(false);\n        LinearLayout.LayoutParams wageTabParams = new LinearLayout.LayoutParams(0, dp(48), 1f);\n        wageTabParams.leftMargin = dp(8);\n        quickSwitch.addView(wageTab, wageTabParams);\n\n        TextView settingsTitle = text("工资设置", 19, true);\n'''
if old not in s:
    raise SystemExit('WageActivity header anchor not found')
s = s.replace(old, new, 1)
p.write_text(s)
