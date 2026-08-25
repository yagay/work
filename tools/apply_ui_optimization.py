from pathlib import Path

# ---------- SettingsActivity ----------
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
s=s.replace('import android.text.InputType;','import android.text.InputType;\nimport android.text.Editable;\nimport android.text.TextWatcher;')
s=s.replace('    private EditText monthlyRestInput;','    private EditText monthlyRestInput;\n    private Button monthlyRestButton;\n    private LinearLayout alarmOptionsGroup;')

# Realtime daily-hours preview: remove manual calculation button.
old='''        previewText = text("", 15, true);\n        previewText.setPadding(0, dp(12), 0, dp(8));\n        root.addView(previewText);\n        Button preview = new Button(this);\n        UiStyle.button(this, preview, false);\n        preview.setText("计算每天工时");\n        preview.setOnClickListener(v -> updatePreview(true));\n        root.addView(preview, new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));\n'''
new='''        LinearLayout dailyHoursRow = settingInputRow("每天工时（自动）", dp(126));\n        previewText = text("", 16, true);\n        previewText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);\n        dailyHoursRow.addView(previewText, compactInputParams(dp(126)));\n        root.addView(dailyHoursRow);\n        TextWatcher hoursWatcher = new TextWatcher() {\n            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }\n            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updatePreview(false); }\n            @Override public void afterTextChanged(Editable s) { }\n        };\n        startInput.addTextChangedListener(hoursWatcher);\n        endInput.addTextChangedListener(hoursWatcher);\n        breakInput.addTextChangedListener(hoursWatcher);\n'''
if old not in s: raise SystemExit('preview block missing')
s=s.replace(old,new,1)

# Alarm options under master switch.
old='''        alarmFollowWorkTimeCheck = new CheckBox(this);\n        alarmFollowWorkTimeCheck.setText("闹钟时间跟随上班时间");\n        alarmFollowWorkTimeCheck.setTextSize(16);\n        root.addView(alarmFollowWorkTimeCheck);\n\n        alarmTimeGroup = settingInputRow("自定义闹钟时间", dp(126));\n        alarmTimeInput = input("07:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);\n        alarmTimeGroup.addView(alarmTimeInput, compactInputParams(dp(126)));\n        root.addView(alarmTimeGroup);\n\n        Runnable updateAlarmTimeVisibility = () -> alarmTimeGroup.setVisibility(\n                alarmFollowWorkTimeCheck.isChecked() ? android.view.View.GONE : android.view.View.VISIBLE);\n        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmTimeVisibility.run());\n\n        LinearLayout updateTimeRow = settingInputRow("自动更新闹钟时间", dp(126));\n        alarmUpdateTimeInput = input("12:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);\n        updateTimeRow.addView(alarmUpdateTimeInput, compactInputParams(dp(126)));\n        root.addView(updateTimeRow);\n        TextView updateTimeInfo = text("每周日到这个时间自动重新计算下一周工作日并同步系统时钟。系统省电策略可能让后台执行稍有延迟。", 13, false);\n        updateTimeInfo.setPadding(0, dp(4), 0, 0);\n        root.addView(updateTimeInfo);\n'''
new='''        alarmOptionsGroup = new LinearLayout(this);\n        alarmOptionsGroup.setOrientation(LinearLayout.VERTICAL);\n        root.addView(alarmOptionsGroup);\n\n        alarmFollowWorkTimeCheck = new CheckBox(this);\n        alarmFollowWorkTimeCheck.setText("闹钟时间跟随上班时间");\n        alarmFollowWorkTimeCheck.setTextSize(16);\n        alarmOptionsGroup.addView(alarmFollowWorkTimeCheck);\n\n        alarmTimeGroup = settingInputRow("自定义闹钟时间", dp(126));\n        alarmTimeInput = input("07:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);\n        alarmTimeGroup.addView(alarmTimeInput, compactInputParams(dp(126)));\n        alarmOptionsGroup.addView(alarmTimeGroup);\n\n        Runnable updateAlarmTimeVisibility = () -> alarmTimeGroup.setVisibility(\n                alarmFollowWorkTimeCheck.isChecked() ? android.view.View.GONE : android.view.View.VISIBLE);\n        alarmFollowWorkTimeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmTimeVisibility.run());\n\n        LinearLayout updateTimeRow = settingInputRow("每周同步时间", dp(126));\n        alarmUpdateTimeInput = input("12:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);\n        updateTimeRow.addView(alarmUpdateTimeInput, compactInputParams(dp(126)));\n        alarmOptionsGroup.addView(updateTimeRow);\n        TextView updateTimeInfo = text("每周日到这个时间重新计算下一周工作日并同步系统时钟。", 13, false);\n        updateTimeInfo.setPadding(0, dp(4), 0, 0);\n        alarmOptionsGroup.addView(updateTimeInfo);\n'''
if old not in s: raise SystemExit('alarm options block missing')
s=s.replace(old,new,1)

