from pathlib import Path

# MainActivity: add 月 detail tab for work statistics.
p=Path('app/src/main/java/com/example/workhours/MainActivity.java')
s=p.read_text()
s=s.replace('''    private LinearLayout monthSectionContainer, weekSectionContainer, rangeSectionContainer;\n    private LinearLayout workContent, wageContent;\n    private Button workWeekTabButton, workDateTabButton;''','''    private LinearLayout monthSectionContainer, weekSectionContainer, rangeSectionContainer;\n    private LinearLayout workContent, wageContent;\n    private Button workWeekTabButton, workDateTabButton, workMonthTabButton;\n    private TextView workMonthSummaryText;\n    private LinearLayout workMonthDetailsContainer;''',1)

old='''        workWeekTabButton = button("星期");\n        workDateTabButton = button("日期");\n        workViewTabs.addView(workWeekTabButton, new LinearLayout.LayoutParams(0, dp(46), 1f));\n        LinearLayout.LayoutParams workDateTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);\n        workDateTabParams.leftMargin = dp(6);\n        workViewTabs.addView(workDateTabButton, workDateTabParams);\n\n        weekSectionContainer = vertical();\n        rangeSectionContainer = vertical();\n        workContent.addView(weekSectionContainer);\n        workContent.addView(rangeSectionContainer);\n        buildWeekSection(weekSectionContainer);\n        buildRangeSection(rangeSectionContainer);\n\n        workWeekTabButton.setOnClickListener(v -> showWorkViewTab(0));\n        workDateTabButton.setOnClickListener(v -> showWorkViewTab(1));\n        showWorkViewTab(0);'''
new='''        workWeekTabButton = button("星期");\n        workDateTabButton = button("日期");\n        workMonthTabButton = button("月");\n        workViewTabs.addView(workWeekTabButton, new LinearLayout.LayoutParams(0, dp(46), 1f));\n        LinearLayout.LayoutParams workDateTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);\n        workDateTabParams.leftMargin = dp(6);\n        workViewTabs.addView(workDateTabButton, workDateTabParams);\n        LinearLayout.LayoutParams workMonthTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);\n        workMonthTabParams.leftMargin = dp(6);\n        workViewTabs.addView(workMonthTabButton, workMonthTabParams);\n\n        weekSectionContainer = vertical();\n        rangeSectionContainer = vertical();\n        monthSectionContainer = vertical();\n        workContent.addView(weekSectionContainer);\n        workContent.addView(rangeSectionContainer);\n        workContent.addView(monthSectionContainer);\n        buildWeekSection(weekSectionContainer);\n        buildRangeSection(rangeSectionContainer);\n        buildWorkMonthDetailSection(monthSectionContainer);\n\n        workWeekTabButton.setOnClickListener(v -> showWorkViewTab(0));\n        workDateTabButton.setOnClickListener(v -> showWorkViewTab(1));\n        workMonthTabButton.setOnClickListener(v -> showWorkViewTab(2));\n        showWorkViewTab(0);'''
if old not in s: raise SystemExit('main tabs anchor missing')
s=s.replace(old,new,1)

start=s.index('    private void showWorkViewTab(int tab) {')
end=s.index('    private void showStatsTab(boolean wage)',start)
method='''    private void showWorkViewTab(int tab) {\n        if (weekSectionContainer == null || rangeSectionContainer == null || monthSectionContainer == null) return;\n        weekSectionContainer.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);\n        rangeSectionContainer.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);\n        monthSectionContainer.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);\n        UiStyle.button(this, workWeekTabButton, tab == 0);\n        UiStyle.button(this, workDateTabButton, tab == 1);\n        UiStyle.button(this, workMonthTabButton, tab == 2);\n        workWeekTabButton.setEnabled(tab != 0);\n        workDateTabButton.setEnabled(tab != 1);\n        workMonthTabButton.setEnabled(tab != 2);\n        if (tab == 0) refreshWeek();\n        else if (tab == 1) calculateRange();\n        else refreshWorkMonthDetails();\n    }\n\n'''
s=s[:start]+method+s[end:]

anchor='''    private void buildWeekSection(LinearLayout root) {\n'''
month_methods='''    private void buildWorkMonthDetailSection(LinearLayout root) {\n        TextView t=text("按月查看工时详情",19,true); t.setPadding(0,dp(24),0,dp(8)); root.addView(t);\n        TextView hint=text("使用上方月份选择器切换月份。",13,false); hint.setPadding(0,0,0,dp(8)); root.addView(hint);\n        LinearLayout c=card(); root.addView(c);\n        workMonthSummaryText=text("",16,true); c.addView(workMonthSummaryText);\n        workMonthDetailsContainer=vertical(); c.addView(workMonthDetailsContainer);\n    }\n\n    private void refreshWorkMonthDetails() {\n        if(workMonthSummaryText==null||workMonthDetailsContainer==null)return;\n        LocalDate today=LocalDate.now(),ws=getWorkStartDate();\n        LocalDate start=displayedMonth.atDay(1); if(ws!=null&&start.isBefore(ws))start=ws;\n        LocalDate end=displayedMonth.equals(YearMonth.now())?today:displayedMonth.atEndOfMonth();\n        workMonthDetailsContainer.removeAllViews();\n        if(start.isAfter(end)){workMonthSummaryText.setText("本月尚未开始工作");return;}\n        Stats st=collectStats(start,end);\n        workMonthSummaryText.setText(displayedMonth.getYear()+"年"+displayedMonth.getMonthValue()+"月\\n总工时："+formatDurationHours(st.totalHours)+"（加班 "+formatDurationHours(st.overtimeHours)+"）\\n"+StatusStatsFormatter.format(st.workDays,st.leaveDays,st.holidayDays,st.restDays));\n        addPeriodDetails(workMonthDetailsContainer,start,end,true,true);\n    }\n\n'''
if anchor not in s: raise SystemExit('main month method anchor missing')
s=s.replace(anchor,month_methods+anchor,1)

