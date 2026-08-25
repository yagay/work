from pathlib import Path

# HolidayCalendar: resolve holiday region by effective-date history.
p=Path('app/src/main/java/com/example/workhours/HolidayCalendar.java')
s=p.read_text()
if 'org.json.JSONArray' not in s:
    s=s.replace('import android.content.SharedPreferences;','import android.content.SharedPreferences;\n\nimport org.json.JSONArray;\nimport org.json.JSONObject;')
s=s.replace('public static final String REGION_KEY = "holiday_region";','public static final String REGION_KEY = "holiday_region";\n    public static final String HISTORY_KEY = "holiday_region_history";',1)
old='''    public static String getHolidayName(SharedPreferences prefs, LocalDate date) {\n        String region = prefs.getString(REGION_KEY, DEFAULT_REGION);\n        if (region == null) region = DEFAULT_REGION;'''
new='''    public static String getHolidayName(SharedPreferences prefs, LocalDate date) {\n        String region = regionForDate(prefs, date);'''
if old not in s: raise SystemExit('HolidayCalendar getHolidayName anchor missing')
s=s.replace(old,new,1)
anchor='''    public static boolean isHoliday(SharedPreferences prefs, LocalDate date) {'''
method='''    public static String regionForDate(SharedPreferences prefs, LocalDate date) {\n        String raw = prefs.getString(HISTORY_KEY, "");\n        if (raw != null && !raw.trim().isEmpty()) {\n            try {\n                JSONArray arr = new JSONArray(raw);\n                LocalDate bestDate = null;\n                String bestRegion = null;\n                for (int i = 0; i < arr.length(); i++) {\n                    JSONObject item = arr.optJSONObject(i);\n                    if (item == null) continue;\n                    LocalDate effective;\n                    try { effective = LocalDate.parse(item.optString("effectiveDate", "")); }\n                    catch (Exception ignored) { continue; }\n                    if (effective.isAfter(date)) continue;\n                    if (bestDate == null || effective.isAfter(bestDate)) {\n                        bestDate = effective;\n                        bestRegion = item.optString("region", DEFAULT_REGION);\n                    }\n                }\n                if (bestRegion != null) return bestRegion;\n            } catch (Exception ignored) { }\n        }\n        String region = prefs.getString(REGION_KEY, DEFAULT_REGION);\n        return region == null ? DEFAULT_REGION : region;\n    }\n\n'''
if anchor not in s: raise SystemExit('HolidayCalendar isHoliday anchor missing')
s=s.replace(anchor,method+anchor,1)
p.write_text(s)

# SettingsActivity: effective-date holiday history UI.
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
s=s.replace('''    private Button holidayRegionButton;''','''    private Button holidayRegionButton;\n    private LinearLayout holidayHistoryContainer;''',1)
old='''        holidayRegionButton.setOnClickListener(v -> chooseHolidayRegion());\n        root.addView(holidayRegionButton, new LinearLayout.LayoutParams(-1, dp(50)));\n\n        TextView restRuleTitle'''
new='''        holidayRegionButton.setOnClickListener(v -> chooseHolidayRegion());\n        root.addView(holidayRegionButton, new LinearLayout.LayoutParams(-1, dp(50)));\n        TextView holidayHistoryTitle = text("公共假日历史", 14, true);\n        holidayHistoryTitle.setPadding(0, dp(10), 0, dp(5));\n        root.addView(holidayHistoryTitle);\n        holidayHistoryContainer = new LinearLayout(this);\n        holidayHistoryContainer.setOrientation(LinearLayout.VERTICAL);\n        root.addView(holidayHistoryContainer);\n\n        TextView restRuleTitle'''
if old not in s: raise SystemExit('Settings holiday UI anchor missing')
s=s.replace(old,new,1)

old='''        holidayRegion = prefs.getString(HolidayCalendar.REGION_KEY, HolidayCalendar.DEFAULT_REGION);'''
new='''        ensureHolidayHistoryMigrated();\n        holidayRegion = HolidayCalendar.regionForDate(prefs, LocalDate.now());'''
if old not in s: raise SystemExit('Settings holiday load anchor missing')
s=s.replace(old,new,1)
s=s.replace('''        updateHolidayRegionButton();\n        setRestRuleMode(restRuleMode);''','''        updateHolidayRegionButton();\n        refreshHolidayHistoryUi();\n        setRestRuleMode(restRuleMode);''',1)