# Extend master checkbox listener to collapse children.
old='''        workAlarmCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {\n            if (isChecked) WorkAlarmNotification.requestPermissionIfNeeded(this);\n        });'''
new='''        workAlarmCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {\n            if (isChecked) WorkAlarmNotification.requestPermissionIfNeeded(this);\n            if (alarmOptionsGroup != null) alarmOptionsGroup.setVisibility(\n                    isChecked ? android.view.View.VISIBLE : android.view.View.GONE);\n        });'''
s=s.replace(old,new,1)

# Monthly rest day becomes picker; keep hidden EditText as storage to preserve existing logic.
old='''        TextView monthlyInfo = text("输入每月固定休息的日期，用逗号分隔，例如：5, 15, 28。留空表示不设置。", 13, false);\n        monthlyInfo.setPadding(0, 0, 0, dp(6));\n        root.addView(monthlyInfo);\n        monthlyRestInput = input("例如：5, 15, 28", InputType.TYPE_CLASS_TEXT);\n        root.addView(monthlyRestInput);\n'''
new='''        TextView monthlyInfo = text("点击选择每月固定休息的日期。", 13, false);\n        monthlyInfo.setPadding(0, 0, 0, dp(6));\n        root.addView(monthlyInfo);\n        monthlyRestInput = input("", InputType.TYPE_CLASS_TEXT);\n        monthlyRestInput.setVisibility(android.view.View.GONE);\n        root.addView(monthlyRestInput);\n        monthlyRestButton = new Button(this);\n        UiStyle.button(this, monthlyRestButton, false);\n        monthlyRestButton.setOnClickListener(v -> showMonthlyRestPicker());\n        root.addView(monthlyRestButton, new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));\n'''
if old not in s: raise SystemExit('monthly block missing')
s=s.replace(old,new,1)

# Load state for alarm group and monthly button.
needle='''        alarmTimeGroup.setVisibility(alarmFollowWorkTimeCheck.isChecked()\n                ? android.view.View.GONE : android.view.View.VISIBLE);\n'''
rep=needle+'''        alarmOptionsGroup.setVisibility(workAlarmCheck.isChecked()\n                ? android.view.View.VISIBLE : android.view.View.GONE);\n        updateMonthlyRestButton();\n'''
s=s.replace(needle,rep,1)

# Update monthly button after save.
s=s.replace('''        monthlyRestInput.setText(monthlyRestDays);\n        updatePreview(false);''','''        monthlyRestInput.setText(monthlyRestDays);\n        updateMonthlyRestButton();\n        updatePreview(false);''',1)

