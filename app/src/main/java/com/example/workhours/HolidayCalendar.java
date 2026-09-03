package com.example.workhours;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

public final class HolidayCalendar {
    public static final String REGION_KEY = "holiday_region";
    public static final String HISTORY_KEY = "holiday_region_history";
    public static final String DEFAULT_REGION = "gb-ew";

    private HolidayCalendar() { }

    public static String getHolidayName(SharedPreferences prefs, LocalDate date) {
        String region = regionForDate(prefs, date);
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

    public static String regionForDate(SharedPreferences prefs, LocalDate date) {
        String raw = prefs.getString(HISTORY_KEY, "");
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                JSONArray arr = new JSONArray(raw);
                LocalDate bestDate = null;
                String bestRegion = null;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item == null) continue;
                    LocalDate effective;
                    try { effective = LocalDate.parse(item.optString("effectiveDate", "")); }
                    catch (Exception ignored) { continue; }
                    if (effective.isAfter(date)) continue;
                    if (bestDate == null || effective.isAfter(bestDate)) {
                        bestDate = effective;
                        bestRegion = item.optString("region", DEFAULT_REGION);
                    }
                }
                if (bestRegion != null) return bestRegion;
            } catch (Exception ignored) { }
        }
        String region = prefs.getString(REGION_KEY, DEFAULT_REGION);
        return region == null ? DEFAULT_REGION : region;
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
        return christmasPair(date, y);
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
        // A Saturday Jan 1 is federally observed on the previous Dec 31.
        // Check next year's New Year as well so the cross-year observed day is not missed.
        if(date.equals(usObserved(LocalDate.of(y+1,1,1)))) return "元旦";
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
