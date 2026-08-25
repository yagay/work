from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/WageActivity.java')
s=p.read_text()
if 'import android.content.Intent;' not in s:
    s=s.replace('import android.content.SharedPreferences;','import android.content.Intent;\nimport android.content.SharedPreferences;')
p.write_text(s)
