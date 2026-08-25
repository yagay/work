from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/MainActivity.java')
s=p.read_text()
s=s.replace('''        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(58));''','''        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(76));''',1)
s=s.replace('''        monthTitle=text("",18,true); monthTitle.setGravity(Gravity.CENTER); monthTitle.setPadding(dp(8),dp(10),dp(8),dp(10)); monthTitle.setOnClickListener(v->chooseWorkMonth());''','''        monthTitle=text("",18,true); monthTitle.setGravity(Gravity.CENTER); monthTitle.setPadding(dp(8),dp(6),dp(8),dp(6)); monthTitle.setMinHeight(dp(68)); monthTitle.setOnClickListener(v->chooseWorkMonth());''',1)
p.write_text(s)
