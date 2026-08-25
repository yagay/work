from pathlib import Path

p = Path('app/src/main/java/com/example/workhours/MainActivity.java')
s = p.read_text()

s = s.replace(
    'private TextView monthTitle, totalHoursText, workDaysText, leaveDaysText, holidayDaysText, restDaysText;',
    'private TextView monthTitle, totalHoursText, statusStatsText;'
)

old_layout = '''        LinearLayout statsRows = vertical();
        card.addView(statsRows);
        LinearLayout statsRow1 = horizontal();
        LinearLayout statsRow2 = horizontal();
        statsRows.addView(statsRow1);
        LinearLayout.LayoutParams row2Params = new LinearLayout.LayoutParams(-1, -2);
        row2Params.topMargin = dp(8);
        statsRows.addView(statsRow2, row2Params);

        workDaysText = statLineText();
        leaveDaysText = statLineText();
        holidayDaysText = statLineText();
        restDaysText = statLineText();
        statsRow1.addView(workDaysText, new LinearLayout.LayoutParams(0, -2, 1f));
        statsRow1.addView(leaveDaysText, new LinearLayout.LayoutParams(0, -2, 1f));
        statsRow2.addView(holidayDaysText, new LinearLayout.LayoutParams(0, -2, 1f));
        statsRow2.addView(restDaysText, new LinearLayout.LayoutParams(0, -2, 1f));
'''
new_layout = '''        statusStatsText = statLineText();
        statusStatsText.setTextSize(13);
        statusStatsText.setGravity(Gravity.CENTER);
        card.addView(statusStatsText);
'''
s = s.replace(old_layout, new_layout)

old_month = '''        workDaysText.setText("工作  " + s.workDays + "天");
        leaveDaysText.setText("请假  " + s.leaveDays + "天");
        holidayDaysText.setText("公共假日  " + s.holidayDays + "天");
        restDaysText.setText("休息  " + s.restDays + "天");'''
new_month = '''        statusStatsText.setText(StatusStatsFormatter.format(
                s.workDays, s.leaveDays, s.holidayDays, s.restDays));'''
s = s.replace(old_month, new_month)

old_week = '''        weekSummaryText.setText("总工时：" + formatDurationHours(s.totalHours) + "（其中加班 " + formatDurationHours(s.overtimeHours) + "）\\n工作：" + s.workDays + "天 · 请假：" + s.leaveDays + "天 · 公共假日：" + s.holidayDays + "天 · 休息：" + s.restDays + "天");'''
new_week = '''        weekSummaryText.setText("总工时：" + formatDurationHours(s.totalHours)
                + "（其中加班 " + formatDurationHours(s.overtimeHours) + "）\\n"
                + StatusStatsFormatter.format(s.workDays, s.leaveDays, s.holidayDays, s.restDays));'''
s = s.replace(old_week, new_week)

old_range = '''private void calculateRange(){if(rangeDetailsContainer==null)return;rangeDetailsContainer.removeAllViews();LocalDate today=LocalDate.now(),ws=getWorkStartDate(),start=rangeStart,end=rangeEnd.isAfter(today)?today:rangeEnd;if(ws!=null&&start.isBefore(ws))start=ws;if(start.isAfter(end)){rangeSummaryText.setText("开始日期不能晚于结束日期");return;}Stats s=collectStats(start,end);rangeSummaryText.setText(start+" 至 "+end+"\\n总工时："+formatDurationHours(s.totalHours)+"（加班 "+formatDurationHours(s.overtimeHours)+"）\\n工作："+s.workDays+"天 · 请假："+s.leaveDays+"天 · 公共假日："+s.holidayDays+"天 · 休息："+s.restDays+"天");addPeriodDetails(rangeDetailsContainer,start,end,false,true);}'''
new_range = '''private void calculateRange(){if(rangeDetailsContainer==null)return;rangeDetailsContainer.removeAllViews();LocalDate today=LocalDate.now(),ws=getWorkStartDate(),start=rangeStart,end=rangeEnd.isAfter(today)?today:rangeEnd;if(ws!=null&&start.isBefore(ws))start=ws;if(start.isAfter(end)){rangeSummaryText.setText("开始日期不能晚于结束日期");return;}Stats s=collectStats(start,end);rangeSummaryText.setText(start+" 至 "+end+"\\n总工时："+formatDurationHours(s.totalHours)+"（加班 "+formatDurationHours(s.overtimeHours)+"）\\n"+StatusStatsFormatter.format(s.workDays,s.leaveDays,s.holidayDays,s.restDays));addPeriodDetails(rangeDetailsContainer,start,end,false,true);}'''
s = s.replace(old_range, new_range)

p.write_text(s)
