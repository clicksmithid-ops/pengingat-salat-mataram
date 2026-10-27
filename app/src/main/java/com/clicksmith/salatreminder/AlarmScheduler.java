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
        
        // Check for Sajadah-cached times first
        SharedPreferences prefs = c.getSharedPreferences("schedule", Context.MODE_PRIVATE);
        boolean useSajadah = false;
        for (String key : ScheduleStore.keys(c)) {
            if (key != null && key.startsWith("sajadah_") && ScheduleStore.get(c, key) != null && !ScheduleStore.get(c, key).isEmpty()) {
                useSajadah = true;
                break;
            }
        }
        
        // Try to get schedule from Sajadah for today first
        String todayKeyFajr = "sajadah_fajr_" + java.time.LocalDate.now(java.time.ZoneId.of("Asia/Makassar")).toString();
        String todayKeyDhuhr = "sajadah_dhuhr_" + java.time.LocalDate.now(java.time.ZoneId.of("Asia/Makassar")).toString();
        String todayKeyAsr = "sajadah_asr_" + java.time.LocalDate.now(java.time.ZoneId.of("Asia/Makassar")).toString();
        String todayKeyMaghrib = "sajadah_maghrib_" + java.time.LocalDate.now(java.time.ZoneId.of("Asia/Makassar")).toString();
        String todayKeyIsha = "sajadah_isha_" + java.time.LocalDate.now(java.time.ZoneId.of("Asia/Makassar")).toString();
        
        // Check if today's Sajadah data exists and is valid
        SharedPreferences prefs = c.getSharedPreferences("schedule", Context.MODE_PRIVATE);
        boolean hasTodaySajadah = prefs.contains(todayKeyFajr) && !prefs.getString(todayKeyFajr, "").isEmpty()
            || prefs.contains(todayKeyDhuhr) && !prefs.getString(todayKeyDhuhr, "").isEmpty()
            || prefs.contains(todayKeyAsr) && !prefs.getString(todayKeyAsr, "").isEmpty()
            || prefs.contains(todayKeyMaghrib) && !prefs.getString(todayKeyMaghrib, "").isEmpty()
            || prefs.contains(todayKeyIsha) && !prefs.getString(todayKeyIsha, "").isEmpty();
        
        if (hasTodaySajadah) {
            // Use Sajadah times for today
            scheduleUsingSajadahTimes(c, todayKeyFajr, todayKeyDhuhr, todayKeyAsr, todayKeyMaghrib, todayKeyIsha);
        } else {
            // Fall back to manual times from ScheduleStore
            scheduleUsingManualTimes(c);
        }
    }
    
    private static void scheduleUsingSajadahTimes(Context c, String... todayKeys) {
        ZoneId zone = ZoneId.of("Asia/Makassar");
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime best = null;
        String bestKey = null;
        
        // Only check the provided today keys
        String[] keys = todayKeys;
        for (String key : keys) {
            if (key == null || key.isEmpty()) continue;
            String value = prefs.getString(key, "");
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
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            long trigger = best.atZone(zone).toInstant().toEpochMilli();
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }
    
    private static void scheduleUsingManualTimes(Context c) {
        // Existing logic - iterate through ScheduleStore keys
        ZoneId zone = ZoneId.of("Asia/Makassar");
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime best = null;
        String bestKey = null;
        for (String key : ScheduleStore.keys(c)) {
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
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
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
