package com.clicksmith.salatreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.Calendar;

public final class AlarmScheduler {
    private static final String ACTION = "com.clicksmith.salatreminder.PRAYER_ALARM";
    private AlarmScheduler() {}
    public static void scheduleNext(Context c) {
        cancel(c);
        ZoneId zone = ZoneId.of("Asia/Makassar");
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime best = null;
        String bestKey = null;
        for (String key : ScheduleStore.keys()) {
            String value = ScheduleStore.get(c, key);
            if (value == null || value.isEmpty()) continue;
            try {
                LocalTime t = LocalTime.parse(value);
                LocalDateTime candidate = now.toLocalDate().atTime(t);
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
                if (best == null || candidate.isBefore(best)) { best = candidate; bestKey = key; }
            } catch (Exception ignored) {}
        }
        if (best == null) return;
        Intent i = new Intent(c, PrayerAlarmReceiver.class);
        i.setAction(ACTION);
        i.putExtra("prayer_key", bestKey);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1001, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            long trigger = best.atZone(zone).toInstant().toEpochMilli();
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }
    public static void cancel(Context c) {
        Intent i = new Intent(c, PrayerAlarmReceiver.class);
        i.setAction(ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1001, i,
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }
}
