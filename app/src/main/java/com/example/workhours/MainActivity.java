package com.example.workhours;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PREFS = "work_hours_prefs";
    private static final String OVERRIDE_PREFIX = "hours_";
    private static final String DAY_START_PREFIX = "day_start_";
    private static final String DAY_END_PREFIX = "day_end_";
    private static final String DAY_BREAK_PREFIX = "day_break_";
    private static final String OVERTIME_START_PREFIX = "overtime_start_";
    private static final String OVERTIME_END_PREFIX = "overtime_end_";
    private static final String LEAVE_PREFIX = "leave_";
    private static final String LEAVE_NOTE_PREFIX = "leave_note_";
    private static final String REST_PREFIX = "rest_";
    private static final String WAGE_DEDUCT_PREFIX = "wage_deduct_";
    private static final String START_TIME_KEY = "start_time";
    private static final String END_TIME_KEY = "end_time";
    private static final String BREAK_MINUTES_KEY = "break_minutes";
    private static final String WORK_START_DATE_KEY = "work_start_date";
    private static final String MONTHLY_REST_DAYS_KEY = "monthly_rest_days";
    private static final String REST_RULE_MODE_KEY = "rest_rule_mode";

    private SharedPreferences prefs;
    private YearMonth displayedMonth;
    private LocalDate displayedWeekStart;
    private LocalDate rangeStart, rangeEnd;
    private TextView monthTitle, totalHoursText, totalWageText, statusStatsText;
    private Button previousMonthButton, nextMonthButton, previousWeekButton, nextWeekButton;
    private GridLayout calendarGrid;
    private LinearLayout exceptionsContainer, weekDetailsContainer, rangeDetailsContainer;
    private LinearLayout monthSectionContainer, weekSectionContainer, rangeSectionContainer;
    private LinearLayout workContent, wageContent;
    private Button workWeekTabButton, workDateTabButton, workMonthTabButton;
    private TextView workMonthSummaryText;
    private LinearLayout workMonthDetailsContainer;
    private Button workTabButton, wageTabButton;
    private WagePanel wagePanel;
    private TextView weekTitle, weekSummaryText, rangeSummaryText;
    private Button rangeStartButton, rangeEndButton;
    private boolean showingWageStats = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LocalDate today = LocalDate.now();
        displayedMonth = YearMonth.now();
        displayedWeekStart = mondayOf(today);
        rangeStart = today.withDayOfMonth(1);
        LocalDate ws = getWorkStartDate();
        if (ws != null && rangeStart.isBefore(ws)) rangeStart = ws;
        rangeEnd = today;
        setContentView(buildUi());
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (monthTitle == null) return;
        LocalDate ws = getWorkStartDate();
        if (ws != null) {
            if (rangeStart.isBefore(ws)) rangeStart = ws;
            if (displayedMonth.isBefore(YearMonth.from(ws))) displayedMonth = YearMonth.from(ws);
            if (displayedWeekStart.isBefore(mondayOf(ws))) displayedWeekStart = mondayOf(ws);
        }
        refreshAll();
        if (wagePanel != null) wagePanel.refresh();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = vertical();
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(root);
        UiStyle.page(scroll);

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);
        top.addView(text("上班总时间", 26, true), new LinearLayout.LayoutParams(0, -2, 1f));
        Button settings = button("设置");
        UiStyle.button(this, settings, true);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        top.addView(settings, new LinearLayout.LayoutParams(dp(86), dp(48)));

        buildSharedMonthSummary(root);

        LinearLayout quickSwitch = horizontal();
        LinearLayout.LayoutParams quickSwitchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        quickSwitchParams.topMargin = dp(14);
        root.addView(quickSwitch, quickSwitchParams);
        workTabButton = button("工时统计");
        wageTabButton = button("工资统计");
        quickSwitch.addView(workTabButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams wageTabParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        wageTabParams.leftMargin = dp(8);
        quickSwitch.addView(wageTabButton, wageTabParams);

        buildSharedCalendar(root);

        workContent = vertical();
        wageContent = vertical();
        root.addView(workContent);
        root.addView(wageContent);
        wagePanel = new WagePanel(this);
        wageContent.addView(wagePanel, new LinearLayout.LayoutParams(-1, -2));
        wageContent.setVisibility(View.GONE);
        workTabButton.setOnClickListener(v -> showStatsTab(false));
        wageTabButton.setOnClickListener(v -> showStatsTab(true));
        showStatsTab(false);

        LinearLayout workViewTabs = horizontal();
        LinearLayout.LayoutParams workViewTabsParams = new LinearLayout.LayoutParams(-1, dp(46));
        workViewTabsParams.topMargin = dp(18);
        workContent.addView(workViewTabs, workViewTabsParams);
        workWeekTabButton = button("星期");
        workDateTabButton = button("日期");
        workMonthTabButton = button("月");
        workViewTabs.addView(workWeekTabButton, new LinearLayout.LayoutParams(0, dp(46), 1f));
        LinearLayout.LayoutParams workDateTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        workDateTabParams.leftMargin = dp(6);
        workViewTabs.addView(workDateTabButton, workDateTabParams);
        LinearLayout.LayoutParams workMonthTabParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        workMonthTabParams.leftMargin = dp(6);
        workViewTabs.addView(workMonthTabButton, workMonthTabParams);

        weekSectionContainer = vertical();
        rangeSectionContainer = vertical();
        monthSectionContainer = vertical();
        workContent.addView(weekSectionContainer);
        workContent.addView(rangeSectionContainer);
        workContent.addView(monthSectionContainer);
        buildWeekSection(weekSectionContainer);
        buildRangeSection(rangeSectionContainer);
        buildWorkMonthDetailSection(monthSectionContainer);

        workWeekTabButton.setOnClickListener(v -> showWorkViewTab(0));
        workDateTabButton.setOnClickListener(v -> showWorkViewTab(1));
        workMonthTabButton.setOnClickListener(v -> showWorkViewTab(2));
        showWorkViewTab(0);
        return scroll;
    }

    private void showWorkViewTab(int tab) {
        if (weekSectionContainer == null || rangeSectionContainer == null || monthSectionContainer == null) return;
        weekSectionContainer.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        rangeSectionContainer.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        monthSectionContainer.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        UiStyle.button(this, workWeekTabButton, tab == 0);
        UiStyle.button(this, workDateTabButton, tab == 1);
        UiStyle.button(this, workMonthTabButton, tab == 2);
        workWeekTabButton.setEnabled(tab != 0);
        workDateTabButton.setEnabled(tab != 1);
        workMonthTabButton.setEnabled(tab != 2);
        if (tab == 0) refreshWeek();
        else if (tab == 1) calculateRange();
        else refreshWorkMonthDetails();
    }

    private void showStatsTab(boolean wage) {
        if (workContent == null || wageContent == null) return;
        showingWageStats = wage;
        workContent.setVisibility(wage ? View.GONE : View.VISIBLE);
        wageContent.setVisibility(wage ? View.VISIBLE : View.GONE);
        UiStyle.button(this, workTabButton, !wage);
        UiStyle.button(this, wageTabButton, wage);
        workTabButton.setEnabled(wage);
        wageTabButton.setEnabled(!wage);
        if (wage && wagePanel != null) { wagePanel.setDisplayedMonth(displayedMonth); wagePanel.refresh(); }
        rebuildSharedCalendar(LocalDate.now());
    }

    private void buildSharedMonthSummary(LinearLayout root) {
        LinearLayout nav = horizontal();
        nav.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(76));
        np.topMargin = dp(14);
        root.addView(nav, np);
        previousMonthButton = button("‹");
        previousMonthButton.setTextSize(24);
        previousMonthButton.setOnClickListener(v -> { YearMonth target=displayedMonth.minusMonths(1); LocalDate ws=getWorkStartDate(); if(ws==null||!target.isBefore(YearMonth.from(ws))){displayedMonth=target;refreshMonth();}});
        nav.addView(previousMonthButton,new LinearLayout.LayoutParams(dp(58),dp(48)));
        monthTitle=text("",18,true); monthTitle.setGravity(Gravity.CENTER); monthTitle.setPadding(dp(8),dp(6),dp(8),dp(6)); monthTitle.setMinHeight(dp(68)); monthTitle.setOnClickListener(v->chooseWorkMonth());
        nav.addView(monthTitle,new LinearLayout.LayoutParams(0,-2,1f));
        nextMonthButton=button("›"); nextMonthButton.setTextSize(24); nextMonthButton.setOnClickListener(v->{if(displayedMonth.isBefore(YearMonth.now())){displayedMonth=displayedMonth.plusMonths(1);refreshMonth();}});
        nav.addView(nextMonthButton,new LinearLayout.LayoutParams(dp(58),dp(48)));

        LinearLayout cards=horizontal();
        root.addView(cards,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout hoursCard=card();
        hoursCard.addView(text("本月工时",13,false));
        totalHoursText=text("",25,true); totalHoursText.setPadding(0,dp(4),0,dp(4)); hoursCard.addView(totalHoursText);
        statusStatsText=statLineText(); statusStatsText.setTextSize(11); statusStatsText.setSingleLine(false); hoursCard.addView(statusStatsText);
        cards.addView(hoursCard,new LinearLayout.LayoutParams(0,-2,1f));
        LinearLayout wageCard=card();
        wageCard.addView(text("本月工资",13,false));
        totalWageText=text("£0.00",25,true); totalWageText.setPadding(0,dp(4),0,dp(4)); wageCard.addView(totalWageText);
        TextView wageHint=text("点击工资统计查看明细",11,false); wageCard.addView(wageHint);
        LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1f); wp.leftMargin=dp(8); cards.addView(wageCard,wp);
    }

    private void buildSharedCalendar(LinearLayout root) {
        TextView title = text("月历", 19, true);
        title.setPadding(0, dp(18), 0, dp(8));
        root.addView(title);
        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        root.addView(calendarGrid);
        TextView hint = text("工时统计显示每天工时；工资统计显示每天工资。点击日期可修改当天记录。", 13, false);
        hint.setPadding(0, dp(8), 0, dp(4));
        root.addView(hint);
    }

    private void rebuildSharedCalendar(LocalDate today) {
        if (calendarGrid == null) return;
        calendarGrid.removeAllViews();
        String[] hs={"一","二","三","四","五","六","日"};
        for(int i=0;i<7;i++){TextView h=text(hs[i],13,true);h.setGravity(Gravity.CENTER);if(i>=5)h.setTextColor(0xFF5F6368);calendarGrid.addView(h,gridParams());}
        LocalDate ws=getWorkStartDate();
        int leading=displayedMonth.atDay(1).getDayOfWeek().getValue()-1,days=displayedMonth.lengthOfMonth(),cells=((leading+days+6)/7)*7;
        float daily=getConfiguredDailyHours();
        for(int i=0;i<cells;i++){
            int day=i-leading+1;
            if(day<1||day>days){calendarGrid.addView(text("",13,false),gridParams());continue;}
            LocalDate d=displayedMonth.atDay(day);
            boolean future=d.isAfter(today),before=ws!=null&&d.isBefore(ws),weekend=d.getDayOfWeek()==DayOfWeek.SATURDAY||d.getDayOfWeek()==DayOfWeek.SUNDAY;
            boolean holiday=!before&&isBankHoliday(d),leave=!holiday&&!before&&isLeave(d),manualRest=!holiday&&!leave&&!before&&isManualRest(d),configured=!before&&isConfiguredWorkDay(d)&&!holiday,override=!holiday&&!leave&&!manualRest&&!before&&hasOverride(d),autoRest=!holiday&&!leave&&!manualRest&&!override&&!before&&!configured;
            float normal=(future||before)?0:getBaseHoursForDate(d,daily,configured),overtime=(future||before)?0:getOvertimeHours(d),total=normal+overtime;
            float wage=(future||before||wagePanel==null)?0:wagePanel.getWageForDateValue(d);
            LinearLayout cell=vertical(); cell.setGravity(Gravity.CENTER); cell.setPadding(dp(2),dp(6),dp(2),dp(5));
            if(d.equals(today))cell.setBackground(UiStyle.roundRect(this,0xFFE8F0FE,12,0xFFB8C8FF,1));
            else if(leave)cell.setBackground(UiStyle.roundRect(this,0xFFFFF0F0,12,0xFFF2CACA,1));
            else if(overtime>0)cell.setBackground(UiStyle.roundRect(this,0xFFF1F8F3,12,0xFFCDE7D4,1));
            else if(override)cell.setBackground(UiStyle.roundRect(this,0xFFFFF7E8,12,0xFFF1D8A6,1));
            else if(holiday)cell.setBackground(UiStyle.roundRect(this,0xFFFFF2F4,12,0xFFF3CDD3,1));
            else if(autoRest||manualRest||weekend)cell.setBackground(UiStyle.roundRect(this,0xFFF4F6F9,12,0xFFE1E6EE,1));
            TextView dt=text(String.valueOf(day),14,d.equals(today)); dt.setGravity(Gravity.CENTER); if(future||before)dt.setTextColor(0xFF9AA0A6); cell.addView(dt);
            String value="";
            if(!future&&!before){
                if(showingWageStats){
                    value=moneyShort(wage);
                    if(holiday) value=(wage>0?moneyShort(wage):"£0")+"\n"+getBankHolidayName(d);
                    else if(leave||manualRest||autoRest) value=wage>0?moneyShort(wage):"£0";
                }else{
                    if(holiday)value=getBankHolidayName(d); else if(leave)value="请假"; else if(manualRest||autoRest)value=overtime>0?shortHours(overtime):"休息"; else if(total>0)value=shortHours(total);
                }
            }
            TextView st=text(value,10,leave||holiday||manualRest||override||autoRest||overtime>0); st.setGravity(Gravity.CENTER); st.setSingleLine(false); cell.addView(st);
            if(!future&&!before)cell.setOnClickListener(v->{if(isBankHoliday(d))Toast.makeText(this,getBankHolidayName(d)+"：公共假日不计正常工时，可在其他工作日设置加班",Toast.LENGTH_SHORT).show();else showEditDayDialog(d);});
            calendarGrid.addView(cell,gridParams());
        }
    }

    private String moneyShort(float value) {
        if (Math.abs(value) < 0.005f) return "£0";
        if (Math.abs(value - Math.round(value)) < 0.005f) return "£" + Math.round(value);
        return String.format(Locale.UK, "£%.2f", value);
    }

    private void buildWorkMonthDetailSection(LinearLayout root) {
        TextView t=text("按月查看工时详情",19,true); t.setPadding(0,dp(24),0,dp(8)); root.addView(t);
        TextView hint=text("使用上方月份选择器切换月份。",13,false); hint.setPadding(0,0,0,dp(8)); root.addView(hint);
        LinearLayout c=card(); root.addView(c);
        workMonthSummaryText=text("",16,true); c.addView(workMonthSummaryText);
        workMonthDetailsContainer=vertical(); c.addView(workMonthDetailsContainer);
    }

    private void refreshWorkMonthDetails() {
        if(workMonthSummaryText==null||workMonthDetailsContainer==null)return;
        LocalDate today=LocalDate.now(),ws=getWorkStartDate();
        LocalDate start=displayedMonth.atDay(1); if(ws!=null&&start.isBefore(ws))start=ws;
        LocalDate end=displayedMonth.equals(YearMonth.now())?today:displayedMonth.atEndOfMonth();
        workMonthDetailsContainer.removeAllViews();
        if(start.isAfter(end)){workMonthSummaryText.setText("本月尚未开始工作");return;}
        Stats st=collectStats(start,end);
        workMonthSummaryText.setText(displayedMonth.getYear()+"年"+displayedMonth.getMonthValue()+"月\n总工时："+formatDurationHours(st.totalHours)+"（加班 "+formatDurationHours(st.overtimeHours)+"）\n"+StatusStatsFormatter.format(st.workDays,st.leaveDays,st.holidayDays,st.restDays));
        addPeriodDetails(workMonthDetailsContainer,start,end,true,true);
    }

    private void buildWeekSection(LinearLayout root) {
        TextView t = text("按星期查看", 19, true); t.setPadding(0, dp(24), 0, dp(8)); root.addView(t);
        LinearLayout nav = horizontal(); nav.setGravity(Gravity.CENTER_VERTICAL); root.addView(nav);
        previousWeekButton = button("‹"); previousWeekButton.setTextSize(24);
        previousWeekButton.setOnClickListener(v -> { LocalDate target = displayedWeekStart.minusWeeks(1); LocalDate ws = getWorkStartDate(); if (ws == null || !target.isBefore(mondayOf(ws))) { displayedWeekStart = target; refreshWeek(); }});
        nav.addView(previousWeekButton, new LinearLayout.LayoutParams(dp(58), dp(48)));
        weekTitle = text("", 17, true); weekTitle.setGravity(Gravity.CENTER);
        weekTitle.setPadding(dp(8), dp(12), dp(8), dp(12));
        weekTitle.setOnClickListener(v -> chooseWorkWeek());
        nav.addView(weekTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        nextWeekButton = button("›"); nextWeekButton.setTextSize(24);
        nextWeekButton.setOnClickListener(v -> { LocalDate cur = mondayOf(LocalDate.now()); if (displayedWeekStart.isBefore(cur)) { displayedWeekStart = displayedWeekStart.plusWeeks(1); refreshWeek(); }});
        nav.addView(nextWeekButton, new LinearLayout.LayoutParams(dp(58), dp(48)));
        LinearLayout c = card(); root.addView(c); weekSummaryText = text("", 16, true); c.addView(weekSummaryText); weekDetailsContainer = vertical(); c.addView(weekDetailsContainer);
    }

    private void buildRangeSection(LinearLayout root) {
        TextView t = text("日期范围统计", 19, true); t.setPadding(0, dp(24), 0, dp(8)); root.addView(t);
        LinearLayout row = horizontal(); row.setGravity(Gravity.CENTER_VERTICAL); root.addView(row);
        rangeStartButton = button(""); rangeStartButton.setOnClickListener(v -> showDatePicker(true)); row.addView(rangeStartButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        TextView to = text(" 至 ", 14, false); to.setGravity(Gravity.CENTER); row.addView(to, new LinearLayout.LayoutParams(dp(38), dp(50)));
        rangeEndButton = button(""); rangeEndButton.setOnClickListener(v -> showDatePicker(false)); row.addView(rangeEndButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        LinearLayout c = card(); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(10); root.addView(c, p); rangeSummaryText = text("", 16, true); c.addView(rangeSummaryText); rangeDetailsContainer = vertical(); c.addView(rangeDetailsContainer);
    }

    private void refreshAll() { updateRangeButtons(); refreshMonth(); refreshWeek(); calculateRange(); }

    private void refreshMonth() {
        LocalDate today=LocalDate.now(); YearMonth now=YearMonth.from(today); LocalDate ws=getWorkStartDate(); YearMonth first=ws==null?null:YearMonth.from(ws);
        if(displayedMonth.isAfter(now))displayedMonth=now; if(first!=null&&displayedMonth.isBefore(first))displayedMonth=first;
        monthTitle.setText(displayedMonth.getYear()+"年"+displayedMonth.getMonthValue()+"月\n点击选择月份");
        previousMonthButton.setEnabled(first==null||displayedMonth.isAfter(first)); nextMonthButton.setEnabled(displayedMonth.isBefore(now));
        LocalDate start=displayedMonth.atDay(1); if(ws!=null&&start.isBefore(ws))start=ws; LocalDate end=displayedMonth.equals(now)?today:displayedMonth.atEndOfMonth();
        Stats st=collectStats(start,end); totalHoursText.setText(formatDurationHours(st.totalHours));
        statusStatsText.setText(StatusStatsFormatter.format(st.workDays,st.leaveDays,st.holidayDays,st.restDays));
        if(wagePanel!=null){ wagePanel.setDisplayedMonth(displayedMonth); totalWageText.setText(String.format(Locale.UK,"£%.2f",wagePanel.getDisplayedMonthWage())); }
        rebuildSharedCalendar(today);
        refreshWorkMonthDetails();
    }

    private void refreshWeek() {
        LocalDate today = LocalDate.now(), cur = mondayOf(today), ws = getWorkStartDate(), first = ws == null ? null : mondayOf(ws);
        if (displayedWeekStart.isAfter(cur)) displayedWeekStart = cur; if (first != null && displayedWeekStart.isBefore(first)) displayedWeekStart = first;
        LocalDate weekEnd = displayedWeekStart.plusDays(6), start = displayedWeekStart; if (ws != null && start.isBefore(ws)) start = ws; LocalDate end = weekEnd.isAfter(today) ? today : weekEnd;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M月d日"); weekTitle.setText(displayedWeekStart.format(f) + " - " + weekEnd.format(f) + "\n点击选择星期");
        previousWeekButton.setEnabled(first == null || displayedWeekStart.isAfter(first)); nextWeekButton.setEnabled(displayedWeekStart.isBefore(cur)); weekDetailsContainer.removeAllViews();
        if (start.isAfter(end)) { weekSummaryText.setText("本周尚未开始工作"); return; }
        Stats s = collectStats(start, end);
        weekSummaryText.setText("总工时：" + formatDurationHours(s.totalHours)
                + "（其中加班 " + formatDurationHours(s.overtimeHours) + "）\n"
                + StatusStatsFormatter.format(s.workDays, s.leaveDays, s.holidayDays, s.restDays));
        addPeriodDetails(weekDetailsContainer, start, end, true, true);
    }

    private void rebuildCalendar(LocalDate today) {
        calendarGrid.removeAllViews(); String[] hs = {"一","二","三","四","五","六","日"};
        for (int i=0;i<7;i++){TextView h=text(hs[i],13,true);h.setGravity(Gravity.CENTER);if(i>=5)h.setTextColor(0xFF5F6368);calendarGrid.addView(h,gridParams());}
        LocalDate ws=getWorkStartDate(); int leading=displayedMonth.atDay(1).getDayOfWeek().getValue()-1, days=displayedMonth.lengthOfMonth(), cells=((leading+days+6)/7)*7; float daily=getConfiguredDailyHours();
        for(int i=0;i<cells;i++){
            int day=i-leading+1; if(day<1||day>days){calendarGrid.addView(text("",13,false),gridParams());continue;}
            LocalDate d=displayedMonth.atDay(day); boolean future=d.isAfter(today), before=ws!=null&&d.isBefore(ws), weekend=d.getDayOfWeek()==DayOfWeek.SATURDAY||d.getDayOfWeek()==DayOfWeek.SUNDAY;
            boolean holiday=!before&&isBankHoliday(d), leave=!holiday&&!before&&isLeave(d), manualRest=!holiday&&!leave&&!before&&isManualRest(d), configured=!before&&isConfiguredWorkDay(d)&&!holiday, override=!holiday&&!leave&&!manualRest&&!before&&hasOverride(d), autoRest=!holiday&&!leave&&!manualRest&&!override&&!before&&!configured;
            float normal=(future||before)?0:getBaseHoursForDate(d,daily,configured), overtime=(future||before)?0:getOvertimeHours(d), total=normal+overtime;
            LinearLayout cell=vertical();cell.setGravity(Gravity.CENTER);cell.setPadding(dp(2),dp(6),dp(2),dp(5));
            if(d.equals(today))cell.setBackground(UiStyle.roundRect(this,0xFFE8F0FE,12,0xFFB8C8FF,1));else if(leave)cell.setBackground(UiStyle.roundRect(this,0xFFFFF0F0,12,0xFFF2CACA,1));else if(overtime>0)cell.setBackground(UiStyle.roundRect(this,0xFFF1F8F3,12,0xFFCDE7D4,1));else if(override)cell.setBackground(UiStyle.roundRect(this,0xFFFFF7E8,12,0xFFF1D8A6,1));else if(holiday)cell.setBackground(UiStyle.roundRect(this,0xFFFFF2F4,12,0xFFF3CDD3,1));else if(autoRest||manualRest||weekend)cell.setBackground(UiStyle.roundRect(this,0xFFF4F6F9,12,0xFFE1E6EE,1));
            TextView dt=text(String.valueOf(day),14,d.equals(today));dt.setGravity(Gravity.CENTER);if(future||before)dt.setTextColor(0xFF9AA0A6);cell.addView(dt);
            String status=""; if(before)status="未开始";else if(!future){if(holiday)status="公共假日";else if(leave)status="请假";else if(manualRest||autoRest)status=overtime>0?"加班 "+shortHours(overtime):"休息";else if(total>0)status=shortHours(total)+(overtime>0?" 加班":"");}
            TextView st=text(status,10,leave||holiday||manualRest||override||autoRest||overtime>0);st.setGravity(Gravity.CENTER);cell.addView(st);
            if(!future&&!before)cell.setOnClickListener(v->{if(isBankHoliday(d))Toast.makeText(this,getBankHolidayName(d)+"：公共假日不计正常工时，可在其他工作日设置加班",Toast.LENGTH_SHORT).show();else showEditDayDialog(d);});
            calendarGrid.addView(cell,gridParams());
        }
    }

    private void rebuildExceptions(LocalDate end) { exceptionsContainer.removeAllViews(); LocalDate start=displayedMonth.atDay(1),ws=getWorkStartDate(); if(ws!=null&&start.isBefore(ws))start=ws; if(start.isAfter(end)){exceptionsContainer.addView(text("本月尚未开始工作",14,false));return;} addPeriodDetails(exceptionsContainer,start,end,false,false); }

    private void addPeriodDetails(LinearLayout container, LocalDate start, LocalDate end, boolean includeNormal, boolean includeAutoRest) {
        container.removeAllViews(); float daily=getConfiguredDailyHours(); DateTimeFormatter f=DateTimeFormatter.ofPattern("M月d日 E",Locale.CHINA); int count=0;
        for(LocalDate d=start;!d.isAfter(end);d=d.plusDays(1)){
            boolean holiday=isBankHoliday(d),leave=!holiday&&isLeave(d),manualRest=!holiday&&!leave&&isManualRest(d),configured=isConfiguredWorkDay(d)&&!holiday,override=!holiday&&!leave&&!manualRest&&hasOverride(d),autoRest=!holiday&&!leave&&!manualRest&&!override&&!configured;
            float base=getBaseHoursForDate(d,daily,configured),ot=getOvertimeHours(d); String line=null;
            if(holiday)line=d.format(f)+" ｜ 公共假日 ｜ "+getBankHolidayName(d)+" ｜ 0 小时";
            else if(leave)line=d.format(f)+" ｜ 请假"+(getLeaveNote(d).isEmpty()?"":" ｜ "+getLeaveNote(d))+" ｜ 0 小时";
            else if(manualRest)line=d.format(f)+" ｜ 休息 ｜ 手动修改";
            else if(override)line=d.format(f)+" ｜ 自定义工时 ｜ "+formatDurationHours(base)+(getDayTimeSummary(d).isEmpty()?"":" ｜ "+getDayTimeSummary(d));
            else if(includeAutoRest&&autoRest)line=d.format(f)+" ｜ 休息 ｜ 自动规则";
            else if(includeNormal&&base>0)line=d.format(f)+" ｜ 正常上班 ｜ "+formatDurationHours(base);
            if(ot>0){String otText="加班 "+getOvertimeTimeSummary(d)+" ｜ "+formatDurationHours(ot);line=line==null?d.format(f)+" ｜ "+otText:line+" ｜ "+otText;}
            if(line!=null){TextView row=text(line,13,holiday||leave||manualRest||override||ot>0);row.setPadding(0,dp(5),0,dp(5));container.addView(row);count++;}
        }
        if(count==0)container.addView(text(includeNormal?"这一周没有可显示的记录。":"本月无公共假日、请假、手动休息、自定义工时或加班记录。",13,false));
    }

    private void showEditDayDialog(LocalDate date) {
        LocalDate ws=getWorkStartDate(); if(ws!=null&&date.isBefore(ws))return;
        float daily=getConfiguredDailyHours(); boolean configured=isConfiguredWorkDay(date), hasOverride=hasOverride(date);
        String defaultStart=prefs.getString(START_TIME_KEY,"09:00"),defaultEnd=prefs.getString(END_TIME_KEY,"17:30"); int defaultBreak=prefs.getInt(BREAK_MINUTES_KEY,30);
        String currentStart=prefs.getString(dayStartKey(date),defaultStart),currentEnd=prefs.getString(dayEndKey(date),defaultEnd); int currentBreak=prefs.getInt(dayBreakKey(date),defaultBreak);
        boolean hasOt=hasOvertime(date); String currentOtStart=prefs.getString(overtimeStartKey(date),currentEnd),currentOtEnd=prefs.getString(overtimeEndKey(date),currentEnd);

        ScrollView sc=new ScrollView(this); LinearLayout box=vertical(); box.setPadding(dp(22),dp(8),dp(22),dp(8)); sc.addView(box);
        box.addView(text("当天状态",14,true)); RadioGroup group=new RadioGroup(this); RadioButton normal=new RadioButton(this),leave=new RadioButton(this),rest=new RadioButton(this); normal.setText("正常上班");leave.setText("请假（0 小时）");rest.setText("休息（0 小时）");group.addView(normal);group.addView(leave);group.addView(rest);box.addView(group);
        if(isLeave(date))leave.setChecked(true);else if(isManualRest(date)||(!configured&&!hasOverride))rest.setChecked(true);else normal.setChecked(true);
        TextView reasonLabel=text("请假原因 / 描述（可选）",14,false);reasonLabel.setPadding(0,dp(8),0,0);box.addView(reasonLabel);EditText reason=new EditText(this);reason.setMinLines(2);reason.setMaxLines(4);reason.setText(getLeaveNote(date));box.addView(reason);
        TextView timeTitle=text("当天上下班时间",14,true);timeTitle.setPadding(0,dp(12),0,dp(6));box.addView(timeTitle);LinearLayout tr=horizontal();Button startButton=button(currentStart),endButton=button(currentEnd);tr.addView(startButton,new LinearLayout.LayoutParams(0,dp(50),1));TextView to=text(" 至 ",14,false);to.setGravity(Gravity.CENTER);tr.addView(to,new LinearLayout.LayoutParams(dp(38),dp(50)));tr.addView(endButton,new LinearLayout.LayoutParams(0,dp(50),1));box.addView(tr);
        TextView breakLabel=text("当天休息时间（分钟）",14,false);breakLabel.setPadding(0,dp(10),0,0);box.addView(breakLabel);EditText breakInput=new EditText(this);breakInput.setSingleLine(true);breakInput.setInputType(InputType.TYPE_CLASS_NUMBER);breakInput.setText(String.valueOf(currentBreak));box.addView(breakInput);

        TextView otTitle=text("加班",14,true);otTitle.setPadding(0,dp(16),0,dp(4));box.addView(otTitle);CheckBox overtimeCheck=new CheckBox(this);overtimeCheck.setText("这一天有加班");overtimeCheck.setChecked(hasOt);box.addView(overtimeCheck);
        LinearLayout otr=horizontal();Button otStartButton=button(currentOtStart),otEndButton=button(currentOtEnd);otr.addView(otStartButton,new LinearLayout.LayoutParams(0,dp(50),1));TextView otTo=text(" 至 ",14,false);otTo.setGravity(Gravity.CENTER);otr.addView(otTo,new LinearLayout.LayoutParams(dp(38),dp(50)));otr.addView(otEndButton,new LinearLayout.LayoutParams(0,dp(50),1));box.addView(otr);
        TextView wageTitle=text("工资",14,true);wageTitle.setPadding(0,dp(16),0,dp(2));box.addView(wageTitle);
        CheckBox wageDeductCheck=new CheckBox(this);wageDeductCheck.setText("这一天需要扣工资");wageDeductCheck.setChecked(prefs.getBoolean(wageDeductKey(date),false));box.addView(wageDeductCheck);
        TextView calculated=text("",15,true);calculated.setPadding(0,dp(10),0,0);box.addView(calculated);
        final String[] ss={currentStart},se={currentEnd},os={currentOtStart},oe={currentOtEnd};
        Runnable update=()->{float base=0,ot=0;if(normal.isChecked()){Integer b=parseBreakMinutes(breakInput.getText().toString());Float h=calculateHours(ss[0],se[0],b==null?-1:b);if(h!=null)base=h;}if(overtimeCheck.isChecked()){Float h=calculateHours(os[0],oe[0],0);if(h!=null)ot=h;}calculated.setText("正常工时："+formatDurationHours(base)+"\n加班："+formatDurationHours(ot)+"\n合计："+formatDurationHours(base+ot));};
        startButton.setOnClickListener(v->showTimePicker(ss[0],x->{ss[0]=x;startButton.setText(x);update.run();})); endButton.setOnClickListener(v->showTimePicker(se[0],x->{se[0]=x;endButton.setText(x);if(!hasOt&&!overtimeCheck.isChecked()){os[0]=x;oe[0]=x;otStartButton.setText(x);otEndButton.setText(x);}update.run();}));
        otStartButton.setOnClickListener(v->showTimePicker(os[0],x->{os[0]=x;otStartButton.setText(x);update.run();}));otEndButton.setOnClickListener(v->showTimePicker(oe[0],x->{oe[0]=x;otEndButton.setText(x);update.run();}));
        Runnable controls=()->{boolean n=normal.isChecked(),l=leave.isChecked(),o=overtimeCheck.isChecked();startButton.setEnabled(n);endButton.setEnabled(n);breakInput.setEnabled(n);reason.setEnabled(l);otStartButton.setEnabled(o);otEndButton.setEnabled(o);update.run();};group.setOnCheckedChangeListener((g,id)->controls.run());overtimeCheck.setOnCheckedChangeListener((b,c)->controls.run());controls.run();

        DateTimeFormatter tf=DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE",Locale.CHINA); AlertDialog dialog=new AlertDialog.Builder(this).setTitle(date.format(tf)).setView(sc).setPositiveButton("保存",null).setNeutralButton("恢复自动",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(x->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String note=reason.getText().toString().trim();float base=0,ot=0;int br=0;if(normal.isChecked()){Integer b=parseBreakMinutes(breakInput.getText().toString());if(b==null||b<0||b>=1440){breakInput.setError("请输入 0～1439 分钟");return;}br=b;Float h=calculateHours(ss[0],se[0],br);if(h==null){Toast.makeText(this,"请检查上下班时间",Toast.LENGTH_SHORT).show();return;}base=h;}if(overtimeCheck.isChecked()){Float h=calculateHours(os[0],oe[0],0);if(h==null||h<=0){Toast.makeText(this,"请设置正确的加班开始和结束时间",Toast.LENGTH_SHORT).show();return;}ot=h;}final float fBase=base,fOt=ot;final int fBr=br;String target=leave.isChecked()?"请假":rest.isChecked()?"休息":"正常上班";String msg="日期："+date.format(tf)+"\n状态："+target+(normal.isChecked()?"\n上班："+ss[0]+"\n下班："+se[0]+"\n休息："+fBr+" 分钟":"")+(overtimeCheck.isChecked()?"\n加班："+os[0]+"–"+oe[0]+"（"+formatDurationHours(fOt)+"）":"")+"\n总工时："+formatDurationHours(fBase+fOt)+(wageDeductCheck.isChecked()?"\n工资：本日扣工资":"");
            new AlertDialog.Builder(this).setTitle("确认修改？").setMessage(msg).setNegativeButton("取消",null).setPositiveButton("确认修改",(d,w)->{SharedPreferences.Editor e=prefs.edit();if(leave.isChecked()){e.putBoolean(leaveKey(date),true).remove(restKey(date)).remove(overrideKey(date)).remove(dayStartKey(date)).remove(dayEndKey(date)).remove(dayBreakKey(date));if(note.isEmpty())e.remove(leaveNoteKey(date));else e.putString(leaveNoteKey(date),note);}else if(rest.isChecked()){e.putBoolean(restKey(date),true).remove(leaveKey(date)).remove(leaveNoteKey(date)).remove(overrideKey(date)).remove(dayStartKey(date)).remove(dayEndKey(date)).remove(dayBreakKey(date));}else{e.remove(leaveKey(date)).remove(leaveNoteKey(date)).remove(restKey(date));boolean same=configured&&ss[0].equals(defaultStart)&&se[0].equals(defaultEnd)&&fBr==defaultBreak&&Math.abs(fBase-daily)<0.0001f;if(same)e.remove(overrideKey(date)).remove(dayStartKey(date)).remove(dayEndKey(date)).remove(dayBreakKey(date));else e.putFloat(overrideKey(date),fBase).putString(dayStartKey(date),ss[0]).putString(dayEndKey(date),se[0]).putInt(dayBreakKey(date),fBr);}if(overtimeCheck.isChecked())e.putString(overtimeStartKey(date),os[0]).putString(overtimeEndKey(date),oe[0]);else e.remove(overtimeStartKey(date)).remove(overtimeEndKey(date));if(wageDeductCheck.isChecked())e.putBoolean(wageDeductKey(date),true);else e.remove(wageDeductKey(date));e.apply();WorkAlarmManager.forceSync(this);dialog.dismiss();refreshAll();Toast.makeText(this,"修改已保存",Toast.LENGTH_SHORT).show();}).show();});
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->new AlertDialog.Builder(this).setTitle("确认恢复自动？").setMessage("将清除当天手动状态、上下班时间和加班时间，重新按自动规则计算。").setNegativeButton("取消",null).setPositiveButton("确认恢复",(d,w)->{prefs.edit().remove(overrideKey(date)).remove(dayStartKey(date)).remove(dayEndKey(date)).remove(dayBreakKey(date)).remove(overtimeStartKey(date)).remove(overtimeEndKey(date)).remove(leaveKey(date)).remove(leaveNoteKey(date)).remove(restKey(date)).remove(wageDeductKey(date)).apply();WorkAlarmManager.forceSync(this);dialog.dismiss();refreshAll();}).show());});
        dialog.show();
    }

    private void chooseWorkWeek(){LocalDate initial=displayedWeekStart,today=LocalDate.now(),ws=getWorkStartDate();DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->{LocalDate picked=LocalDate.of(y,m+1,day);displayedWeekStart=mondayOf(picked);refreshWeek();},initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth());d.getDatePicker().setMaxDate(System.currentTimeMillis());if(ws!=null)d.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());d.show();}
    private void chooseWorkMonth(){LocalDate ws=getWorkStartDate();int minYear=ws==null?Math.max(2000,displayedMonth.getYear()-20):ws.getYear(),maxYear=YearMonth.now().getYear();LinearLayout box=horizontal();box.setPadding(dp(24),dp(12),dp(24),dp(4));box.setGravity(Gravity.CENTER);NumberPicker yp=new NumberPicker(this);yp.setMinValue(minYear);yp.setMaxValue(maxYear);yp.setValue(Math.max(minYear,Math.min(maxYear,displayedMonth.getYear())));box.addView(yp,new LinearLayout.LayoutParams(0,-2,1f));NumberPicker mp=new NumberPicker(this);mp.setMinValue(1);mp.setMaxValue(12);mp.setDisplayedValues(new String[]{"1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"});mp.setValue(displayedMonth.getMonthValue());box.addView(mp,new LinearLayout.LayoutParams(0,-2,1f));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("选择月份").setView(box).setNegativeButton("取消",null).setPositiveButton("确定",null).create();dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{YearMonth picked=YearMonth.of(yp.getValue(),mp.getValue()),now=YearMonth.now(),first=ws==null?null:YearMonth.from(ws);if(picked.isAfter(now)){Toast.makeText(this,"不能选择未来月份",Toast.LENGTH_SHORT).show();return;}if(first!=null&&picked.isBefore(first)){Toast.makeText(this,"不能早于工作开始月份",Toast.LENGTH_SHORT).show();return;}displayedMonth=picked;refreshMonth();dialog.dismiss();}));dialog.show();}
    private void showDatePicker(boolean startPicker){LocalDate initial=startPicker?rangeStart:rangeEnd,today=LocalDate.now(),ws=getWorkStartDate();if(ws!=null&&initial.isBefore(ws))initial=ws;DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->{LocalDate s=LocalDate.of(y,m+1,day);if(s.isAfter(today))s=today;if(ws!=null&&s.isBefore(ws))s=ws;if(startPicker){rangeStart=s;if(rangeEnd.isBefore(rangeStart))rangeEnd=rangeStart;}else{rangeEnd=s;if(rangeStart.isAfter(rangeEnd))rangeStart=rangeEnd;}updateRangeButtons();calculateRange();},initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth());d.getDatePicker().setMaxDate(System.currentTimeMillis());if(ws!=null)d.getDatePicker().setMinDate(ws.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());d.show();}
    private void updateRangeButtons(){DateTimeFormatter f=DateTimeFormatter.ofPattern("yyyy-MM-dd");rangeStartButton.setText("开始\n"+rangeStart.format(f));rangeEndButton.setText("结束\n"+rangeEnd.format(f));}
    private void calculateRange(){if(rangeDetailsContainer==null)return;rangeDetailsContainer.removeAllViews();LocalDate today=LocalDate.now(),ws=getWorkStartDate(),start=rangeStart,end=rangeEnd.isAfter(today)?today:rangeEnd;if(ws!=null&&start.isBefore(ws))start=ws;if(start.isAfter(end)){rangeSummaryText.setText("开始日期不能晚于结束日期");return;}Stats s=collectStats(start,end);rangeSummaryText.setText(start+" 至 "+end+"\n总工时："+formatDurationHours(s.totalHours)+"（加班 "+formatDurationHours(s.overtimeHours)+"）\n"+StatusStatsFormatter.format(s.workDays,s.leaveDays,s.holidayDays,s.restDays));addPeriodDetails(rangeDetailsContainer,start,end,false,true);}
    private Stats collectStats(LocalDate start,LocalDate end){Stats s=new Stats();if(start==null||end==null||start.isAfter(end))return s;float daily=getConfiguredDailyHours();LocalDate ws=getWorkStartDate();for(LocalDate d=start;!d.isAfter(end);d=d.plusDays(1)){if(ws!=null&&d.isBefore(ws))continue;boolean h=isBankHoliday(d),l=!h&&isLeave(d),r=!h&&!l&&isManualRest(d),c=isConfiguredWorkDay(d)&&!h,o=!h&&!l&&!r&&hasOverride(d),a=!h&&!l&&!r&&!o&&!c;float base=getBaseHoursForDate(d,daily,c),ot=getOvertimeHours(d);s.totalHours+=base+ot;s.overtimeHours+=ot;if(base+ot>0)s.workDays++;if(l)s.leaveDays++;if(h)s.holidayDays++;if(r||a)s.restDays++;if(o)s.overrideDays++;}return s;}
    private static class Stats{float totalHours,overtimeHours;int workDays,leaveDays,holidayDays,restDays,overrideDays;}

    private interface TimeSelectedListener{void onSelected(String value);} private void showTimePicker(String initial,TimeSelectedListener l){LocalTime t=parseTime(initial);if(t==null)t=LocalTime.of(9,0);new TimePickerDialog(this,(v,h,m)->l.onSelected(String.format(Locale.US,"%02d:%02d",h,m)),t.getHour(),t.getMinute(),true).show();}
    private Integer parseBreakMinutes(String raw){try{return Integer.parseInt(raw.trim());}catch(Exception e){return null;}}
    private Float calculateHours(String s,String e,int br){if(br<0||br>=1440)return null;LocalTime a=parseTime(s),b=parseTime(e);if(a==null||b==null)return null;long m=Duration.between(a,b).toMinutes();if(m<=0)m+=1440;long w=m-br;return w<0?null:w/60f;}
    private boolean hasOvertime(LocalDate d){return prefs.contains(overtimeStartKey(d))&&prefs.contains(overtimeEndKey(d));}
    private float getOvertimeHours(LocalDate d){if(!hasOvertime(d))return 0f;Float h=calculateHours(prefs.getString(overtimeStartKey(d),""),prefs.getString(overtimeEndKey(d),""),0);return h==null?0f:h;}
    private String getOvertimeTimeSummary(LocalDate d){return hasOvertime(d)?prefs.getString(overtimeStartKey(d),"")+"–"+prefs.getString(overtimeEndKey(d),""):"";}
    private String getDayTimeSummary(LocalDate d){if(!prefs.contains(dayStartKey(d))||!prefs.contains(dayEndKey(d)))return "";return prefs.getString(dayStartKey(d),"")+"–"+prefs.getString(dayEndKey(d),"")+" / 休息"+prefs.getInt(dayBreakKey(d),0)+"分钟";}
    private boolean isConfiguredWorkDay(LocalDate d){
        if("monthly".equals(prefs.getString(REST_RULE_MODE_KEY,"weekly"))) return !getMonthlyRestDays().contains(d.getDayOfMonth());
        int i=d.getDayOfWeek().getValue()-1; return prefs.getBoolean("day_"+i,i<5);
    }
    private Set<Integer> getMonthlyRestDays(){Set<Integer>s=new HashSet<>();String raw=prefs.getString(MONTHLY_REST_DAYS_KEY,"");if(raw==null||raw.trim().isEmpty())return s;for(String p:raw.replace('，',',').split(",")){try{int v=Integer.parseInt(p.trim());if(v>=1&&v<=31)s.add(v);}catch(Exception ignored){}}return s;}
    private float getConfiguredDailyHours(){LocalTime s=parseTime(prefs.getString(START_TIME_KEY,"09:00")),e=parseTime(prefs.getString(END_TIME_KEY,"17:30"));int br=prefs.getInt(BREAK_MINUTES_KEY,30);if(s==null||e==null)return prefs.getFloat("daily_hours",8f);long m=Duration.between(s,e).toMinutes();if(m<=0)m+=1440;return Math.max(0,m-br)/60f;}
    private LocalTime parseTime(String r){try{return LocalTime.parse(r,DateTimeFormatter.ofPattern("HH:mm"));}catch(Exception e){return null;}}
    private LocalDate getWorkStartDate(){String s=prefs.getString(WORK_START_DATE_KEY,"");try{return s==null||s.isEmpty()?null:LocalDate.parse(s);}catch(Exception e){return null;}}
    private LocalDate mondayOf(LocalDate d){return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));}
    private float getBaseHoursForDate(LocalDate d,float daily,boolean configured){LocalDate ws=getWorkStartDate();if(ws!=null&&d.isBefore(ws))return 0;if(isBankHoliday(d)||isLeave(d)||isManualRest(d))return 0;return prefs.getFloat(overrideKey(d),configured?daily:0);}
    private boolean hasOverride(LocalDate d){return prefs.contains(overrideKey(d));} private boolean isLeave(LocalDate d){return prefs.getBoolean(leaveKey(d),false);} private boolean isManualRest(LocalDate d){return prefs.getBoolean(restKey(d),false);} private String getLeaveNote(LocalDate d){return prefs.getString(leaveNoteKey(d),"");}
    private String overrideKey(LocalDate d){return OVERRIDE_PREFIX+d;} private String dayStartKey(LocalDate d){return DAY_START_PREFIX+d;} private String dayEndKey(LocalDate d){return DAY_END_PREFIX+d;} private String dayBreakKey(LocalDate d){return DAY_BREAK_PREFIX+d;} private String overtimeStartKey(LocalDate d){return OVERTIME_START_PREFIX+d;} private String overtimeEndKey(LocalDate d){return OVERTIME_END_PREFIX+d;} private String leaveKey(LocalDate d){return LEAVE_PREFIX+d;} private String wageDeductKey(LocalDate d){return WAGE_DEDUCT_PREFIX+d;} private String leaveNoteKey(LocalDate d){return LEAVE_NOTE_PREFIX+d;} private String restKey(LocalDate d){return REST_PREFIX+d;}
    private boolean isBankHoliday(LocalDate d){return HolidayCalendar.isHoliday(prefs,d);}
    private String getBankHolidayName(LocalDate d){return HolidayCalendar.getHolidayName(prefs,d);}
    private LocalDate observedDate(LocalDate d){if(d.getDayOfWeek()==DayOfWeek.SATURDAY)return d.plusDays(2);if(d.getDayOfWeek()==DayOfWeek.SUNDAY)return d.plusDays(1);return d;}
    private LocalDate easterSunday(int y){int a=y%19,b=y/100,c=y%100,d=b/4,e=b%4,f=(b+8)/25,g=(b-f+1)/3,h=(19*a+b-d-g+15)%30,i=c/4,k=c%4,l=(32+2*e+2*i-h-k)%7,m=(a+11*h+22*l)/451,mo=(h+l-7*m+114)/31,da=((h+l-7*m+114)%31)+1;return LocalDate.of(y,mo,da);}
    private String formatDurationHours(float h){int m=Math.round(h*60),hh=m/60,mm=m%60;if(mm==0)return hh+" 小时";if(hh==0)return mm+" 分钟";return hh+" 小时 "+mm+" 分钟";} private String shortHours(float h){int m=Math.round(h*60);return m%60==0?(m/60)+"h":String.format(Locale.US,"%.1fh",h);}
    private TextView statLineText(){TextView v=text("",15,true);v.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);v.setSingleLine(true);v.setPadding(dp(4),dp(2),dp(4),dp(2));return v;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;} private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;} private LinearLayout card(){LinearLayout l=vertical();l.setPadding(dp(16),dp(15),dp(16),dp(15));UiStyle.card(this,l);return l;} private Button button(String s){Button b=new Button(this);b.setText(s);UiStyle.button(this,b,false);return b;} private TextView text(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(UiStyle.TEXT);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private GridLayout.LayoutParams gridParams(){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(58);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(1),dp(1),dp(1),dp(1));return p;} private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