# Ensure month refresh also updates work month details if visible/created.
needle='''        rebuildSharedCalendar(today);\n    }'''
rep='''        rebuildSharedCalendar(today);\n        refreshWorkMonthDetails();\n    }'''
if needle in s: s=s.replace(needle,rep,1)
p.write_text(s)

# WagePanel: restore 月 tab as a detail-only panel synchronized to shared selected month.
p=Path('app/src/main/java/com/example/workhours/WagePanel.java')
s=p.read_text()
old='''        weekTabButton = button("星期");\n        dayTabButton = button("日期");\n        viewTabs.addView(weekTabButton, new LinearLayout.LayoutParams(0, dp(46), 1f));\n        LinearLayout.LayoutParams middleTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);\n        middleTabParams.leftMargin = dp(6);\n        viewTabs.addView(dayTabButton, middleTabParams);'''
new='''        weekTabButton = button("星期");\n        dayTabButton = button("日期");\n        monthTabButton = button("月");\n        viewTabs.addView(weekTabButton, new LinearLayout.LayoutParams(0, dp(46), 1f));\n        LinearLayout.LayoutParams middleTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);\n        middleTabParams.leftMargin = dp(6);\n        viewTabs.addView(dayTabButton, middleTabParams);\n        LinearLayout.LayoutParams lastTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);\n        lastTabParams.leftMargin = dp(6);\n        viewTabs.addView(monthTabButton, lastTabParams);'''
if old not in s: raise SystemExit('wage tabs anchor missing')
s=s.replace(old,new,1)

s=s.replace('''        root.addView(weekContent);\n        root.addView(dayContent);''','''        root.addView(weekContent);\n        root.addView(dayContent);\n        root.addView(monthContent);''',1)

anchor='''        weekTabButton.setOnClickListener(v -> showViewTab(0));\n        dayTabButton.setOnClickListener(v -> showViewTab(1));\n        showViewTab(0);'''
month_ui='''        TextView monthSection = text("按月查看工资详情", 19, true);\n        monthSection.setPadding(0, dp(18), 0, dp(8));\n        monthContent.addView(monthSection);\n        TextView monthHint = text("使用上方月份选择器切换月份。", 13, false);\n        monthHint.setPadding(0, 0, 0, dp(8));\n        monthContent.addView(monthHint);\n        LinearLayout monthCard = card();\n        monthContent.addView(monthCard);\n        monthSummary = text("", 16, true);\n        monthCard.addView(monthSummary);\n        monthDetails = vertical();\n        monthCard.addView(monthDetails);\n\n        weekTabButton.setOnClickListener(v -> showViewTab(0));\n        dayTabButton.setOnClickListener(v -> showViewTab(1));\n        monthTabButton.setOnClickListener(v -> showViewTab(2));\n        showViewTab(0);'''
if anchor not in s: raise SystemExit('wage month ui anchor missing')
s=s.replace(anchor,month_ui,1)

# Replace showViewTab implementation.
start=s.index('    private void showViewTab(int tab) {')
end=s.index('    private void refreshDay()',start)
new_method='''    private void showViewTab(int tab) {\n        if (weekContent == null || dayContent == null || monthContent == null) return;\n        weekContent.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);\n        dayContent.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);\n        monthContent.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);\n        UiStyle.button(host, weekTabButton, tab == 0);\n        UiStyle.button(host, dayTabButton, tab == 1);\n        UiStyle.button(host, monthTabButton, tab == 2);\n        weekTabButton.setEnabled(tab != 0);\n        dayTabButton.setEnabled(tab != 1);\n        monthTabButton.setEnabled(tab != 2);\n        if (tab == 0) refreshWeek();\n        else if (tab == 1) refreshDay();\n        else refreshMonth();\n    }\n\n'''
s=s[:start]+new_method+s[end:]

# Make refreshMonth null-safe and functional with recreated views.
# Existing method already populates monthSummary/monthDetails. Keep it, but guard fields.
needle='''    private void refreshMonth() {\n        YearMonth now = YearMonth.now();'''
rep='''    private void refreshMonth() {\n        if (monthSummary == null || monthDetails == null) return;\n        YearMonth now = YearMonth.now();'''
if needle not in s: raise SystemExit('refreshMonth anchor missing')
s=s.replace(needle,rep,1)
p.write_text(s)
