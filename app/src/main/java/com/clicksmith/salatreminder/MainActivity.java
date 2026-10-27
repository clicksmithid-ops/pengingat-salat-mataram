package com.clicksmith.salatreminder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView status;
    private TextView scheduleInfo;
    private SajadahApiHelper sajadahHelper;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        sajadahHelper = new SajadahApiHelper();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(36, 24, 36, 24);
        root.setBackgroundColor(0xFF101820);

        TextView title = new TextView(this);
        title.setText("Pengingat Salat Mataram");
        title.setTextSize(26);
        title.setTextColor(0xFFFFFFFF);
        root.addView(title);

        // Schedule info area
        scheduleInfo = new TextView(this);
        scheduleInfo.setTextSize(16);
        scheduleInfo.setTextColor(0xFFD6E2EA);
        scheduleInfo.setPadding(0, 20, 0, 20);
        root.addView(scheduleInfo);

        status = new TextView(this);
        status.setTextColor(0xFFFFD166);
        root.addView(status);

        // Auto-fetch schedule from Sajadah API
        sajadahHelper.fetchAndFindMataramRegion(this);
        boolean hasSchedule = sajadahHelper.hasValidCachedSchedule(this);
        updateScheduleDisplay(hasSchedule);

        // Buttons
        addButton(root, "Atur izin popup", v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        addButton(root, "Atur jadwal salat", v -> editSchedule());
        addButton(root, "Uji popup sekarang", v -> {
            if (!Settings.canDrawOverlays(this)) {
                status.setText("Aktifkan izin popup terlebih dahulu.");
                return;
            }
            Intent i = new Intent(this, OverlayService.class);
            i.setAction(OverlayService.ACTION_SHOW_TEST);
            startService(i);
            status.setText("Perintah uji popup dikirim.");
        });
        addButton(root, "Aktifkan pengingat harian", v -> {
            if (!Settings.canDrawOverlays(this)) {
                status.setText("Izin popup belum aktif.");
                return;
            }
            // Coba pakai jadwal yang tersimpan dulu
            if (!ScheduleStore.hasAnySchedule(this)) {
                // Coba ambil dari cache Sajadah
                if (!sajadahHelper.hasValidCachedSchedule(this)) {
                    status.setText("Jadwal belum diinstal dan izin popup belum aktif.");
                    return;
                }
                status.setText("Menggunakan jadwal dari data cached. Aktifkan pengingat?");
                // User still needs to manually activate
                AlarmScheduler.scheduleNext(this);
                status.setText("Pengingat harian diaktifkan untuk jadwal cached.");
                return;
            }
            AlarmScheduler.scheduleNext(this);
            status.setText("Pengingat harian diaktifkan untuk jadwal yang tersimpan.");
        });
        addButton(root, "Nonaktifkan pengingat", v -> {
            AlarmScheduler.cancel(this);
            status.setText("Pengingat dinonaktifkan.");
        });
        setContentView(root);
        updateStatus();
    }

    private void updateScheduleDisplay(boolean hasCached) {
        if (hasCached) {
            scheduleInfo.setText("✅ Jadwal tersedia dari cache lokal");
        } else {
            scheduleInfo.setText("⚠️ Tidak ada jadwal cached. Tekan 'Perbarui Jadwal' untuk mengambil dari Sajadah.");
        }
    }

    private void updateStatus() {
        status.setText("Izin popup: " + (Settings.canDrawOverlays(this) ? "aktif" : "belum aktif")
                + "
Jadwal tersimpan: " + (ScheduleStore.hasAnySchedule(this) ? "ada" : "belum ada"));
    }

    private void addButton(LinearLayout root, String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setOnClickListener(listener);
        root.addView(b);
    }

    private void editSchedule() {
        final String[] names = {"Subuh", "Zuhur", "Asar", "Magrib", "Isya"};
        final String[] keys = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
        StringBuilder msg = new StringBuilder("Isi jadwal pada format 24 jam (HH:mm) satu per satu. Gunakan jadwal resmi untuk kabupaten/kota Anda.");
        new AlertDialog.Builder(this).setTitle("Jadwal salat")
            .setItems(names, (d, which) -> {
                android.widget.EditText input = new android.widget.EditText(this);
                input.setSingleLine(true);
                input.setHint("contoh 04:35");
                input.setText(ScheduleStore.get(this, keys[which]));
                new AlertDialog.Builder(this).setTitle(names[which] + " — WITA")
                    .setView(input).setNegativeButton("Batal", null)
                    .setPositiveButton("Simpan", (dd, ww) -> {
                        String value = input.getText().toString().trim();
                        if (value.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
                            ScheduleStore.put(this, keys[which], value);
                            updateStatus();
                        } else {
                            new AlertDialog.Builder(this).setMessage("Format tidak valid. Gunakan HH:mm, misalnya 04:35.")
                                .setPositiveButton("OK", null).show();
                        }
                    }).show();
            }).setPositiveButton("Selesai", null).show();
    }
}