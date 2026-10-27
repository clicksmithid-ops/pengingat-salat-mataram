package com.clicksmith.salatreminder;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.GregorianCalendar;

public final class ScheduleStore {
    private static final String PREF = "schedule";
    private static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    private static final String SAJADAH_PREF = "sajadah_schedule";

    private ScheduleStore() {
    }

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

    public static String[] keys() {
        return KEYS.clone();
    }

    /**
     * Get the stored prayer time using the original key format
     */
    public static String nameFor(String key) {
        switch (key) {
            case "fajr": return "Subuh";
            case "dhuhr": return "Zuhur";
            case "asr": return "Asar";
            case "maghrib": return "Magrib";
            default: return "Isya";
        }
    }

    /**
     * Get the prefs object for this schedule store
     */
    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /**
     * Get the Sajadah API schedule prefs object
     */
    public static SharedPreferences sajadahPrefs(Context c) {
        return c.getSharedPreferences(SAJADAH_PREF, Context.MODE_PRIVATE);
    }

    /**
     * Get the keys array (original format)
     */
    