# Add monthly picker helpers before settingInputRow.
anchor='''    private LinearLayout settingInputRow(String label, int inputWidth) {\n'''
helper='''    private void updateMonthlyRestButton() {\n        if (monthlyRestButton == null || monthlyRestInput == null) return;\n        String raw = monthlyRestInput.getText().toString().trim();\n        monthlyRestButton.setText(raw.isEmpty() ? "未设置固定日期" : "已选：" + raw);\n    }\n\n    private void showMonthlyRestPicker() {\n        final boolean[] selected = new boolean[32];\n        String raw = monthlyRestInput.getText().toString().trim();\n        if (!raw.isEmpty()) {\n            for (String part : raw.replace('，', ',').split(",")) {\n                try { int d = Integer.parseInt(part.trim()); if (d >= 1 && d <= 31) selected[d] = true; }\n                catch (Exception ignored) { }\n            }\n        }\n        GridLayout grid = new GridLayout(this);\n        grid.setColumnCount(7);\n        grid.setPadding(dp(12), dp(8), dp(12), dp(8));\n        Button[] buttons = new Button[32];\n        for (int d = 1; d <= 31; d++) {\n            final int day = d;\n            Button b = new Button(this);\n            buttons[d] = b;\n            b.setText(String.valueOf(d));\n            b.setTextSize(14);\n            b.setMinWidth(0); b.setMinHeight(0); b.setPadding(0,0,0,0);\n            Runnable paint = () -> {\n                b.setTextColor(selected[day] ? android.graphics.Color.WHITE : UiStyle.TEXT);\n                b.setBackground(UiStyle.roundRect(this, selected[day] ? UiStyle.PRIMARY : UiStyle.CARD_BG,\n                        12, selected[day] ? UiStyle.PRIMARY : UiStyle.BORDER, 1));\n            };\n            b.setOnClickListener(v -> { selected[day] = !selected[day]; paint.run(); });\n            paint.run();\n            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();\n            gp.width = 0; gp.height = dp(44); gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);\n            gp.setMargins(dp(3), dp(3), dp(3), dp(3));\n            grid.addView(b, gp);\n        }\n        new AlertDialog.Builder(this)\n                .setTitle("每月固定休息日")\n                .setView(grid)\n                .setNegativeButton("取消", null)\n                .setNeutralButton("清空", (d,w) -> { monthlyRestInput.setText(""); updateMonthlyRestButton(); })\n                .setPositiveButton("确定", (d,w) -> {\n                    StringBuilder out = new StringBuilder();\n                    for (int i=1;i<=31;i++) if (selected[i]) { if (out.length()>0) out.append(", "); out.append(i); }\n                    monthlyRestInput.setText(out.toString());\n                    updateMonthlyRestButton();\n                }).show();\n    }\n\n'''
if anchor not in s: raise SystemExit('settings helper anchor missing')
s=s.replace(anchor,helper+anchor,1)
p.write_text(s)

# ---------- MainActivity ----------
p=Path('app/src/main/java/com/example/workhours/MainActivity.java')
s=p.read_text()
s=s.replace('import android.view.Gravity;','import android.view.Gravity;\nimport android.view.View;')
s=s.replace('    private static final String REST_PREFIX = "rest_";','    private static final String REST_PREFIX = "rest_";\n    private static final String WAGE_DEDUCT_PREFIX = "wage_deduct_";')
s=s.replace('    private LinearLayout exceptionsContainer, weekDetailsContainer, rangeDetailsContainer;','    private LinearLayout exceptionsContainer, weekDetailsContainer, rangeDetailsContainer;\n    private LinearLayout weekSectionContainer, rangeSectionContainer;')

# Main page collapsible statistics.
old='''        buildMonthSection(root);\n        buildWeekSection(root);\n        buildRangeSection(root);\n'''
new='''        buildMonthSection(root);\n\n        TextView statsTitle = text("更多统计", 19, true);\n        statsTitle.setPadding(0, dp(24), 0, dp(8));\n        root.addView(statsTitle);\n        LinearLayout statsButtons = horizontal();\n        root.addView(statsButtons);\n        Button weekToggle = button("周统计");\n        Button rangeToggle = button("范围统计");\n        statsButtons.addView(weekToggle, new LinearLayout.LayoutParams(0, dp(48), 1f));\n        LinearLayout.LayoutParams rtp = new LinearLayout.LayoutParams(0, dp(48), 1f); rtp.leftMargin = dp(8);\n        statsButtons.addView(rangeToggle, rtp);\n\n        weekSectionContainer = vertical();\n        weekSectionContainer.setVisibility(View.GONE);\n        root.addView(weekSectionContainer);\n        buildWeekSection(weekSectionContainer);\n        rangeSectionContainer = vertical();\n        rangeSectionContainer.setVisibility(View.GONE);\n        root.addView(rangeSectionContainer);\n        buildRangeSection(rangeSectionContainer);\n        weekToggle.setOnClickListener(v -> {\n            boolean show = weekSectionContainer.getVisibility() != View.VISIBLE;\n            weekSectionContainer.setVisibility(show ? View.VISIBLE : View.GONE);\n            if (show) rangeSectionContainer.setVisibility(View.GONE);\n        });\n        rangeToggle.setOnClickListener(v -> {\n            boolean show = rangeSectionContainer.getVisibility() != View.VISIBLE;\n            rangeSectionContainer.setVisibility(show ? View.VISIBLE : View.GONE);\n            if (show) weekSectionContainer.setVisibility(View.GONE);\n        });\n'''
if old not in s: raise SystemExit('main section block missing')
s=s.replace(old,new,1)

