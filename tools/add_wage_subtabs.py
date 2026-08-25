from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/WagePanel.java')
s=p.read_text()

s=s.replace('    private CheckBox deductCheck;\n', '''    private CheckBox deductCheck;\n    private LinearLayout weekContent;\n    private LinearLayout dayContent;\n    private LinearLayout monthContent;\n    private Button weekTabButton;\n    private Button dayTabButton;\n    private Button monthTabButton;\n''')

old='''        TextView dayInfo = text("单日扣工资请在主页点击对应日期设置。", 13, false);\n        dayInfo.setPadding(0, dp(12), 0, dp(4));\n        root.addView(dayInfo);\n\n        TextView weekSection = text("按星期查看工资", 19, true);\n        weekSection.setPadding(0, dp(24), 0, dp(8));\n        root.addView(weekSection);\n        LinearLayout weekNav = horizontal();\n        weekNav.setGravity(Gravity.CENTER_VERTICAL);\n        root.addView(weekNav);\n'''
new='''        TextView dayInfo = text("单日扣工资请在主页点击对应日期设置。", 13, false);\n        dayInfo.setPadding(0, dp(12), 0, dp(4));\n        root.addView(dayInfo);\n\n        LinearLayout viewTabs = horizontal();\n        LinearLayout.LayoutParams viewTabsParams = new LinearLayout.LayoutParams(-1, dp(46));\n        viewTabsParams.topMargin = dp(18);\n        root.addView(viewTabs, viewTabsParams);\n        weekTabButton = button("星期");\n        dayTabButton = button("日期");\n        monthTabButton = button("月份");\n        viewTabs.addView(weekTabButton, new LinearLayout.LayoutParams(0, dp(46), 1f));\n        LinearLayout.LayoutParams middleTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);\n        middleTabParams.leftMargin = dp(6);\n        viewTabs.addView(dayTabButton, middleTabParams);\n        LinearLayout.LayoutParams lastTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);\n        lastTabParams.leftMargin = dp(6);\n        viewTabs.addView(monthTabButton, lastTabParams);\n\n        weekContent = vertical();\n        dayContent = vertical();\n        monthContent = vertical();\n        root.addView(weekContent);\n        root.addView(dayContent);\n        root.addView(monthContent);\n\n        TextView weekSection = text("按星期查看工资", 19, true);\n        weekSection.setPadding(0, dp(18), 0, dp(8));\n        weekContent.addView(weekSection);\n        LinearLayout weekNav = horizontal();\n        weekNav.setGravity(Gravity.CENTER_VERTICAL);\n        weekContent.addView(weekNav);\n'''
if old not in s: raise SystemExit('anchor1 not found')
s=s.replace(old,new,1)
s=s.replace('        root.addView(weekCard);\n', '        weekContent.addView(weekCard);\n',1)

old2='''        TextView monthSection = text("按月查看工资", 19, true);\n        monthSection.setPadding(0, dp(24), 0, dp(8));\n        root.addView(monthSection);\n        LinearLayout monthNav = horizontal();\n        monthNav.setGravity(Gravity.CENTER_VERTICAL);\n        root.addView(monthNav);\n'''
new2='''        TextView daySection = text("按日期查看工资", 19, true);\n        daySection.setPadding(0, dp(18), 0, dp(8));\n        dayContent.addView(daySection);\n        selectedDateButton = button("");\n        selectedDateButton.setOnClickListener(v -> chooseDate());\n        dayContent.addView(selectedDateButton, new LinearLayout.LayoutParams(-1, dp(50)));\n        LinearLayout dayCard = card();\n        LinearLayout.LayoutParams dayCardParams = new LinearLayout.LayoutParams(-1, -2);\n        dayCardParams.topMargin = dp(10);\n        dayContent.addView(dayCard, dayCardParams);\n        daySummary = text("", 16, true);\n        dayCard.addView(daySummary);\n        TextView dayHint = text("扣工资请回到工时统计后点击对应日期修改。", 13, false);\n        dayHint.setPadding(0, dp(10), 0, 0);\n        dayCard.addView(dayHint);\n\n        TextView monthSection = text("按月查看工资", 19, true);\n        monthSection.setPadding(0, dp(18), 0, dp(8));\n        monthContent.addView(monthSection);\n        LinearLayout monthNav = horizontal();\n        monthNav.setGravity(Gravity.CENTER_VERTICAL);\n        monthContent.addView(monthNav);\n'''
if old2 not in s: raise SystemExit('anchor2 not found')
s=s.replace(old2,new2,1)
s=s.replace('        root.addView(monthCard);\n', '        monthContent.addView(monthCard);\n',1)

anchor='''        monthDetails = vertical();\n        monthCard.addView(monthDetails);\n\n    }\n'''
rep='''        monthDetails = vertical();\n        monthCard.addView(monthDetails);\n\n        weekTabButton.setOnClickListener(v -> showViewTab(0));\n        dayTabButton.setOnClickListener(v -> showViewTab(1));\n        monthTabButton.setOnClickListener(v -> showViewTab(2));\n        showViewTab(0);\n\n    }\n'''
if anchor not in s: raise SystemExit('anchor3 not found')
s=s.replace(anchor,rep,1)

s=s.replace('''        refreshWeek();\n        refreshMonth();\n    }\n\n    private void refreshDay() {''','''        refreshWeek();\n        refreshDay();\n        refreshMonth();\n    }\n\n    private void showViewTab(int tab) {\n        if (weekContent == null || dayContent == null || monthContent == null) return;\n        weekContent.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);\n        dayContent.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);\n        monthContent.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);\n        UiStyle.button(host, weekTabButton, tab == 0);\n        UiStyle.button(host, dayTabButton, tab == 1);\n        UiStyle.button(host, monthTabButton, tab == 2);\n        weekTabButton.setEnabled(tab != 0);\n        dayTabButton.setEnabled(tab != 1);\n        monthTabButton.setEnabled(tab != 2);\n        if (tab == 0) refreshWeek();\n        else if (tab == 1) refreshDay();\n        else refreshMonth();\n    }\n\n    private void refreshDay() {''',1)

# refreshDay should tolerate panel creation; fields now always exist, but keep guard robust.
s=s.replace('''    private void refreshDay() {\n        selectedDateButton.setText''','''    private void refreshDay() {\n        if (selectedDateButton == null || daySummary == null) return;\n        selectedDateButton.setText''',1)
# deductCheck no longer shown in this panel; avoid dereference.
s=s.replace('        deductCheck.setChecked(isDeducted(selectedDate));\n','')

p.write_text(s)
