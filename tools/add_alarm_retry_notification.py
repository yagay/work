from pathlib import Path

# Receiver: automatic success stays silent; only failure posts actionable notification.
p=Path('app/src/main/java/com/example/workhours/WorkAlarmRescheduleReceiver.java')
s=p.read_text()
s=s.replace('''            boolean success = WorkAlarmManager.forceSyncNextWeek(context);\n            WorkAlarmNotification.notifySyncResult(context, success, true);''','''            boolean success = WorkAlarmManager.forceSyncNextWeek(context);\n            if (!success) WorkAlarmNotification.notifyRetryRequired(context);''',1)
p.write_text(s)

# Notification: add actionable retry notification pointing at a foreground Activity.
p=Path('app/src/main/java/com/example/workhours/WorkAlarmNotification.java')
s=p.read_text()
anchor='''    public static void notifySyncResult(Context context, boolean success, boolean automatic) {\n'''
method='''    public static void notifyRetryRequired(Context context) {\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU\n                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)\n                != PackageManager.PERMISSION_GRANTED) {\n            return;\n        }\n\n        NotificationManager manager =\n                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);\n        if (manager == null) return;\n\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {\n            NotificationChannel channel = new NotificationChannel(\n                    CHANNEL_ID,\n                    "上班闹钟同步",\n                    NotificationManager.IMPORTANCE_DEFAULT);\n            channel.setDescription("后台自动同步失败时提醒点击完成同步");\n            manager.createNotificationChannel(channel);\n        }\n\n        Intent retry = new Intent(context, WorkAlarmRetryActivity.class)\n                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);\n        PendingIntent retryIntent = PendingIntent.getActivity(\n                context, 7203, retry,\n                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);\n\n        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O\n                ? new Notification.Builder(context, CHANNEL_ID)\n                : new Notification.Builder(context);\n        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)\n                .setContentTitle("下周闹钟需要确认同步")\n                .setContentText("后台自动同步被系统阻止，点一下完成同步。")\n                .setContentIntent(retryIntent)\n                .setAutoCancel(true)\n                .setCategory(Notification.CATEGORY_REMINDER)\n                .setVisibility(Notification.VISIBILITY_PUBLIC);\n\n        manager.notify(NOTIFICATION_ID, builder.build());\n    }\n\n'''
if anchor not in s: raise SystemExit('notification anchor missing')
s=s.replace(anchor,method+anchor,1)
p.write_text(s)

# Foreground retry Activity: user click allows the AlarmClock Activity launch path.
p=Path('app/src/main/java/com/example/workhours/WorkAlarmRetryActivity.java')
p.write_text('''package com.example.workhours;\n\nimport android.app.Activity;\nimport android.app.NotificationManager;\nimport android.os.Bundle;\nimport android.widget.Toast;\n\npublic class WorkAlarmRetryActivity extends Activity {\n    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n        super.onCreate(savedInstanceState);\n        boolean success = WorkAlarmManager.forceSyncNextWeek(this);\n        if (success) {\n            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);\n            if (manager != null) manager.cancel(7201);\n            Toast.makeText(this, "下周上班闹钟已同步", Toast.LENGTH_SHORT).show();\n        } else {\n            Toast.makeText(this, "仍无法写入系统时钟，请打开 App 检查设置", Toast.LENGTH_LONG).show();\n            WorkAlarmNotification.notifyRetryRequired(this);\n        }\n        finish();\n    }\n}\n''')

# Manifest registration with translucent/no-title theme to avoid opening the full app UI.
p=Path('app/src/main/AndroidManifest.xml')
s=p.read_text()
anchor='''        <activity\n            android:name=".WageActivity"'''
entry='''        <activity\n            android:name=".WorkAlarmRetryActivity"\n            android:exported="false"\n            android:excludeFromRecents="true"\n            android:noHistory="true"\n            android:theme="@android:style/Theme.Translucent.NoTitleBar" />\n\n'''
if anchor not in s: raise SystemExit('manifest anchor missing')
s=s.replace(anchor,entry+anchor,1)
p.write_text(s)
