from pathlib import Path

# New centralized holiday calendar
p=Path('app/src/main/java/com/example/workhours/HolidayCalendar.java')
p.write_text(r'''package com.example.workhours;

import android.content.SharedPreferences;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

public final class HolidayCalendar {
    public static final String REGION_KEY = "holiday_region";
    public static final String DEFAULT_REGION = "gb-ew";

    private HolidayCalendar() { }

    public static String getHolidayName(SharedPreferences prefs, LocalDate date) {
        String region = prefs.getString(REGION_KEY, DEFAULT_REGION);
        if (region == null) region = DEFAULT_REGION;
        switch (region) {
            case "none": return null;
            case "gb-sc": return scotland(date);
            case "gb-ni": return northernIreland(date);
            case "us": return unitedStates(date);
            case "ca": return canada(date);
            case "au": return australia(date);
            case "gb-ew":
            default: return englandWales(date);
        }
    }

    public static boolean isHoliday(SharedPreferences prefs, LocalDate date) {
        return getHolidayName(prefs, date) != null;
    }

    public static String label(String region) {
        if (region == null) region = DEFAULT_REGION;
        switch (region) {
            case "gb-sc": return "英国－苏格兰";
            case "gb-ni": return "英国－北爱尔兰";
            case "us": return "美国－联邦公共假日";
            case "ca": return "加拿大－联邦公共假日";
            case "au": return "澳大利亚－全国共同假日";
            case "none": return "不使用公共假日";
            case "gb-ew":
            default: return "英国－英格兰和威尔士";
        }
    }

    public static String[] regions() {
        return new String[]{"gb-ew", "gb-sc", "gb-ni", "us", "ca", "au", "none"};
    }

    public static String[] labels() {
        String[] regions = regions();
        String[] labels = new String[regions.length];
        for (int i = 0; i < regions.length; i++) labels[i] = label(regions[i]);
        return labels;
    }

    private static String englandWales(LocalDate date) {
        int y=date.getYear();
        if(date.equals(observed(LocalDate.of(y,1,1)))) return "元旦";
        LocalDate easter=easterSunday(y);
        if(date.equals(easter.minusDays(2))) return "耶稣受难日";
        if(date.equals(easter.plusDays(1))) return "复活节星期一";
        if(date.equals(LocalDate.of(y,5,1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)))) return "五月初银行假日";
        if(date.equals(LocalDate.of(y,5,31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))) return "春季银行假日";
        if(date.equals(LocalDate.of(y,8,31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))) return "夏季银行假日";
        String christmas = christmasPair(date, y);
        return christmas;
    }

    private static String scotland(LocalDate date) {
        int y=date.getYear();
        LocalDate[] newYear = observedPair(LocalDate.of(y,1,1), LocalDate.of(y,1,2));
        if(date.equals(newYear[0])) return "元旦";
        if(date.equals(newYear[1])) return "1月2日银行假日";
        LocalDate easter=easterSunday(y);
        if(date.equals(easter.minusDays(2))) return "耶稣受难日";
        if(date.equals(LocalDate.of(y,5,1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)))) return "五月初银行假日";
        if(date.equals(LocalDate.of(y,5,31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))) return "春季银行假日";
        if(date.equals(LocalDate.of(y,8,1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)))) return "夏季银行假日";
        if(date.equals(observed(LocalDate.of(y,11,30)))) return "圣安德鲁日";
        return christmasPair(date,y);
    }

    private static String northernIreland(LocalDate date) {
        String base=englandWales(date);
        if(base!=null) return base;
        int y=date.getYear();
        if(date.equals(observed(LocalDate.of(y,3,17)))) return "圣帕特里克节";
        if(date.equals(observed(LocalDate.of(y,7,12)))) return "博因河战役纪念日";
        return null;
    }

    private static String unitedStates(LocalDate date) {
        int y=date.getYear();
        if(date.equals(usObserved(LocalDate.of(y,1,1)))) return "元旦";
        if(date.equals(nthWeekday(y,1,DayOfWeek.MONDAY,3))) return "马丁·路德·金纪念日";
        if(date.equals(nthWeekday(y,2,DayOfWeek.MONDAY,3))) return "华盛顿诞辰纪念日";
        if(date.equals(LocalDate.of(y,5,31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))) return "阵亡将士纪念日";
        if(date.equals(usObserved(LocalDate.of(y,6,19)))) return "六月节";
        if(date.equals(usObserved(LocalDate.of(y,7,4)))) return "独立日";
        if(date.equals(nthWeekday(y,9,DayOfWeek.MONDAY,1))) return "劳动节";
        if(date.equals(nthWeekday(y,10,DayOfWeek.MONDAY,2))) return "哥伦布日";
        if(date.equals(usObserved(LocalDate.of(y,11,11)))) return "退伍军人节";
        if(date.equals(nthWeekday(y,11,DayOfWeek.THURSDAY,4))) return "感恩节";
        if(date.equals(usObserved(LocalDate.of(y,12,25)))) return "圣诞节";
        return null;
    }

    private static String canada(LocalDate date) {
        int y=date.getYear();
        if(date.equals(observed(LocalDate.of(y,1,1)))) return "元旦";
        LocalDate easter=easterSunday(y);
        if(date.equals(easter.minusDays(2))) return "耶稣受难日";
        LocalDate victoria=LocalDate.of(y,5,24).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if(date.equals(victoria)) return "维多利亚日";
        if(date.equals(observed(LocalDate.of(y,7,1)))) return "加拿大日";
        if(date.equals(nthWeekday(y,9,DayOfWeek.MONDAY,1))) return "劳动节";
        if(date.equals(observed(LocalDate.of(y,9,30)))) return "全国真相与和解日";
        if(date.equals(nthWeekday(y,10,DayOfWeek.MONDAY,2))) return "感恩节";
        if(date.equals(observed(LocalDate.of(y,11,11)))) return "国殇纪念日";
        LocalDate[] xmas=observedPair(LocalDate.of(y,12,25),LocalDate.of(y,12,26));
        if(date.equals(xmas[0])) return "圣诞节";
        if(date.equals(xmas[1])) return "节礼日";
        return null;
    }

    private static String australia(LocalDate date) {
        int y=date.getYear();
        if(date.equals(observed(LocalDate.of(y,1,1)))) return "元旦";
        if(date.equals(observed(LocalDate.of(y,1,26)))) return "澳大利亚日";
        LocalDate easter=easterSunday(y);
        if(date.equals(easter.minusDays(2))) return "耶稣受难日";
        if(date.equals(easter.plusDays(1))) return "复活节星期一";
        if(date.equals(LocalDate.of(y,4,25))) return "澳新军团日";
        LocalDate[] xmas=observedPair(LocalDate.of(y,12,25),LocalDate.of(y,12,26));
        if(date.equals(xmas[0])) return "圣诞节";
        if(date.equals(xmas[1])) return "节礼日";
        return null;
    }

    private static String christmasPair(LocalDate date,int y){
        LocalDate[] pair=observedPair(LocalDate.of(y,12,25),LocalDate.of(y,12,26));
        if(date.equals(pair[0])) return "圣诞节";
        if(date.equals(pair[1])) return "节礼日";
        return null;
    }

    private static LocalDate observed(LocalDate date){
        if(date.getDayOfWeek()==DayOfWeek.SATURDAY) return date.plusDays(2);
        if(date.getDayOfWeek()==DayOfWeek.SUNDAY) return date.plusDays(1);
        return date;
    }

    private static LocalDate usObserved(LocalDate date){
        if(date.getDayOfWeek()==DayOfWeek.SATURDAY) return date.minusDays(1);
        if(date.getDayOfWeek()==DayOfWeek.SUNDAY) return date.plusDays(1);
        return date;
    }

    private static LocalDate[] observedPair(LocalDate first, LocalDate second){
        Set<LocalDate> used=new HashSet<>();
        LocalDate a=nextWeekday(first,used); used.add(a);
        LocalDate b=nextWeekday(second,used);
        return new LocalDate[]{a,b};
    }

    private static LocalDate nextWeekday(LocalDate date,Set<LocalDate> used){
        LocalDate d=date;
        while(d.getDayOfWeek()==DayOfWeek.SATURDAY || d.getDayOfWeek()==DayOfWeek.SUNDAY || used.contains(d)) d=d.plusDays(1);
        return d;
    }

    private static LocalDate nthWeekday(int y,int month,DayOfWeek day,int n){
        LocalDate first=LocalDate.of(y,month,1).with(TemporalAdjusters.firstInMonth(day));
        return first.plusWeeks(n-1L);
    }

    private static LocalDate easterSunday(int year){
        int a=year%19,b=year/100,c=year%100,d=b/4,e=b%4,f=(b+8)/25,g=(b-f+1)/3;
        int h=(19*a+b-d-g+15)%30,i=c/4,k=c%4,l=(32+2*e+2*i-h-k)%7,m=(a+11*h+22*l)/451;
        int month=(h+l-7*m+114)/31, day=((h+l-7*m+114)%31)+1;
        return LocalDate.of(year,month,day);
    }
}
''')

