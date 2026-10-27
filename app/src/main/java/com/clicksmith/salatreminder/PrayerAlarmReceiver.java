package com.clicksmith.salatreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String key = intent == null ? "" : intent.getStringExtra("prayer_key");
        if (key == null) key = "";
        Intent show = new Intent(context, OverlayService.class);
        show.putExtra("prayer_name", ScheduleStore.nameFor(key));
        context.startService(show);
        AlarmScheduler.scheduleNext(context);
    }
}
