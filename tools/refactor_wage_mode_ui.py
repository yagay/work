from pathlib import Path

p = Path('app/src/main/java/com/example/workhours/WageActivity.java')
s = p.read_text()

s = s.replace(
    '    private EditText hourlyRateInput;\n    private EditText monthlySalaryInput;\n',
    '    private EditText hourlyRateInput;\n    private EditText monthlySalaryInput;\n    private LinearLayout hourlySettingsContainer;\n    private LinearLayout monthlySettingsContainer;\n'
)

old = '''        root.addView(text("每小时工资（£）", 14, true));
        hourlyRateInput = decimalInput("例如：12.50");
        root.addView(hourlyRateInput);

        TextView monthlyLabel = text("每月固定工资（£）", 14, true);
        monthlyLabel.setPadding(0, dp(10), 0, 0);
        root.addView(monthlyLabel);
        monthlySalaryInput = decimalInput("例如：2200");
        root.addView(monthlySalaryInput);

        TextView rule = text("月薪模式：月薪按当月计划上班日平均分摊。公共假日和请假默认保留工资；只有明确标记“扣工资”的日期才扣除当天份额。", 13, false);
        rule.setPadding(0, dp(8), 0, dp(8));
        root.addView(rule);
'''
new = '''        hourlySettingsContainer = vertical();
        hourlySettingsContainer.addView(text("每小时工资（£）", 14, true));
        hourlyRateInput = decimalInput("例如：12.50");
        hourlySettingsContainer.addView(hourlyRateInput);
        root.addView(hourlySettingsContainer);

        monthlySettingsContainer = vertical();
        TextView monthlyLabel = text("每月固定工资（£）", 14, true);
        monthlyLabel.setPadding(0, dp(10), 0, 0);
        monthlySettingsContainer.addView(monthlyLabel);
        monthlySalaryInput = decimalInput("例如：2200");
        monthlySettingsContainer.addView(monthlySalaryInput);

        TextView rule = text("月薪模式：月薪按当月计划上班日平均分摊。公共假日和请假默认保留工资；只有明确标记“扣工资”的日期才扣除当天份额。", 13, false);
        rule.setPadding(0, dp(8), 0, dp(8));
        monthlySettingsContainer.addView(rule);
        root.addView(monthlySettingsContainer);

        modes.setOnCheckedChangeListener((group, checkedId) -> updateWageModeVisibility());
'''
if old not in s:
    raise SystemExit('settings block not found')
s = s.replace(old, new)

old_load = '''        hourlyRateInput.setText(trimMoney(prefs.getFloat(HOURLY_RATE_KEY, 0f)));
        monthlySalaryInput.setText(trimMoney(prefs.getFloat(MONTHLY_SALARY_KEY, 0f)));
    }
'''
new_load = '''        hourlyRateInput.setText(trimMoney(prefs.getFloat(HOURLY_RATE_KEY, 0f)));
        monthlySalaryInput.setText(trimMoney(prefs.getFloat(MONTHLY_SALARY_KEY, 0f)));
        updateWageModeVisibility();
    }

    private void updateWageModeVisibility() {
        boolean monthly = monthlyMode != null && monthlyMode.isChecked();
        if (hourlySettingsContainer != null) {
            hourlySettingsContainer.setVisibility(monthly ? View.GONE : View.VISIBLE);
        }
        if (monthlySettingsContainer != null) {
            monthlySettingsContainer.setVisibility(monthly ? View.VISIBLE : View.GONE);
        }
    }
'''
if old_load not in s:
    raise SystemExit('load block not found')
s = s.replace(old_load, new_load)

old_save = '''    private void saveWageSettings() {
        Float hourly = parseMoney(hourlyRateInput, "请输入正确的时薪");
        Float monthly = parseMoney(monthlySalaryInput, "请输入正确的月薪");
        if (hourly == null || monthly == null) return;
        prefs.edit()
                .putString(WAGE_MODE_KEY, monthlyMode.isChecked() ? "monthly" : "hourly")
                .putFloat(HOURLY_RATE_KEY, hourly)
                .putFloat(MONTHLY_SALARY_KEY, monthly)
                .apply();
'''
new_save = '''    private void saveWageSettings() {
        boolean monthlySelected = monthlyMode.isChecked();
        Float hourly = monthlySelected
                ? prefs.getFloat(HOURLY_RATE_KEY, 0f)
                : parseMoney(hourlyRateInput, "请输入正确的时薪");
        Float monthly = monthlySelected
                ? parseMoney(monthlySalaryInput, "请输入正确的月薪")
                : prefs.getFloat(MONTHLY_SALARY_KEY, 0f);
        if (hourly == null || monthly == null) return;
        prefs.edit()
                .putString(WAGE_MODE_KEY, monthlySelected ? "monthly" : "hourly")
                .putFloat(HOURLY_RATE_KEY, hourly)
                .putFloat(MONTHLY_SALARY_KEY, monthly)
                .apply();
'''
if old_save not in s:
    raise SystemExit('save block not found')
s = s.replace(old_save, new_save)

p.write_text(s)
