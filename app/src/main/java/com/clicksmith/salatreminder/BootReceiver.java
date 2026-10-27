package com.clicksmith.salatreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Settings.canDrawOverlays(context) && ScheduleStore.hasAnySchedule(context)) {
            AlarmScheduler.scheduleNext(context);
        }
    }
}
