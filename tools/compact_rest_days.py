from pathlib import Path

p = Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s = p.read_text()

s = s.replace('private final CheckBox[] restDayChecks = new CheckBox[7];',
              'private final Button[] restDayButtons = new Button[7];')

old = '''        TextView weeklyInfo = text("勾选一星期中固定休息的日期。默认周六、周日休息。", 13, false);\n        weeklyInfo.setPadding(0, 0, 0, dp(6));\n        root.addView(weeklyInfo);\n        String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};\n        for (int i = 0; i < 7; i++) {\n            restDayChecks[i] = new CheckBox(this);\n            restDayChecks[i].setText(names[i] + "休息");\n            restDayChecks[i].setTextSize(16);\n            root.addView(restDayChecks[i]);\n        }\n'''
new = '''        TextView weeklyInfo = text("点选固定休息日；高亮表示休息。默认周六、周日休息。", 13, false);\n        weeklyInfo.setPadding(0, 0, 0, dp(8));\n        root.addView(weeklyInfo);\n        String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};\n        LinearLayout restRow1 = new LinearLayout(this);\n        restRow1.setOrientation(LinearLayout.HORIZONTAL);\n        root.addView(restRow1);\n        LinearLayout restRow2 = new LinearLayout(this);\n        restRow2.setOrientation(LinearLayout.HORIZONTAL);\n        LinearLayout.LayoutParams restRow2Params = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);\n        restRow2Params.topMargin = dp(8);\n        root.addView(restRow2, restRow2Params);\n        for (int i = 0; i < 7; i++) {\n            final int index = i;\n            Button day = new Button(this);\n            day.setText(names[i]);\n            day.setTextSize(14);\n            day.setAllCaps(false);\n            day.setMinHeight(0);\n            day.setMinWidth(0);\n            day.setPadding(dp(6), 0, dp(6), 0);\n            day.setOnClickListener(v -> {\n                day.setSelected(!day.isSelected());\n                updateRestDayButtonStyle(day);\n            });\n            restDayButtons[i] = day;\n            LinearLayout target = i < 4 ? restRow1 : restRow2;\n            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(0, dp(44), 1f);\n            if ((i < 4 && i > 0) || (i >= 4 && i > 4)) dayParams.leftMargin = dp(8);\n            target.addView(day, dayParams);\n            updateRestDayButtonStyle(day);\n        }\n'''
if old not in s:
    raise SystemExit('weekly block not found')
s = s.replace(old, new, 1)

s = s.replace('restDayChecks[i].setChecked(!wasWorkDay);',
              'restDayButtons[i].setSelected(!wasWorkDay);\n            updateRestDayButtonStyle(restDayButtons[i]);')
s = s.replace('editor.putBoolean("day_" + i, !restDayChecks[i].isChecked());',
              'editor.putBoolean("day_" + i, !restDayButtons[i].isSelected());')

anchor = '''    private LinearLayout settingInputRow(String label, int inputWidth) {\n'''
helper = '''    private void updateRestDayButtonStyle(Button button) {\n        boolean rest = button.isSelected();\n        button.setTextColor(rest ? android.graphics.Color.WHITE : UiStyle.TEXT);\n        button.setBackground(UiStyle.roundRect(this,\n                rest ? UiStyle.PRIMARY : UiStyle.CARD_BG,\n                14, rest ? UiStyle.PRIMARY : UiStyle.BORDER, 1));\n    }\n\n'''
if anchor not in s:
    raise SystemExit('helper anchor missing')
s = s.replace(anchor, helper + anchor, 1)

p.write_text(s)
