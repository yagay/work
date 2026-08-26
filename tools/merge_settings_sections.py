from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

old='''        LinearLayout alarmBehaviorSection = createCollapsibleSection(root, "闹钟响铃设置", false);\n        TextView behaviorInfo = text("单独设置铃声、震动、渐强、稍后提醒和按键行为。电源键/Home 键由 Android 系统保留，App 无法可靠拦截。", 13, false);'''
new='''        LinearLayout alarmBehaviorSection = alarmOptionsGroup;\n        TextView alarmBehaviorTitle = text("响铃与稍后提醒", 17, true);\n        alarmBehaviorTitle.setPadding(0, dp(18), 0, dp(6));\n        alarmBehaviorSection.addView(alarmBehaviorTitle);\n        TextView behaviorInfo = text("单独设置铃声、震动、渐强、稍后提醒和按键行为。电源键/Home 键由 Android 系统保留，App 无法可靠拦截。", 13, false);'''
if old not in s: raise SystemExit('alarm section anchor missing')
s=s.replace(old,new,1)

old='''        LinearLayout holidaySection = createCollapsibleSection(root, "公共假日", false);\n        TextView holidayInfo = text("选择国家或地区后，公共假日会自动从工时、工资和上班闹钟中排除。", 13, false);'''
new='''        LinearLayout restHolidaySection = createCollapsibleSection(root, "休息与公共假日", true);\n        LinearLayout holidaySection = restHolidaySection;\n        TextView holidayTitle = text("公共假日", 17, true);\n        holidayTitle.setPadding(0, 0, 0, dp(6));\n        holidaySection.addView(holidayTitle);\n        TextView holidayInfo = text("选择国家或地区后，公共假日会自动从工时、工资和上班闹钟中排除。", 13, false);'''
if old not in s: raise SystemExit('holiday section anchor missing')
s=s.replace(old,new,1)

old='''        LinearLayout restSection = createCollapsibleSection(root, "休息规则", true);\n        TextView restRuleInfo = text("每周休息日和每月固定休息日二选一，只会使用当前选中的规则。", 13, false);'''
new='''        LinearLayout restSection = restHolidaySection;\n        TextView restTitle = text("休息规则", 17, true);\n        restTitle.setPadding(0, dp(20), 0, dp(6));\n        restSection.addView(restTitle);\n        TextView restRuleInfo = text("每周休息日和每月固定休息日二选一，只会使用当前选中的规则。", 13, false);'''
if old not in s: raise SystemExit('rest section anchor missing')
s=s.replace(old,new,1)

p.write_text(s)
