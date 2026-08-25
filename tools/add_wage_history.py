from pathlib import Path

# ---------------- SettingsActivity ----------------
p = Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s = p.read_text()

s = s.replace('''    private static final String MONTHLY_SALARY_KEY = "monthly_salary";''', '''    private static final String MONTHLY_SALARY_KEY = "monthly_salary";\n    private static final String WAGE_HISTORY_KEY = "wage_history";''', 1)

old_fields = '''    private RadioButton hourlyWageMode;\n    private RadioButton monthlyWageMode;\n    private EditText hourlyRateInput;\n    private EditText monthlySalaryInput;\n    private LinearLayout hourlyWageGroup;\n    private LinearLayout monthlyWageGroup;'''
new_fields = '''    private TextView currentWageText;\n    private LinearLayout wageHistoryContainer;'''
if old_fields not in s:
    raise SystemExit('old wage fields not found')
s = s.replace(old_fields, new_fields, 1)

# Insert wage-history section before Save button.
anchor = '''        Button save = new Button(this);\n'''
wage_ui = '''        TextView wageTitle = text("工资设置", 17, true);\n        wageTitle.setPadding(0, dp(26), 0, dp(6));\n        root.addView(wageTitle);\n        TextView wageInfo = text("工资按生效日期保存历史。修改工资不会重算成新工资；历史日期继续使用当时有效的工资。", 13, false);\n        wageInfo.setPadding(0, 0, 0, dp(10));\n        root.addView(wageInfo);\n\n        LinearLayout currentWageCard = new LinearLayout(this);\n        currentWageCard.setOrientation(LinearLayout.VERTICAL);\n        currentWageCard.setPadding(dp(14), dp(12), dp(14), dp(12));\n        currentWageCard.setBackground(UiStyle.roundRect(this, UiStyle.CARD_BG, 16, UiStyle.BORDER, 1));\n        currentWageCard.addView(text("当前工资", 13, false));\n        currentWageText = text("未设置", 20, true);\n        currentWageText.setPadding(0, dp(4), 0, dp(2));\n        currentWageCard.addView(currentWageText);\n        TextView currentHint = text("按日期自动匹配最近一次已生效的工资记录", 12, false);\n        currentWageCard.addView(currentHint);\n        root.addView(currentWageCard);\n\n        Button addWageChange = new Button(this);\n        addWageChange.setText("＋ 新增工资变更");\n        UiStyle.button(this, addWageChange, true);\n        LinearLayout.LayoutParams addWageParams = new LinearLayout.LayoutParams(-1, dp(50));\n        addWageParams.topMargin = dp(10);\n        root.addView(addWageChange, addWageParams);\n        addWageChange.setOnClickListener(v -> showAddWageChangeDialog());\n\n        TextView historyTitle = text("工资历史", 15, true);\n        historyTitle.setPadding(0, dp(14), 0, dp(6));\n        root.addView(historyTitle);\n        wageHistoryContainer = new LinearLayout(this);\n        wageHistoryContainer.setOrientation(LinearLayout.VERTICAL);\n        root.addView(wageHistoryContainer);\n\n'''
if anchor not in s:
    raise SystemExit('save button anchor not found')
s = s.replace(anchor, wage_ui + anchor, 1)

# Remove old loadSettings wage control access, replace with migration + refresh.
old_load = '''        String wageMode = prefs.getString(WAGE_MODE_KEY, "hourly");\n        hourlyWageMode.setChecked(!"monthly".equals(wageMode));\n        monthlyWageMode.setChecked("monthly".equals(wageMode));\n        hourlyRateInput.setText(trimMoney(prefs.getFloat(HOURLY_RATE_KEY, 0f)));\n        monthlySalaryInput.setText(trimMoney(prefs.getFloat(MONTHLY_SALARY_KEY, 0f)));\n        updateWageModeVisibility();\n'''
new_load = '''        ensureWageHistoryMigrated();\n        refreshWageHistoryUi();\n'''
if old_load not in s:
    raise SystemExit('old wage load block not found')
s = s.replace(old_load, new_load, 1)

# Remove old wage validation from save().
old_save_validate = '''        boolean monthlyWage = monthlyWageMode.isChecked();\n        Float hourlyRate = parseMoney(hourlyRateInput, "请输入正确的时薪");\n        Float monthlySalary = parseMoney(monthlySalaryInput, "请输入正确的月薪");\n        if (hourlyRate == null || monthlySalary == null) return;\n\n'''
if old_save_validate not in s:
    raise SystemExit('old wage save validation not found')
s = s.replace(old_save_validate, '', 1)