# SettingsActivity UI/state
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
s=s.replace('''    private String restRuleMode = "weekly";''','''    private String restRuleMode = "weekly";\n    private String holidayRegion = HolidayCalendar.DEFAULT_REGION;\n    private Button holidayRegionButton;''',1)

anchor='''        TextView restRuleTitle = text("休息规则", 17, true);'''
ui='''        TextView holidayTitle = text("公共假日", 17, true);\n        holidayTitle.setPadding(0, dp(24), 0, dp(6));\n        root.addView(holidayTitle);\n        TextView holidayInfo = text("选择国家或地区后，公共假日会自动从工时、工资和上班闹钟中排除。", 13, false);\n        holidayInfo.setPadding(0, 0, 0, dp(8));\n        root.addView(holidayInfo);\n        holidayRegionButton = new Button(this);\n        UiStyle.button(this, holidayRegionButton, false);\n        holidayRegionButton.setOnClickListener(v -> chooseHolidayRegion());\n        root.addView(holidayRegionButton, new LinearLayout.LayoutParams(-1, dp(50)));\n\n'''
if anchor not in s: raise SystemExit('settings rest anchor missing')
s=s.replace(anchor,ui+anchor,1)

needle='''        restRuleMode = prefs.getString(REST_RULE_MODE_KEY, "weekly");'''
rep='''        restRuleMode = prefs.getString(REST_RULE_MODE_KEY, "weekly");\n        holidayRegion = prefs.getString(HolidayCalendar.REGION_KEY, HolidayCalendar.DEFAULT_REGION);'''
if needle not in s: raise SystemExit('load rest mode missing')
s=s.replace(needle,rep,1)
needle='''        updateMonthlyRestButton();\n        setRestRuleMode(restRuleMode);'''
rep='''        updateMonthlyRestButton();\n        updateHolidayRegionButton();\n        setRestRuleMode(restRuleMode);'''
if needle not in s: raise SystemExit('load UI anchor missing')
s=s.replace(needle,rep,1)
needle='''.putString(REST_RULE_MODE_KEY, restRuleMode)'''
rep='''.putString(REST_RULE_MODE_KEY, restRuleMode)\n                .putString(HolidayCalendar.REGION_KEY, holidayRegion)'''
if needle not in s: raise SystemExit('save mode anchor missing')
s=s.replace(needle,rep,1)