# Add wage deduction checkbox in day editor.
old='''        TextView calculated=text("",15,true);calculated.setPadding(0,dp(10),0,0);box.addView(calculated);\n'''
new='''        TextView wageTitle=text("工资",14,true);wageTitle.setPadding(0,dp(16),0,dp(2));box.addView(wageTitle);\n        CheckBox wageDeductCheck=new CheckBox(this);wageDeductCheck.setText("这一天需要扣工资");wageDeductCheck.setChecked(prefs.getBoolean(wageDeductKey(date),false));box.addView(wageDeductCheck);\n        TextView calculated=text("",15,true);calculated.setPadding(0,dp(10),0,0);box.addView(calculated);\n'''
if old not in s: raise SystemExit('day editor calculated anchor missing')
s=s.replace(old,new,1)

# Include wage deduction in confirm text.
s=s.replace('''+"\\n总工时："+formatDurationHours(fBase+fOt);''','''+"\\n总工时："+formatDurationHours(fBase+fOt)+(wageDeductCheck.isChecked()?"\\n工资：本日扣工资":"");''',1)
# Save deduction flag just before apply.
s=s.replace('''else e.remove(overtimeStartKey(date)).remove(overtimeEndKey(date));e.apply();WorkAlarmManager.forceSync(this);''','''else e.remove(overtimeStartKey(date)).remove(overtimeEndKey(date));if(wageDeductCheck.isChecked())e.putBoolean(wageDeductKey(date),true);else e.remove(wageDeductKey(date));e.apply();WorkAlarmManager.forceSync(this);''',1)
# Reset also clears deduction.
s=s.replace('''.remove(leaveNoteKey(date)).remove(restKey(date)).apply();WorkAlarmManager.forceSync(this);''','''.remove(leaveNoteKey(date)).remove(restKey(date)).remove(wageDeductKey(date)).apply();WorkAlarmManager.forceSync(this);''',1)
# helper key.
s=s.replace('''private String leaveKey(LocalDate d){return LEAVE_PREFIX+d;}''','''private String leaveKey(LocalDate d){return LEAVE_PREFIX+d;} private String wageDeductKey(LocalDate d){return WAGE_DEDUCT_PREFIX+d;}''',1)
p.write_text(s)

# ---------- WageActivity ----------
p=Path('app/src/main/java/com/example/workhours/WageActivity.java')
s=p.read_text()
# Keep existing backend methods/fields for compatibility but hide the duplicated day-deduction UI from build.
start=s.find('''        TextView dayTitle = text("查看某一天工资 / 扣工资", 19, true);''')
end=s.find('''        TextView weekSection = text("按星期查看工资", 19, true);''')
if start<0 or end<0: raise SystemExit('wage day UI block missing')
s=s[:start]+'''        TextView dayInfo = text("单日扣工资请在主页点击对应日期设置。", 13, false);\n        dayInfo.setPadding(0, dp(12), 0, dp(4));\n        root.addView(dayInfo);\n\n'''+s[end:]
# refreshAll no longer calls refreshDay, preventing null view access.
s=s.replace('''        refreshDay();\n        refreshWeek();''','''        refreshWeek();''',1)
p.write_text(s)
