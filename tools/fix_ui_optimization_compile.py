from pathlib import Path
p=Path('app/src/main/java/com/example/workhours/SettingsActivity.java')
s=p.read_text()
if 'import android.widget.GridLayout;' not in s:
    s=s.replace('import android.widget.EditText;','import android.widget.EditText;\nimport android.widget.GridLayout;')
s=s.replace('previewText.setText("正常工作日自动计入：" + formatDurationHours(value));','previewText.setText(formatDurationHours(value));')
p.write_text(s)