helper_anchor='''    private void setRestRuleMode(String mode) {'''
helpers='''    private void chooseHolidayRegion() {\n        String[] regions = HolidayCalendar.regions();\n        String[] labels = HolidayCalendar.labels();\n        int selected = 0;\n        for (int i=0;i<regions.length;i++) if (regions[i].equals(holidayRegion)) { selected=i; break; }\n        final int[] choice = {selected};\n        new AlertDialog.Builder(this)\n                .setTitle("公共假日国家 / 地区")\n                .setSingleChoiceItems(labels, selected, (dialog, which) -> choice[0] = which)\n                .setNegativeButton("取消", null)\n                .setPositiveButton("确定", (dialog, which) -> {\n                    holidayRegion = regions[choice[0]];\n                    updateHolidayRegionButton();\n                })\n                .show();\n    }\n\n    private void updateHolidayRegionButton() {\n        if (holidayRegionButton != null) holidayRegionButton.setText(HolidayCalendar.label(holidayRegion) + "  ›");\n    }\n\n'''
if helper_anchor not in s: raise SystemExit('settings helper anchor missing')
s=s.replace(helper_anchor,helpers+helper_anchor,1)
p.write_text(s)

# MainActivity centralize holiday name.
p=Path('app/src/main/java/com/example/workhours/MainActivity.java')
s=p.read_text()
start=s.find('''    private boolean isBankHoliday(LocalDate d){return getBankHolidayName(d)!=null;}''')
if start<0: raise SystemExit('Main bank holiday method missing')
name_start=s.find('''    private String getBankHolidayName(LocalDate date){''',start)
name_end=s.find('''    private LocalDate observedDate(LocalDate d){''',name_start)
if name_start<0 or name_end<0: raise SystemExit('Main holiday name block missing')
s=s[:start]+'''    private boolean isBankHoliday(LocalDate d){return HolidayCalendar.isHoliday(prefs,d);}\n    private String getBankHolidayName(LocalDate d){return HolidayCalendar.getHolidayName(prefs,d);}\n'''+s[name_end:]
p.write_text(s)

# WagePanel centralize holiday check/name, keep old helpers dead if necessary.
p=Path('app/src/main/java/com/example/workhours/WagePanel.java')
s=p.read_text()
start=s.find('''    private boolean isBankHoliday(LocalDate d) { return getBankHolidayName(d) != null; }''')
if start<0: raise SystemExit('Wage bank holiday method missing')
name_start=s.find('''    private String getBankHolidayName(LocalDate date) {''',start)
name_end=s.find('''    private LocalDate observedDate(LocalDate d) {''',name_start)
if name_start<0 or name_end<0: raise SystemExit('Wage holiday name block missing')
s=s[:start]+'''    private boolean isBankHoliday(LocalDate d) { return HolidayCalendar.isHoliday(prefs, d); }\n    private String getBankHolidayName(LocalDate d) { return HolidayCalendar.getHolidayName(prefs, d); }\n\n'''+s[name_end:]
p.write_text(s)

# WorkAlarmManager uses centralized holiday calendar with prefs.
p=Path('app/src/main/java/com/example/workhours/WorkAlarmManager.java')
s=p.read_text()
s=s.replace('''        if (isBankHoliday(date)) return false;''','''        if (HolidayCalendar.isHoliday(prefs, date)) return false;''',1)
p.write_text(s)