old_editor_tail = '''                .putString(WorkAlarmManager.ALARM_TIME_KEY, alarmTime)\n                .putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime)\n                .putString(WAGE_MODE_KEY, monthlyWage ? "monthly" : "hourly")\n                .putFloat(HOURLY_RATE_KEY, hourlyRate)\n                .putFloat(MONTHLY_SALARY_KEY, monthlySalary);'''
new_editor_tail = '''                .putString(WorkAlarmManager.ALARM_TIME_KEY, alarmTime)\n                .putString(WorkAlarmUpdateScheduler.UPDATE_TIME_KEY, alarmUpdateTime);'''
if old_editor_tail not in s:
    raise SystemExit('old wage editor tail not found')
s = s.replace(old_editor_tail, new_editor_tail, 1)

# Replace obsolete wage helper methods with history implementation.
start = s.find('    private void updateWageModeVisibility() {')
end = s.find('    private void chooseWorkStartDate() {', start)
if start < 0 or end < 0:
    raise SystemExit('old wage helper section not found')
helpers = r'''    private JSONArray readWageHistory() {
        String raw = prefs.getString(WAGE_HISTORY_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return new JSONArray();
        try { return new JSONArray(raw); }
        catch (JSONException e) { return new JSONArray(); }
    }

    private void ensureWageHistoryMigrated() {
        JSONArray existing = readWageHistory();
        if (existing.length() > 0) return;
        String mode = prefs.getString(WAGE_MODE_KEY, "hourly");
        float amount = "monthly".equals(mode)
                ? prefs.getFloat(MONTHLY_SALARY_KEY, 0f)
                : prefs.getFloat(HOURLY_RATE_KEY, 0f);
        if (amount <= 0f) return;
        String start = prefs.getString(WORK_START_DATE_KEY, "");
        if (start == null || start.isEmpty()) start = "1970-01-01";
        try {
            JSONArray arr = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("effectiveDate", start);
            item.put("mode", "monthly".equals(mode) ? "monthly" : "hourly");
            item.put("amount", amount);
            arr.put(item);
            prefs.edit().putString(WAGE_HISTORY_KEY, arr.toString()).apply();
        } catch (JSONException ignored) { }
    }

    private void refreshWageHistoryUi() {
        if (currentWageText == null || wageHistoryContainer == null) return;
        ensureWageHistoryMigrated();
        JSONArray arr = readWageHistory();
        wageHistoryContainer.removeAllViews();
        JSONObject current = findWageRule(LocalDate.now(), arr);
        if (current == null) currentWageText.setText("未设置");
        else currentWageText.setText(formatWageRule(current));

        if (arr.length() == 0) {
            TextView empty = text("暂无工资记录。点击“新增工资变更”设置第一条工资。", 13, false);
            empty.setPadding(0, dp(4), 0, dp(8));
            wageHistoryContainer.addView(empty);
            return;
        }

        java.util.ArrayList<JSONObject> rows = new java.util.ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) rows.add(o);
        }
        rows.sort((a, b) -> b.optString("effectiveDate", "").compareTo(a.optString("effectiveDate", "")));
        for (JSONObject item : rows) {
            String date = item.optString("effectiveDate", "");
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(8), dp(10));
            row.setBackground(UiStyle.roundRect(this, UiStyle.CARD_BG, 14, UiStyle.BORDER, 1));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            TextView amount = text(formatWageRule(item), 15, true);
            TextView dateText = text(date + " 起生效", 12, false);
            dateText.setPadding(0, dp(2), 0, 0);
            info.addView(amount);
            info.addView(dateText);
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

            Button delete = new Button(this);
            delete.setText("删除");
            delete.setTextSize(13);
            UiStyle.button(this, delete, false);
            delete.setOnClickListener(v -> confirmDeleteWageRule(date));
            row.addView(delete, new LinearLayout.LayoutParams(dp(72), dp(42)));

            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = dp(7);
            wageHistoryContainer.addView(row, rp);
        }
    }

    private JSONObject findWageRule(LocalDate date, JSONArray arr) {
        JSONObject best = null;
        LocalDate bestDate = null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            try {
                LocalDate d = LocalDate.parse(item.optString("effectiveDate", ""));
                if (d.isAfter(date)) continue;
                if (bestDate == null || d.isAfter(bestDate)) { bestDate = d; best = item; }
            } catch (Exception ignored) { }
        }
        return best;
    }

    private String formatWageRule(JSONObject item) {
        String mode = item.optString("mode", "hourly");
        double amount = item.optDouble("amount", 0d);
        return "monthly".equals(mode)
                ? String.format(Locale.UK, "£%.2f / 月", amount)
                : String.format(Locale.UK, "£%.2f / 小时", amount);
    }

    private void showAddWageChangeDialog() {
        final LocalDate[] effective = {LocalDate.now()};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        TextView dateLabel = text("生效日期", 14, true);
        box.addView(dateLabel);
        Button dateButton = new Button(this);
        UiStyle.button(this, dateButton, false);
        Runnable updateDate = () -> dateButton.setText(effective[0].format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        updateDate.run();
        dateButton.setOnClickListener(v -> {
            LocalDate initial = effective[0];
            DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
                effective[0] = LocalDate.of(y, m + 1, d);
                updateDate.run();
            }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
            LocalDate ws = getSavedWorkStartDate();
            if (ws != null) picker.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            picker.show();
        });
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, dp(48));
        dpv.bottomMargin = dp(12);
        box.addView(dateButton, dpv);

        TextView modeLabel = text("工资类型", 14, true);
        box.addView(modeLabel);
        RadioGroup modes = new RadioGroup(this);
        modes.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton hourly = new RadioButton(this); hourly.setText("时薪"); hourly.setChecked(true);
        RadioButton monthly = new RadioButton(this); monthly.setText("月薪");
        modes.addView(hourly, new RadioGroup.LayoutParams(0, -2, 1f));
        modes.addView(monthly, new RadioGroup.LayoutParams(0, -2, 1f));
        box.addView(modes);

        TextView amountLabel = text("金额（£）", 14, true);
        amountLabel.setPadding(0, dp(8), 0, 0);
        box.addView(amountLabel);
        EditText amount = input("例如 13.20", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(amount, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView hint = text("同一生效日期只能保留一条记录；如果日期相同，新记录会替换旧记录。", 12, false);
        hint.setPadding(0, dp(8), 0, 0);
        box.addView(hint);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新增工资变更")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            float value;
            try { value = Float.parseFloat(amount.getText().toString().trim()); }
            catch (Exception e) { amount.setError("请输入正确的工资金额"); return; }
            if (value < 0f || value > 10000000f) { amount.setError("请输入正确的工资金额"); return; }
            saveWageRule(effective[0], monthly.isChecked() ? "monthly" : "hourly", value);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private LocalDate getSavedWorkStartDate() {
        String raw = prefs.getString(WORK_START_DATE_KEY, "");
        try { return raw == null || raw.isEmpty() ? null : LocalDate.parse(raw); }
        catch (Exception e) { return null; }
    }

    private void saveWageRule(LocalDate date, String mode, float amount) {
        JSONArray arr = readWageHistory();
        JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject old = arr.optJSONObject(i);
            if (old == null || date.toString().equals(old.optString("effectiveDate", ""))) continue;
            out.put(old);
        }
        try {
            JSONObject item = new JSONObject();
            item.put("effectiveDate", date.toString());
            item.put("mode", "monthly".equals(mode) ? "monthly" : "hourly");
            item.put("amount", amount);
            out.put(item);
        } catch (JSONException e) {
            Toast.makeText(this, "保存工资记录失败", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit().putString(WAGE_HISTORY_KEY, out.toString());
        JSONObject latest = findLatestWageRule(out);
        if (latest != null) {
            String latestMode = latest.optString("mode", "hourly");
            float latestAmount = (float) latest.optDouble("amount", 0d);
            editor.putString(WAGE_MODE_KEY, latestMode);
            if ("monthly".equals(latestMode)) editor.putFloat(MONTHLY_SALARY_KEY, latestAmount);
            else editor.putFloat(HOURLY_RATE_KEY, latestAmount);
        }
        editor.apply();
        refreshWageHistoryUi();
        Toast.makeText(this, "工资变更已保存", Toast.LENGTH_SHORT).show();
    }

    private JSONObject findLatestWageRule(JSONArray arr) {
        JSONObject best = null;
        String bestDate = "";
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            String d = item.optString("effectiveDate", "");
            if (d.compareTo(bestDate) > 0) { bestDate = d; best = item; }
        }
        return best;
    }

    private void confirmDeleteWageRule(String effectiveDate) {
        new AlertDialog.Builder(this)
                .setTitle("删除工资记录？")
                .setMessage(effectiveDate + " 起的工资记录将被删除，之后的历史工资计算会改用更早的一条记录。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    JSONArray arr = readWageHistory();
                    JSONArray out = new JSONArray();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.optJSONObject(i);
                        if (item == null || effectiveDate.equals(item.optString("effectiveDate", ""))) continue;
                        out.put(item);
                    }
                    SharedPreferences.Editor editor = prefs.edit().putString(WAGE_HISTORY_KEY, out.toString());
                    JSONObject latest = findLatestWageRule(out);
                    if (latest != null) {
                        String mode = latest.optString("mode", "hourly");
                        float value = (float) latest.optDouble("amount", 0d);
                        editor.putString(WAGE_MODE_KEY, mode);
                        if ("monthly".equals(mode)) editor.putFloat(MONTHLY_SALARY_KEY, value);
                        else editor.putFloat(HOURLY_RATE_KEY, value);
                    }
                    editor.apply();
                    refreshWageHistoryUi();
                }).show();
    }

'''
s = s[:start] + helpers + s[end:]

