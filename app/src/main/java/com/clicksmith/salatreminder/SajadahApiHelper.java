package com.clicksmith.salatreminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class SajadahApiHelper {
    private static final String TAG = "SajadahApiHelper";
    private static final String PREF_KEY = "sajadah_schedule";
    private static final String[] PRAYER_KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};

    // API endpoints
    private static final String REGIONS_URL = "https://sajadah.org/api/v1/regions.json";
    private static final String TIMES_URL_PREFIX = "https://sajadah.org/api/v1/times/id/";

    private static OkHttpClient client = new OkHttpClient();

    // Region ID for Kota Mataram, NTB
    private static final String MATARAM_REGION_ID = "kota-mataram";

    // Current year for schedule fetching
    private static int currentYear;

    public SajadahApiHelper() {
    }

    /**
     * Fetch the current year using Calendar
     */
    private static int getCurrentYear() {
        Calendar calendar = new GregorianCalendar();
        return calendar.get(Calendar.YEAR);
    }

    /**
     * Fetch regions from Sajadah API and find the region matching Kota Mataram,
     * Nusa Tenggara Barat, Asia/Makassar
     *
     * @return region ID if found, null otherwise
     */
    public static String fetchAndFindMataramRegion(Context context) {
        currentYear = getCurrentYear();

        try {
            // Step 1: Fetch regions list
            String json = fetch(REGIONS_URL);
            if (json == null) {
                Log.e(TAG, "Failed to fetch regions");
                return null;
            }

            JSONObject regionsResponse = new JSONObject(json);
            JSONArray regions = regionsResponse.getJSONArray("regions");

            // Search for Kota Mataram with correct criteria
            for (int i = 0; i < regions.length(); i++) {
                JSONObject region = regions.getJSONObject(i);
                String name = region.getString("name");
                String province = region.getString("province");
                String country = region.getString("country");
                String tz = region.getString("tz");
                JSONArray years = region.getJSONArray("years");

                // Check: name contains Mataram, province is NTB, country is ID, tz is Asia/Makassar
                if (name.contains("Mataram")
                        && "Nusa Tenggara Barat".equals(province)
                        && "ID".equals(country)
                        && "Asia/Makassar".equals(tz)) {
                    // Verify the year we need is available
                    for (int j = 0; j < years.length(); j++) {
                        int year = years.getInt(j);
                        if (year >= currentYear - 1 && year <= currentYear + 1) {
                            Log.i(TAG, "Found Mataram region: " + region.getString("id") +
                                    ", tz: " + tz + ", years available: " + years.toString());
                            return region.getString("id");
                        }
                    }
                }
            }
            Log.w(TAG, "Mataram region not found with all criteria");
            return null;

        } catch (IOException e) {
            Log.e(TAG, "IO Error fetching regions", e);
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error in regions", e);
        }
        return null;
    }

    /**
     * Fetch schedule data for a specific region and year
     *
     * @param regionId region ID (e.g., "kota-mataram")
     * @param year     year (e.g., 2026)
     * @return JSONObject containing schedule data, or null on failure
     */
    public static JSONObject fetchSchedule(String regionId, int year, Context context) {
        try {
            String url = TIMES_URL_PREFIX + regionId + "/" + year + ".json";
            String json = fetch(url);
            if (json == null) {
                Log.e(TAG, "Failed to fetch schedule for " + regionId + "/" + year);
                return null;
            }

            JSONObject response = new JSONObject(json);
            // Validate schema
            if (response.getInt("schema") != 1) {
                Log.e(TAG, "Invalid schema version: " + response.getInt("schema"));
                return null;
            }

            // Validate fields
            JSONArray fields = response.getJSONArray("fields");
            if (fields.length() < 9) {
                Log.e(TAG, "Invalid fields count: " + fields.length());
                return null;
            }

            // Store the schedule
            saveScheduleToPrefs(response, context);

            Log.i(TAG, "Fetched schedule for " + regionId + " " + year);
            return response;

        } catch (IOException e) {
            Log.e(TAG, "IO Error fetching schedule, will retry later", e);
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error in schedule", e);
            return null;
        }
        return null;
    }

    /**
     * Save schedule data to SharedPreferences for local caching
     */
    private static void saveScheduleToPrefs(JSONObject response, Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("sajadah_schedule", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save the fetch timestamp
            long now = System.currentTimeMillis();
            editor.putLong("sajadah_last_fetch", now);
            editor.putInt("sajadah_year", currentYear);

            // Save the prayer times from the days array
            JSONArray days = response.getJSONArray("days");
            for (int i = 0; i < days.length(); i++) {
                JSONArray day = days.getJSONArray(i);
                String date = day.getString(0); // YYYY-MM-DD format

                // Extract prayer times based on field indices:
                // fields: [date, imsak, fajr, sunrise, dhuha, dhuhr, asr, maghrib, isha]
                // Indices:       0      1       2         3       4      5       6       7       8
                String fajr = day.getString(2);    // Subuh
                String dhuhr = day.getString(5);   // Zuhur
                String asr = day.getString(6);     // Asar
                String maghrib = day.getString(7); // Magrib
                String isha = day.getString(8);    // Isya

                // Store each prayer time with date as key prefix
                editor.putString("sajadah_fajr_" + date, fajr);
                editor.putString("sajadah_dhuhr_" + date, dhuhr);
                editor.putString("sajadah_asr_" + date, asr);
                editor.putString("sajadah_maghrib_" + date, maghrib);
                editor.putString("sajadah_isha_" + date, isha);
            }
            editor.apply();
            Log.i(TAG, "Saved schedule for " + currentYear + " with " + days.length() + " days");

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error saving schedule to prefs", e);
        }
    }

    /**
     * Fetch schedule data using OkHttp
     */
    private static String fetch(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP error: " + response.code());
                return null;
            }
            return response.body().string();
        }
    }

    /**
     * Get stored fajr time for a specific date
     *
     * @param context Context
     * @param date    date in YYYY-MM-DD format
     * @return fajr time string or empty if not available
     */
    public static String getStoredFajr(Context context, String date) {
        SharedPreferences prefs = context.getSharedPreferences("sajadah_schedule", Context.MODE_PRIVATE);
        return prefs.getString("sajadah_fajr_" + date, "");
    }

    /**
     * Get stored dhuhr time for a specific date
     */
    public static String getStoredDhuhr(Context context, String date) {
        SharedPreferences prefs = context.getSharedPreferences("sajadah_schedule", Context.MODE_PRIVATE);
        return prefs.getString("sajadah_dhuhr_" + date, "");
    }

    /**
     * Get stored asr time for a specific date
     */
    public static String getStoredAsr(Context context, String date) {
        SharedPreferences prefs = context.getSharedPreferences("sajadah_schedule", Context.MODE_PRIVATE);
        return prefs.getString("sajadah_asr_" + date, "");
    }

    /**
     * Get stored maghrib time for a specific date
     */
    public static String getStoredMaghrib(Context context, String date) {
        SharedPreferences prefs = context.getSharedPreferences("sajadah_schedule", Context.MODE_PRIVATE);
        return prefs.getString("sajadah_maghrib_" + date, "");
    }

    /**
     * Get stored isha time for a specific date
     */
    public static String getStoredIsha(Context context, String date) {
        SharedPreferences prefs = context.getSharedPreferences("sajadah_schedule", Context.MODE_PRIVATE);
        return prefs.getString("sajadah_isha_" + date, "");
    }

    /**
     * Check if we have valid cached schedule data for the current date
     *
     * @param context Context
     * @return true if we have valid cached data for today
     */
    public static boolean hasValidCachedSchedule(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sajadah_schedule", Context.MODE_PRIVATE);
        long lastFetch = prefs.getLong("sajadah_last_fetch", 0);
        int storedYear = prefs.getInt("sajadah_year", 0);

        // Check if data was fetched for current year
        if (storedYear != getCurrentYear()) {
            return false;
        }

        return true;
    }
}