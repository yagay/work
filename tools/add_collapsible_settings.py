from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

# Helper to redirect root.addView calls inside a source range to a section container.
def redirect(start_marker, end_marker, section):
    global s
    a=s.find(start_marker)
    b=s.find(end_marker,a)
    if a<0 or b<0: raise SystemExit(f'missing range {start_marker[:30]} -> {end_marker[:30]}')
    chunk=s[a:b]
    chunk=chunk.replace('root.addView(', section+'.addView(')
    s=s[:a]+chunk+s[b:]

# Insert first/default section before work-start settings.
anchor='''        root.addView(text("工作开始日期（可选）", 17, true));'''
insert='''        LinearLayout basicSection = createCollapsibleSection(root, "基本工作设置", true);\n'''
if anchor not in s: raise SystemExit('basic anchor missing')
s=s.replace(anchor,insert+anchor,1)

# Insert section containers at boundaries before redirecting ranges.
anchor='''        TextView alarmTitle = text("上班闹钟", 17, true);'''
if anchor not in s: raise SystemExit('alarm anchor missing')
s=s.replace(anchor,'''        LinearLayout alarmSection = createCollapsibleSection(root, "上班闹钟", false);\n'''+anchor,1)

anchor='''        TextView holidayTitle = text("公共假日", 17, true);'''
if anchor not in s: raise SystemExit('holiday anchor missing')
s=s.replace(anchor,'''        LinearLayout holidaySection = createCollapsibleSection(root, "公共假日", false);\n'''+anchor,1)

anchor='''        TextView restRuleTitle = text("休息规则", 17, true);'''
if anchor not in s: raise SystemExit('rest anchor missing')
s=s.replace(anchor,'''        LinearLayout restSection = createCollapsibleSection(root, "休息规则", true);\n'''+anchor,1)

anchor='''        TextView wageTitle = text("工资设置", 17, true);'''
if anchor not in s: raise SystemExit('wage anchor missing')
s=s.replace(anchor,'''        LinearLayout wageSection = createCollapsibleSection(root, "工资设置", false);\n'''+anchor,1)

anchor='''        TextView backupTitle = text("数据备份与迁移", 17, true);'''
if anchor not in s: raise SystemExit('backup anchor missing')
s=s.replace(anchor,'''        LinearLayout backupSection = createCollapsibleSection(root, "数据备份与迁移", false);\n'''+anchor,1)

# Redirect each logical range. Keep Save button outside all sections.
redirect('''        root.addView(text("工作开始日期（可选）", 17, true));''','''        LinearLayout alarmSection = createCollapsibleSection(root, "上班闹钟", false);''','basicSection')
redirect('''        TextView alarmTitle = text("上班闹钟", 17, true);''','''        LinearLayout holidaySection = createCollapsibleSection(root, "公共假日", false);''','alarmSection')
redirect('''        TextView holidayTitle = text("公共假日", 17, true);''','''        LinearLayout restSection = createCollapsibleSection(root, "休息规则", true);''','holidaySection')
redirect('''        TextView restRuleTitle = text("休息规则", 17, true);''','''        LinearLayout wageSection = createCollapsibleSection(root, "工资设置", false);''','restSection')
redirect('''        TextView wageTitle = text("工资设置", 17, true);''','''        Button save = new Button(this);''','wageSection')
redirect('''        TextView backupTitle = text("数据备份与迁移", 17, true);''','''        return scroll;''','backupSection')

# Avoid duplicate section name headings inside each fold: make them smaller descriptive labels or remove title-like duplication.
s=s.replace('''        TextView alarmTitle = text("上班闹钟", 17, true);\n        alarmTitle.setPadding(0, dp(22), 0, dp(4));\n        alarmSection.addView(alarmTitle);\n''','',1)
s=s.replace('''        TextView holidayTitle = text("公共假日", 17, true);\n        holidayTitle.setPadding(0, dp(24), 0, dp(6));\n        holidaySection.addView(holidayTitle);\n''','',1)
s=s.replace('''        TextView restRuleTitle = text("休息规则", 17, true);\n        restRuleTitle.setPadding(0, dp(24), 0, dp(8));\n        restSection.addView(restRuleTitle);\n''','',1)
s=s.replace('''        TextView wageTitle = text("工资设置", 17, true);\n        wageTitle.setPadding(0, dp(26), 0, dp(6));\n        wageSection.addView(wageTitle);\n''','',1)
s=s.replace('''        TextView backupTitle = text("数据备份与迁移", 17, true);\n        backupTitle.setPadding(0, dp(28), 0, dp(4));\n        backupSection.addView(backupTitle);\n''','',1)

# Add collapsible helper before settingInputRow.
anchor='''    private LinearLayout settingInputRow(String label, int inputWidth) {'''
helper='''    private LinearLayout createCollapsibleSection(LinearLayout root, String title, boolean expanded) {\n        LinearLayout wrapper = new LinearLayout(this);\n        wrapper.setOrientation(LinearLayout.VERTICAL);\n        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(-1, -2);\n        wrapperParams.topMargin = dp(10);\n        root.addView(wrapper, wrapperParams);\n\n        Button header = new Button(this);\n        header.setAllCaps(false);\n        header.setTextSize(16);\n        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);\n        header.setPadding(dp(14), 0, dp(14), 0);\n        UiStyle.button(this, header, false);\n        wrapper.addView(header, new LinearLayout.LayoutParams(-1, dp(50)));\n\n        LinearLayout content = new LinearLayout(this);\n        content.setOrientation(LinearLayout.VERTICAL);\n        content.setPadding(dp(4), dp(8), dp(4), dp(6));\n        wrapper.addView(content, new LinearLayout.LayoutParams(-1, -2));\n\n        final boolean[] open = {expanded};\n        Runnable paint = () -> {\n            content.setVisibility(open[0] ? android.view.View.VISIBLE : android.view.View.GONE);\n            header.setText((open[0] ? "▼  " : "▶  ") + title);\n        };\n        header.setOnClickListener(v -> { open[0] = !open[0]; paint.run(); });\n        paint.run();\n        return content;\n    }\n\n'''
if anchor not in s: raise SystemExit('helper anchor missing')
s=s.replace(anchor,helper+anchor,1)

p.write_text(s)
