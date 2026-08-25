from pathlib import Path

p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

old='''        root.addView(text("上班时间（HH:mm）", 15, true));
        startInput = input("09:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        root.addView(startInput);
        TextView endLabel = text("下班时间（HH:mm）", 15, true);
        endLabel.setPadding(0, dp(14), 0, 0);
        root.addView(endLabel);
        endInput = input("17:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        root.addView(endInput);
        TextView breakLabel = text("休息时间（分钟）", 15, true);
        breakLabel.setPadding(0, dp(14), 0, 0);
        root.addView(breakLabel);
        breakInput = input("30", InputType.TYPE_CLASS_NUMBER);
        root.addView(breakInput);
'''
new='''        LinearLayout startRow = settingInputRow("上班时间", dp(126));
        startInput = input("09:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        startRow.addView(startInput, compactInputParams(dp(126)));
        root.addView(startRow);

        LinearLayout endRow = settingInputRow("下班时间", dp(126));
        endInput = input("17:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        endRow.addView(endInput, compactInputParams(dp(126)));
        root.addView(endRow);

        LinearLayout breakRow = settingInputRow("休息时间", dp(126));
        breakInput = input("30", InputType.TYPE_CLASS_NUMBER);
        breakRow.addView(breakInput, compactInputParams(dp(126)));
        root.addView(breakRow);
'''
if old not in s: raise SystemExit('work time block not found')
s=s.replace(old,new,1)

old='''        alarmTimeGroup = new LinearLayout(this);
        alarmTimeGroup.setOrientation(LinearLayout.VERTICAL);
        TextView alarmTimeLabel = text("自定义闹钟时间（HH:mm）", 15, true);
        alarmTimeLabel.setPadding(0, dp(4), 0, 0);
        alarmTimeGroup.addView(alarmTimeLabel);
        alarmTimeInput = input("例如：07:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        alarmTimeGroup.addView(alarmTimeInput);
        root.addView(alarmTimeGroup);
'''
new='''        alarmTimeGroup = settingInputRow("自定义闹钟时间", dp(126));
        alarmTimeInput = input("07:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        alarmTimeGroup.addView(alarmTimeInput, compactInputParams(dp(126)));
        root.addView(alarmTimeGroup);
'''
if old not in s: raise SystemExit('custom alarm block not found')
s=s.replace(old,new,1)

old='''        TextView updateTimeLabel = text("每周日自动更新闹钟时间（HH:mm）", 15, true);
        updateTimeLabel.setPadding(0, dp(12), 0, 0);
        root.addView(updateTimeLabel);
        alarmUpdateTimeInput = input("例如：12:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        root.addView(alarmUpdateTimeInput);
'''
new='''        LinearLayout updateTimeRow = settingInputRow("自动更新闹钟时间", dp(126));
        alarmUpdateTimeInput = input("12:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        updateTimeRow.addView(alarmUpdateTimeInput, compactInputParams(dp(126)));
        root.addView(updateTimeRow);
'''
if old not in s: raise SystemExit('update alarm block not found')
s=s.replace(old,new,1)

anchor='''    private EditText input(String hint, int type) {
'''
helper='''    private LinearLayout settingInputRow(String label, int inputWidth) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView title = text(label, 15, true);
        row.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private LinearLayout.LayoutParams compactInputParams(int width) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, dp(48));
        params.leftMargin = dp(12);
        return params;
    }

'''
if anchor not in s: raise SystemExit('input helper anchor not found')
s=s.replace(anchor,helper+anchor,1)

p.write_text(s)
