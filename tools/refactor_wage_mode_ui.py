from pathlib import Path

p = Path('app/src/main/java/com/example/workhours/WageActivity.java')
s = p.read_text()

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
