from pathlib import Path

# MainActivity
p = Path('app/src/main/java/com/example/workhours/MainActivity.java')
s = p.read_text()
s = s.replace('        root.setPadding(dp(18), dp(22), dp(18), dp(28));\n        scroll.addView(root);',
              '        root.setPadding(dp(18), dp(22), dp(18), dp(28));\n        scroll.addView(root);\n        UiStyle.page(scroll);')
s = s.replace('Button settings = button("设置");', 'Button settings = button("设置");\n        UiStyle.button(this, settings, true);')
s = s.replace('Button calc = button("统计这段时间"); calc.setOnClickListener', 'Button calc = button("统计这段时间"); UiStyle.button(this, calc, true); calc.setOnClickListener')
s = s.replace('private LinearLayout card(){LinearLayout l=vertical();l.setPadding(dp(14),dp(12),dp(14),dp(12));l.setBackgroundColor(0xFFF8F9FA);return l;}',
              'private LinearLayout card(){LinearLayout l=vertical();l.setPadding(dp(16),dp(15),dp(16),dp(15));UiStyle.card(this,l);return l;}')
s = s.replace('private Button button(String s){Button b=new Button(this);b.setText(s);return b;}',
              'private Button button(String s){Button b=new Button(this);b.setText(s);UiStyle.button(this,b,false);return b;}')
s = s.replace('private TextView text(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(0xFF202124);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}',
              'private TextView text(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(UiStyle.TEXT);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}')
s = s.replace('if(d.equals(today))cell.setBackgroundColor(0xFFE8F0FE);else if(leave)cell.setBackgroundColor(0xFFEAF2FF);else if(overtime>0)cell.setBackgroundColor(0xFFE8F5E9);else if(override)cell.setBackgroundColor(0xFFFFF4E5);else if(holiday)cell.setBackgroundColor(0xFFFFEBEE);else if(autoRest||manualRest||weekend)cell.setBackgroundColor(0xFFF1F3F4);',
              'if(d.equals(today))cell.setBackground(UiStyle.roundRect(this,0xFFE8F0FE,12,0xFFB8C8FF,1));else if(leave)cell.setBackground(UiStyle.roundRect(this,0xFFFFF0F0,12,0xFFF2CACA,1));else if(overtime>0)cell.setBackground(UiStyle.roundRect(this,0xFFF1F8F3,12,0xFFCDE7D4,1));else if(override)cell.setBackground(UiStyle.roundRect(this,0xFFFFF7E8,12,0xFFF1D8A6,1));else if(holiday)cell.setBackground(UiStyle.roundRect(this,0xFFFFF2F4,12,0xFFF3CDD3,1));else if(autoRest||manualRest||weekend)cell.setBackground(UiStyle.roundRect(this,0xFFF4F6F9,12,0xFFE1E6EE,1));')
p.write_text(s)

# WageActivity
p = Path('app/src/main/java/com/example/workhours/WageActivity.java')
s = p.read_text()
s = s.replace('        root.setPadding(dp(18), dp(22), dp(18), dp(28));\n        scroll.addView(root);',
              '        root.setPadding(dp(18), dp(22), dp(18), dp(28));\n        scroll.addView(root);\n        UiStyle.page(scroll);')
s = s.replace('Button save = button("保存工资设置");', 'Button save = button("保存工资设置");\n        UiStyle.button(this, save, true);')
s = s.replace('Button saveDeduction = button("保存这一天的扣工资设置");', 'Button saveDeduction = button("保存这一天的扣工资设置");\n        UiStyle.button(this, saveDeduction, true);')
s = s.replace('        e.setPadding(dp(12), dp(8), dp(12), dp(8));\n        return e;',
              '        UiStyle.input(this, e);\n        return e;')
s = s.replace('        l.setPadding(dp(14), dp(12), dp(14), dp(12));\n        l.setBackgroundColor(0xFFF8F9FA);',
              '        l.setPadding(dp(16), dp(15), dp(16), dp(15));\n        UiStyle.card(this, l);')
s = s.replace('private Button button(String s) { Button b = new Button(this); b.setText(s); return b; }',
              'private Button button(String s) { Button b = new Button(this); b.setText(s); UiStyle.button(this,b,false); return b; }')
s = s.replace('TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(0xFF202124);',
              'TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(UiStyle.TEXT);')
p.write_text(s)

# SettingsActivity
p = Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s = p.read_text()
s = s.replace('        root.setPadding(dp(20), dp(24), dp(20), dp(28));\n        scroll.addView(root);',
              '        root.setPadding(dp(20), dp(24), dp(20), dp(28));\n        scroll.addView(root);\n        UiStyle.page(scroll);')
s = s.replace('        back.setTextSize(24);', '        back.setTextSize(24);\n        UiStyle.navButton(this, back);')
s = s.replace('        workStartDateButton = new Button(this);', '        workStartDateButton = new Button(this);\n        UiStyle.button(this, workStartDateButton, false);')
s = s.replace('        Button clearDate = new Button(this);\n        clearDate.setText("清除");', '        Button clearDate = new Button(this);\n        UiStyle.button(this, clearDate, false);\n        clearDate.setText("清除");')
s = s.replace('        Button preview = new Button(this);\n        preview.setText("计算每天工时");', '        Button preview = new Button(this);\n        UiStyle.button(this, preview, false);\n        preview.setText("计算每天工时");')
s = s.replace('        Button save = new Button(this);\n        save.setText("保存设置");', '        Button save = new Button(this);\n        UiStyle.button(this, save, true);\n        save.setText("保存设置");')
s = s.replace('        Button export = new Button(this);\n        export.setText("导出全部数据");', '        Button export = new Button(this);\n        UiStyle.button(this, export, false);\n        export.setText("导出全部数据");')
s = s.replace('        Button importButton = new Button(this);\n        importButton.setText("导入 / 恢复数据");', '        Button importButton = new Button(this);\n        UiStyle.button(this, importButton, false);\n        importButton.setText("导入 / 恢复数据");')
s = s.replace('        view.setPadding(dp(12), dp(8), dp(12), dp(8));\n        return view;',
              '        UiStyle.input(this, view);\n        return view;')
s = s.replace('        view.setTextColor(0xFF202124);', '        view.setTextColor(UiStyle.TEXT);')
p.write_text(s)
