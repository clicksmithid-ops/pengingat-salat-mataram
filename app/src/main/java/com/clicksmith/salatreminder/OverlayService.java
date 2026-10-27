package com.clicksmith.salatreminder;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    public static final String ACTION_SHOW_TEST = "com.clicksmith.salatreminder.SHOW_TEST";
    private WindowManager wm;
    private android.view.View overlay;
    @Override public void onCreate() { super.onCreate(); wm = (WindowManager)getSystemService(WINDOW_SERVICE); }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SHOW_TEST.equals(intent.getAction())) show("Ini popup uji — Pengingat Salat Mataram");
        else if (intent != null) show(intent.getStringExtra("prayer_name") + " — waktunya salat");
        return START_NOT_STICKY;
    }
    private void show(String message) {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        remove();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(22, 14, 22, 14);
        box.setBackgroundColor(0xEE10202A);
        TextView text = new TextView(this);
        text.setText(message == null ? "Pengingat Salat Mataram" : message);
        text.setTextColor(0xFFFFFFFF);
        text.setTextSize(20);
        box.addView(text);
        Button close = new Button(this);
        close.setText("Tutup");
        close.setOnClickListener(v -> { remove(); stopSelf(); });
        box.addView(close);
        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            (int)(getResources().getDisplayMetrics().widthPixels * 0.34),
            WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = 24; p.y = 40;
        overlay = box;
        wm.addView(overlay, p);
    }
    private void remove() {
        if (overlay != null) { try { wm.removeView(overlay); } catch (Exception ignored) {} overlay = null; }
    }
    @Override public void onDestroy() { remove(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
