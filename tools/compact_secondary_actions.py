from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

old='''        Button testAlarm = new Button(this); testAlarm.setText("测试闹钟（立即响铃）"); UiStyle.button(this, testAlarm, true);\n        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(-1, dp(UI_ACTION_DP)); testParams.topMargin=dp(12); alarmBehaviorSection.addView(testAlarm,testParams);\n        testAlarm.setOnClickListener(v -> testAlarmNow());'''
new='''        Button testAlarm = actionRow("测试闹钟");\n        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(-1, dp(UI_ROW_DP));\n        testParams.topMargin = dp(8);\n        alarmBehaviorSection.addView(testAlarm, testParams);\n        testAlarm.setOnClickListener(v -> testAlarmNow());'''
if old not in s: raise SystemExit('test alarm block missing')
s=s.replace(old,new,1)

old='''        Button addWageChange = new Button(this);\n        addWageChange.setText("＋ 新增工资变更");\n        UiStyle.button(this, addWageChange, true);\n        LinearLayout.LayoutParams addWageParams = new LinearLayout.LayoutParams(-1, dp(UI_ACTION_DP));\n        addWageParams.topMargin = dp(10);\n        wageSection.addView(addWageChange, addWageParams);\n        addWageChange.setOnClickListener(v -> showAddWageChangeDialog());'''
new='''        Button addWageChange = actionRow("＋  新增工资变更");\n        LinearLayout.LayoutParams addWageParams = new LinearLayout.LayoutParams(-1, dp(UI_ROW_DP));\n        addWageParams.topMargin = dp(8);\n        wageSection.addView(addWageChange, addWageParams);\n        addWageChange.setOnClickListener(v -> showAddWageChangeDialog());'''
if old not in s: raise SystemExit('add wage block missing')
s=s.replace(old,new,1)

old='''        Button export = new Button(this);\n        UiStyle.button(this, export, false);\n        export.setText("导出全部数据");\n        export.setOnClickListener(v -> exportBackup());\n        backupSection.addView(export, new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(UI_ACTION_DP)));\n\n        Button importButton = new Button(this);\n        UiStyle.button(this, importButton, false);\n        importButton.setText("导入 / 恢复数据");\n        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(UI_ACTION_DP));\n        importParams.topMargin = dp(8);\n        backupSection.addView(importButton, importParams);\n        importButton.setOnClickListener(v -> importBackup());'''
new='''        Button export = actionRow("导出全部数据");\n        export.setOnClickListener(v -> exportBackup());\n        backupSection.addView(export, new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(UI_ROW_DP)));\n\n        Button importButton = actionRow("导入 / 恢复数据");\n        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(UI_ROW_DP));\n        importParams.topMargin = dp(6);\n        backupSection.addView(importButton, importParams);\n        importButton.setOnClickListener(v -> importBackup());'''
if old not in s: raise SystemExit('backup action blocks missing')
s=s.replace(old,new,1)

anchor='''    private Button optionButton() {\n        Button b=new Button(this);\n        UiStyle.button(this,b,false);\n        b.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);\n        b.setTextSize(UI_VALUE_SP);\n        b.setMinHeight(0);\n        b.setMinimumHeight(0);\n        b.setPadding(dp(8),0,dp(10),0);\n        return b;\n    }'''
addition=anchor+'''\n\n    private Button actionRow(String label) {\n        Button b = new Button(this);\n        b.setAllCaps(false);\n        b.setText(label + "  ›");\n        b.setTextSize(UI_LABEL_SP);\n        b.setTextColor(UiStyle.TEXT);\n        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);\n        b.setMinHeight(0);\n        b.setMinimumHeight(0);\n        b.setPadding(dp(14), 0, dp(12), 0);\n        b.setBackground(UiStyle.roundRect(this, UiStyle.CARD_BG, 12, UiStyle.BORDER, 1));\n        return b;\n    }'''
if anchor not in s: raise SystemExit('optionButton anchor missing')
s=s.replace(anchor,addition,1)

p.write_text(s)
