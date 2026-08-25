from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/WagePanel.java')
s=p.read_text().replace('UiStyle.input(this, e);','UiStyle.input(host, e);')
p.write_text(s)