start=s.find('    private void chooseHolidayRegion() {')
end=s.find('    private void setRestRuleMode(String mode) {',start)
if start<0 or end<0: raise SystemExit('Settings holiday methods block missing')
helpers=r'''    private JSONArray readHolidayHistory() {
        String raw = prefs.getString(HolidayCalendar.HISTORY_KEY, "");
        if (raw == null || raw.trim().isEmpty()) return new JSONArray();
        try { return new JSONArray(raw); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void ensureHolidayHistoryMigrated() {
        JSONArray existing = readHolidayHistory();
        if (existing.length() > 0) return;
        String region = prefs.getString(HolidayCalendar.REGION_KEY, HolidayCalendar.DEFAULT_REGION);
        if (region == null) region = HolidayCalendar.DEFAULT_REGION;
        String start = prefs.getString(WORK_START_DATE_KEY, "");
        if (start == null || start.isEmpty()) start = "1970-01-01";
        try {
            JSONArray arr = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("effectiveDate", start);
            item.put("region", region);
            arr.put(item);
            prefs.edit().putString(HolidayCalendar.HISTORY_KEY, arr.toString()).apply();
        } catch (Exception ignored) { }
    }

    private void chooseHolidayRegion() {
        String[] regions = HolidayCalendar.regions();
        String[] labels = HolidayCalendar.labels();
        String current = HolidayCalendar.regionForDate(prefs, LocalDate.now());
        int selected = 0;
        for (int i=0;i<regions.length;i++) if (regions[i].equals(current)) { selected=i; break; }
        final int[] choice = {selected};
        new AlertDialog.Builder(this)
                .setTitle("选择新的公共假日国家 / 地区")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> choice[0] = which)
                .setNegativeButton("取消", null)
                .setPositiveButton("下一步", (dialog, which) -> chooseHolidayEffectiveDate(regions[choice[0]]))
                .show();
    }

    private void chooseHolidayEffectiveDate(String region) {
        LocalDate initial = LocalDate.now();
        DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
            LocalDate effective = LocalDate.of(y, m + 1, d);
            saveHolidayRule(effective, region);
        }, initial.getYear(), initial.getMonthValue()-1, initial.getDayOfMonth());
        LocalDate ws = getSavedWorkStartDate();
        if (ws != null) picker.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        picker.setTitle("选择生效日期");
        picker.show();
    }

    private void saveHolidayRule(LocalDate effective, String region) {
        JSONArray arr = readHolidayHistory();
        JSONArray out = new JSONArray();
        for (int i=0;i<arr.length();i++) {
            JSONObject old = arr.optJSONObject(i);
            if (old == null || effective.toString().equals(old.optString("effectiveDate", ""))) continue;
            out.put(old);
        }
        try {
            JSONObject item = new JSONObject();
            item.put("effectiveDate", effective.toString());
            item.put("region", region);
            out.put(item);
        } catch (Exception e) { return; }
        prefs.edit().putString(HolidayCalendar.HISTORY_KEY, out.toString()).putString(HolidayCalendar.REGION_KEY, region).apply();
        holidayRegion = HolidayCalendar.regionForDate(prefs, LocalDate.now());
        updateHolidayRegionButton();
        refreshHolidayHistoryUi();
        WorkAlarmManager.forceSync(this);
        Toast.makeText(this, "公共假日设置已保存，从 " + effective + " 起生效", Toast.LENGTH_LONG).show();
    }

    private void refreshHolidayHistoryUi() {
        if (holidayHistoryContainer == null) return;
        ensureHolidayHistoryMigrated();
        JSONArray arr = readHolidayHistory();
        java.util.ArrayList<JSONObject> rows = new java.util.ArrayList<>();
        for (int i=0;i<arr.length();i++) { JSONObject o=arr.optJSONObject(i); if(o!=null) rows.add(o); }
        rows.sort((a,b)->b.optString("effectiveDate","").compareTo(a.optString("effectiveDate","")));
        holidayHistoryContainer.removeAllViews();
        for (JSONObject item: rows) {
            String date=item.optString("effectiveDate","");
            String region=item.optString("region",HolidayCalendar.DEFAULT_REGION);
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11),dp(8),dp(8),dp(8)); row.setBackground(UiStyle.roundRect(this,UiStyle.CARD_BG,12,UiStyle.BORDER,1));
            LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(HolidayCalendar.label(region),14,true));
            info.addView(text(date+" 起生效",12,false));
            row.addView(info,new LinearLayout.LayoutParams(0,-2,1f));
            Button del=new Button(this); del.setText("删除"); del.setTextSize(12); UiStyle.button(this,del,false);
            del.setOnClickListener(v->confirmDeleteHolidayRule(date));
            row.addView(del,new LinearLayout.LayoutParams(dp(70),dp(40)));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.bottomMargin=dp(6); holidayHistoryContainer.addView(row,rp);
        }
    }

    private void confirmDeleteHolidayRule(String date) {
        JSONArray arr=readHolidayHistory();
        if(arr.length()<=1){Toast.makeText(this,"至少保留一条公共假日历史",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("删除这条公共假日历史？").setMessage(date+" 起的规则将被删除，之后会使用更早一条有效规则。")
                .setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{
                    JSONArray out=new JSONArray();
                    for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o!=null&&!date.equals(o.optString("effectiveDate","")))out.put(o);}
                    prefs.edit().putString(HolidayCalendar.HISTORY_KEY,out.toString()).apply();
                    holidayRegion=HolidayCalendar.regionForDate(prefs,LocalDate.now()); updateHolidayRegionButton(); refreshHolidayHistoryUi(); WorkAlarmManager.forceSync(this);
                }).show();
    }

    private void updateHolidayRegionButton() {
        if (holidayRegionButton != null) holidayRegionButton.setText("当前：" + HolidayCalendar.label(HolidayCalendar.regionForDate(prefs, LocalDate.now())) + "  ›");
    }

'''
s=s[:start]+helpers+s[end:]
p.write_text(s)

