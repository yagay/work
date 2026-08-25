from pathlib import Path
import re

root = Path('app/src/main/java/com/example/workhours')
wage_path = root/'WageActivity.java'
main_path = root/'MainActivity.java'
ws = wage_path.read_text()

# Build WagePanel from WageActivity source, keeping the established wage calculation code.
# Imports/class shell.
ws = ws.replace('import android.content.Intent;\n', '')
ws = ws.replace('import android.os.Bundle;\n', '')
ws = ws.replace('import android.view.WindowInsets;\n', '')
ws = ws.replace('public class WageActivity extends Activity {', 'public class WagePanel extends LinearLayout {\n\n    private final Activity host;')

# Remove Activity lifecycle and replace with panel constructor.
start = ws.index('    @Override\n    protected void onCreate(Bundle savedInstanceState) {')
build = ws.index('    private ScrollView buildUi() {')
prefix = ws[:start]
rest = ws[build:]
ctor = '''    public WagePanel(Activity host) {\n        super(host);\n        this.host = host;\n        setOrientation(VERTICAL);\n        prefs = host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);\n        LocalDate today = LocalDate.now();\n        displayedMonth = YearMonth.from(today);\n        displayedWeekStart = mondayOf(today);\n        selectedDate = today;\n        buildUi();\n        loadSettings();\n        refreshAll();\n    }\n\n    public void refresh() {\n        if (monthSummary != null) refreshAll();\n    }\n\n'''
ws = prefix + ctor + rest

# Convert buildUi to populate this panel and remove its old header/tabs.
ws = ws.replace('    private ScrollView buildUi() {\n        ScrollView scroll = new ScrollView(this);\n        LinearLayout root = vertical();\n        root.setPadding(dp(18), dp(22), dp(18), dp(28));\n        scroll.addView(root);\n        UiStyle.page(scroll);\n',
'''    private void buildUi() {\n        LinearLayout root = this;\n        root.setPadding(0, dp(8), 0, dp(28));\n''', 1)
header_start = ws.index('        LinearLayout top = horizontal();')
header_end = ws.index('        TextView settingsTitle = text("工资设置", 19, true);')
ws = ws[:header_start] + ws[header_end:]
ws = ws.replace('        return scroll;\n    }', '    }', 1)

# Context-sensitive constructors/helpers now use host Activity.
ws = re.sub(r'new (DatePickerDialog|TextView|Button|EditText|LinearLayout|RadioButton|RadioGroup|ScrollView)\(this', r'new \1(host', ws)
ws = ws.replace('Toast.makeText(this,', 'Toast.makeText(host,')
ws = ws.replace('UiStyle.button(this,', 'UiStyle.button(host,')
ws = ws.replace('UiStyle.card(this,', 'UiStyle.card(host,')
ws = ws.replace('getSharedPreferences(', 'host.getSharedPreferences(')
# Constructor got double host after generic replacement in case helper touched it.
ws = ws.replace('prefs = host.host.getSharedPreferences', 'prefs = host.getSharedPreferences')

# Methods inherited from View can stay as-is; Activity-specific calls need host.
ws = ws.replace('getWindow().', 'host.getWindow().')

# Remove safe-inset helper if present; not needed inside parent ScrollView.
m = re.search(r'\n    private void applySafeInsets\(.*?\n    }\n', ws, flags=re.S)
if m:
    ws = ws[:m.start()] + '\n' + ws[m.end():]

(root/'WagePanel.java').write_text(ws)

# Refactor MainActivity into in-page tab switching.
ms = main_path.read_text()
ms = ms.replace('    private LinearLayout weekSectionContainer, rangeSectionContainer;\n',
'''    private LinearLayout weekSectionContainer, rangeSectionContainer;\n    private LinearLayout workContent, wageContent;\n    private Button workTabButton, wageTabButton;\n    private WagePanel wagePanel;\n''')

