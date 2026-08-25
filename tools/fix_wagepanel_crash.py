from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/WagePanel.java')
s=p.read_text()
old='''        previousMonthButton.setEnabled(first == null || displayedMonth.isAfter(first));
        nextMonthButton.setEnabled(displayedMonth.isBefore(now));
        monthTitle.setText(displayedMonth.getYear() + "年" + displayedMonth.getMonthValue() + "月\\n点击选择月份");
'''
if old not in s:
    raise SystemExit('old month navigation refs not found')
s=s.replace(old,'',1)
p.write_text(s)
