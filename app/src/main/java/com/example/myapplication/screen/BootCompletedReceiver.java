package com.example.myapplication.screen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent lockscreenIntent = new Intent(context, lockscreen.class);
            lockscreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(lockscreenIntent);
        }
    }
}
