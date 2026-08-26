from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()

# Add centralized UI metrics after class declaration.
anchor='public class SettingsActivity extends Activity {\n\n'
metrics='''public class SettingsActivity extends Activity {\n\n    // Unified settings-page typography and sizing.\n    private static final int UI_PAGE_TITLE_SP = 22;\n    private static final int UI_SECTION_TITLE_SP = 16;\n    private static final int UI_LABEL_SP = 15;\n    private static final int UI_VALUE_SP = 14;\n    private static final int UI_BODY_SP = 13;\n    private static final int UI_ROW_DP = 48;\n    private static final int UI_ACTION_DP = 50;\n\n'''
if anchor not in s: raise SystemExit('class anchor missing')
s=s.replace(anchor,metrics,1)

# Page title and introductory text.
s=s.replace('text("工作时间设置", 24, true)', 'text("工作时间设置", UI_PAGE_TITLE_SP, true)')
s=s.replace('text("设置一次后，App 会按工作时间和休息规则自动计算。", 14, false)', 'text("设置一次后，App 会按工作时间和休息规则自动计算。", UI_BODY_SP, false)')

# Section/subsection headings used on settings page.
for old in [
    'text("工作开始日期（可选）", 17, true)',
    'text("每天工作时间", 17, true)',
    'text("响铃与稍后提醒", 17, true)',
    'text("公共假日", 17, true)',
    'text("休息规则", 17, true)',
]:
    s=s.replace(old, old.replace('17, true', 'UI_SECTION_TITLE_SP, true'))

# Description/help text: retain hierarchy but use a single body size.
import re
s=re.sub(r'text\(("(?:[^"\\]|\\.)*"), 13, false\)', r'text(\1, UI_BODY_SP, false)', s)

# Checkboxes become one consistent setting-label size.
s=s.replace('setTextSize(16);', 'setTextSize(UI_LABEL_SP);')

# Input text size is currently visually oversized compared with labels.
s=s.replace('view.setTextSize(18);', 'view.setTextSize(UI_LABEL_SP);')

# Main collapsible section header remains a stronger 16sp level.
s=s.replace('header.setTextSize(16);', 'header.setTextSize(UI_SECTION_TITLE_SP);')

# Standard setting labels and right-side values.
s=s.replace('TextView title = text(label, 15, true);', 'TextView title = text(label, UI_LABEL_SP, true);')
s=s.replace('b.setTextSize(14);', 'b.setTextSize(UI_VALUE_SP);')

# Normalize the common row/control heights used by the settings page.
# Keep compact calendar cells and icon navigation sizes untouched.
s=s.replace('new LinearLayout.LayoutParams(0, dp(50), 1f)', 'new LinearLayout.LayoutParams(0, dp(UI_ACTION_DP), 1f)')
s=s.replace('new LinearLayout.LayoutParams(dp(86), dp(50))', 'new LinearLayout.LayoutParams(dp(86), dp(UI_ACTION_DP))')
s=s.replace('new LinearLayout.LayoutParams(-1, dp(50))', 'new LinearLayout.LayoutParams(-1, dp(UI_ACTION_DP))')
s=s.replace('new LinearLayout.LayoutParams(-1, dp(52))', 'new LinearLayout.LayoutParams(-1, dp(UI_ACTION_DP))')
s=s.replace('LinearLayout.LayoutParams.MATCH_PARENT, dp(52)', 'LinearLayout.LayoutParams.MATCH_PARENT, dp(UI_ACTION_DP)')
s=s.replace('LinearLayout.LayoutParams.MATCH_PARENT, dp(54)', 'LinearLayout.LayoutParams.MATCH_PARENT, dp(UI_ACTION_DP)')
s=s.replace('wrapper.addView(header, new LinearLayout.LayoutParams(-1, dp(50)));', 'wrapper.addView(header, new LinearLayout.LayoutParams(-1, dp(UI_ACTION_DP)));')

# Standard compact input/right-value height.
s=s.replace('new LinearLayout.LayoutParams(width, dp(48))', 'new LinearLayout.LayoutParams(width, dp(UI_ROW_DP))')

# Make setting rows use a predictable minimum touch target and spacing.
old='''        row.setGravity(Gravity.CENTER_VERTICAL);\n        row.setPadding(0, dp(6), 0, dp(6));'''
new='''        row.setGravity(Gravity.CENTER_VERTICAL);\n        row.setMinimumHeight(dp(UI_ROW_DP));\n        row.setPadding(0, dp(4), 0, dp(4));'''
if old not in s: raise SystemExit('setting row anchor missing')
s=s.replace(old,new,1)

# Normalize option button minimum height while preserving calendar/day buttons.
old='''        b.setTextSize(UI_VALUE_SP);\n        b.setPadding(dp(10),0,dp(12),0);'''
new='''        b.setTextSize(UI_VALUE_SP);\n        b.setMinHeight(dp(UI_ROW_DP));\n        b.setPadding(dp(10),0,dp(12),0);'''
if old not in s: raise SystemExit('option button anchor missing')
s=s.replace(old,new,1)

# Slightly tighten global page/section spacing for a more coherent density.
s=s.replace('root.setPadding(dp(20), dp(24), dp(20), dp(28));', 'root.setPadding(dp(18), dp(20), dp(18), dp(24));')
s=s.replace('wrapperParams.topMargin = dp(10);', 'wrapperParams.topMargin = dp(8);')
s=s.replace('content.setPadding(dp(4), dp(8), dp(4), dp(6));', 'content.setPadding(dp(4), dp(8), dp(4), dp(8));')

p.write_text(s)
