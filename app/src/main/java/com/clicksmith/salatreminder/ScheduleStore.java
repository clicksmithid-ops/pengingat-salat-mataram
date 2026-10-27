package com.clicksmith.salatreminder;

import android.content.Context;
import android.content.SharedPreferences;

public final class ScheduleStore {
    private static final String PREF = "schedule";
    private static final String[] KEYS = {"fajr","dhuhr","asr","maghrib","isha"};
    private ScheduleStore() {}
    public static void put(Context c, String key, String value) {
        prefs(c).edit().putString(key, value).apply();
    }
    public static String get(Context c, String key) {
        return prefs(c).getString(key, "");
    }
    public static boolean hasAnySchedule(Context c) {
        SharedPreferences p = prefs(c);
        for (String k : KEYS) if (p.contains(k) && !p.getString(k, "").isEmpty()) return true;
        return false;
    }
    public static String nameFor(String key) {
        switch (key) {
            case "fajr": return "Subuh";
            case "dhuhr": return "Zuhur";
            case "asr": return "Asar";
            case "maghrib": return "Magrib";
            default: return "Isya";
        }
    }
    public static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
    public static String[] keys() { return KEYS.clone(); }
}