p.write_text(s)

# ---------------- WagePanel ----------------
p = Path('app/src/main/java/com/example/workhours/WagePanel.java')
s = p.read_text()

# Add JSON imports if needed.
if 'import org.json.JSONArray;' not in s:
    marker = 'import java.time.DayOfWeek;\n'
    s = s.replace(marker, 'import org.json.JSONArray;\nimport org.json.JSONObject;\n\n' + marker, 1)

s = s.replace('''    private static final String MONTHLY_SALARY_KEY = "monthly_salary";''', '''    private static final String MONTHLY_SALARY_KEY = "monthly_salary";\n    private static final String WAGE_HISTORY_KEY = "wage_history";''', 1)

old = '''    private float getWageBeforeDeduction(LocalDate date) {\n        LocalDate ws = getWorkStartDate();\n        if (date.isAfter(LocalDate.now()) || (ws != null && date.isBefore(ws))) return 0f;\n        if (!isMonthlyMode()) return getHours(date) * prefs.getFloat(HOURLY_RATE_KEY, 0f);\n        if (!isPlannedPaidDay(date)) return 0f;\n        float monthly = prefs.getFloat(MONTHLY_SALARY_KEY, 0f);\n        int planned = getPlannedPaidDays(YearMonth.from(date));\n        return planned <= 0 ? 0f : monthly / planned;\n    }\n\n    private boolean isMonthlyMode() {\n        return "monthly".equals(prefs.getString(WAGE_MODE_KEY, "hourly"));\n    }'''
new = '''    private float getWageBeforeDeduction(LocalDate date) {\n        LocalDate ws = getWorkStartDate();\n        if (date.isAfter(LocalDate.now()) || (ws != null && date.isBefore(ws))) return 0f;\n        WageRule rule = getWageRuleForDate(date);\n        if (rule == null) return 0f;\n        if (!"monthly".equals(rule.mode)) return getHours(date) * rule.amount;\n        if (!isPlannedPaidDay(date)) return 0f;\n        int planned = getPlannedPaidDays(YearMonth.from(date));\n        return planned <= 0 ? 0f : rule.amount / planned;\n    }\n\n    private boolean isMonthlyMode() {\n        WageRule rule = getWageRuleForDate(LocalDate.now());\n        return rule != null && "monthly".equals(rule.mode);\n    }\n\n    private WageRule getWageRuleForDate(LocalDate date) {\n        String raw = prefs.getString(WAGE_HISTORY_KEY, "");\n        if (raw != null && !raw.trim().isEmpty()) {\n            try {\n                JSONArray arr = new JSONArray(raw);\n                LocalDate bestDate = null;\n                WageRule best = null;\n                for (int i = 0; i < arr.length(); i++) {\n                    JSONObject item = arr.optJSONObject(i);\n                    if (item == null) continue;\n                    LocalDate effective;\n                    try { effective = LocalDate.parse(item.optString("effectiveDate", "")); }\n                    catch (Exception ignored) { continue; }\n                    if (effective.isAfter(date)) continue;\n                    if (bestDate == null || effective.isAfter(bestDate)) {\n                        bestDate = effective;\n                        best = new WageRule(item.optString("mode", "hourly"), (float)item.optDouble("amount", 0d));\n                    }\n                }\n                if (best != null) return best;\n            } catch (Exception ignored) { }\n        }\n        String mode = prefs.getString(WAGE_MODE_KEY, "hourly");\n        float amount = "monthly".equals(mode) ? prefs.getFloat(MONTHLY_SALARY_KEY, 0f) : prefs.getFloat(HOURLY_RATE_KEY, 0f);\n        return amount <= 0f ? null : new WageRule(mode, amount);\n    }\n\n    private static class WageRule {\n        final String mode; final float amount;\n        WageRule(String mode, float amount) { this.mode = mode; this.amount = amount; }\n    }'''
if old not in s:
    raise SystemExit('WagePanel wage calculation block not found')
s = s.replace(old, new, 1)

p.write_text(s)