# MainActivity: show concrete holiday names in calendar and details.
p=Path('app/src/main/java/com/example/workhours/MainActivity.java')
s=p.read_text()
s=s.replace('''                    value=moneyShort(wage);\n                    if(holiday||leave||manualRest||autoRest) value=wage>0?moneyShort(wage):"£0";''','''                    value=moneyShort(wage);\n                    if(holiday) value=(wage>0?moneyShort(wage):"£0")+"\\n"+getBankHolidayName(d);\n                    else if(leave||manualRest||autoRest) value=wage>0?moneyShort(wage):"£0";''',1)
s=s.replace('''                    if(holiday)value="假日"; else if(leave)value="请假";''','''                    if(holiday)value=getBankHolidayName(d); else if(leave)value="请假";''',1)
# Ensure calendar status may wrap.
s=s.replace('''TextView st=text(value,10,leave||holiday||manualRest||override||autoRest||overtime>0); st.setGravity(Gravity.CENTER); cell.addView(st);''','''TextView st=text(value,10,leave||holiday||manualRest||override||autoRest||overtime>0); st.setGravity(Gravity.CENTER); st.setSingleLine(false); cell.addView(st);''',1)
# Common detail strings: replace generic holiday status construction where present.
s=s.replace('''String status=h?"公共假日":l?"请假":r?"休息":''','''String status=h?("公共假日 · "+getBankHolidayName(d)):l?"请假":r?"休息":''')
s=s.replace('''String state=h?"公共假日":l?"请假":r?"休息":''','''String state=h?("公共假日 · "+getBankHolidayName(d)):l?"请假":r?"休息":''')
p.write_text(s)

# WagePanel: concrete holiday name in detail rows/status.
p=Path('app/src/main/java/com/example/workhours/WagePanel.java')
s=p.read_text()
s=s.replace('''        if (isBankHoliday(d)) return "公共假日";''','''        if (isBankHoliday(d)) return "公共假日 · " + getBankHolidayName(d);''',1)
p.write_text(s)