old_resume = '''        refreshAll();\n    }\n\n    private ScrollView buildUi() {'''
new_resume = '''        refreshAll();\n        if (wagePanel != null) wagePanel.refresh();\n    }\n\n    private ScrollView buildUi() {'''
ms = ms.replace(old_resume, new_resume, 1)

old_tabs = '''        LinearLayout quickSwitch = horizontal();\n        LinearLayout.LayoutParams quickSwitchParams = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));\n        quickSwitchParams.topMargin = dp(14);\n        root.addView(quickSwitch, quickSwitchParams);\n        Button workTab = button("工时统计");\n        UiStyle.button(this, workTab, true);\n        workTab.setEnabled(false);\n        quickSwitch.addView(workTab, new LinearLayout.LayoutParams(0, dp(48), 1f));\n        Button wageTab = button("工资统计");\n        UiStyle.button(this, wageTab, false);\n        wageTab.setOnClickListener(v -> {\n            Intent intent = new Intent(this, WageActivity.class);\n            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);\n            startActivity(intent);\n        });\n        LinearLayout.LayoutParams wageTabParams = new LinearLayout.LayoutParams(0, dp(48), 1f);\n        wageTabParams.leftMargin = dp(8);\n        quickSwitch.addView(wageTab, wageTabParams);\n\n        buildMonthSection(root);\n'''
new_tabs = '''        LinearLayout quickSwitch = horizontal();\n        LinearLayout.LayoutParams quickSwitchParams = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));\n        quickSwitchParams.topMargin = dp(14);\n        root.addView(quickSwitch, quickSwitchParams);\n        workTabButton = button("工时统计");\n        wageTabButton = button("工资统计");\n        quickSwitch.addView(workTabButton, new LinearLayout.LayoutParams(0, dp(48), 1f));\n        LinearLayout.LayoutParams wageTabParams = new LinearLayout.LayoutParams(0, dp(48), 1f);\n        wageTabParams.leftMargin = dp(8);\n        quickSwitch.addView(wageTabButton, wageTabParams);\n\n        workContent = vertical();\n        wageContent = vertical();\n        root.addView(workContent);\n        root.addView(wageContent);\n        wagePanel = new WagePanel(this);\n        wageContent.addView(wagePanel, new LinearLayout.LayoutParams(-1, -2));\n        wageContent.setVisibility(View.GONE);\n        workTabButton.setOnClickListener(v -> showStatsTab(false));\n        wageTabButton.setOnClickListener(v -> showStatsTab(true));\n        showStatsTab(false);\n\n        buildMonthSection(workContent);\n'''
if old_tabs not in ms:
    raise SystemExit('Main tabs anchor not found')
ms = ms.replace(old_tabs, new_tabs, 1)

# Route remaining main-page sections to workContent until return scroll.
segment_start = ms.index('        TextView statsTitle = text("更多统计", 19, true);')
segment_end = ms.index('        return scroll;', segment_start)
seg = ms[segment_start:segment_end].replace('root.addView(', 'workContent.addView(')
# build helper calls that still target root
seg = seg.replace('buildWeekSection(weekSectionContainer);', 'buildWeekSection(weekSectionContainer);')
ms = ms[:segment_start] + seg + ms[segment_end:]

# Add switch helper before buildMonthSection.
anchor = '    private void buildMonthSection(LinearLayout root) {'
helper = '''    private void showStatsTab(boolean wage) {\n        if (workContent == null || wageContent == null) return;\n        workContent.setVisibility(wage ? View.GONE : View.VISIBLE);\n        wageContent.setVisibility(wage ? View.VISIBLE : View.GONE);\n        UiStyle.button(this, workTabButton, !wage);\n        UiStyle.button(this, wageTabButton, wage);\n        workTabButton.setEnabled(wage);\n        wageTabButton.setEnabled(!wage);\n        if (wage && wagePanel != null) wagePanel.refresh();\n    }\n\n'''
ms = ms.replace(anchor, helper + anchor, 1)
main_path.write_text(ms)
