package com.example.myapplication.screen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class ScreenOnReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ScreenOnReceiver", "onReceive called");
        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("AppData", Context.MODE_PRIVATE);

            String formattedAppUsageTime = sharedPreferences.getString("formattedAppUsageTime", "0");
            int finishBooknum = sharedPreferences.getInt("finishBooknum", 0);
            long timeValue = sharedPreferences.getLong("timeValue", 0);
            int workNum = sharedPreferences.getInt("workNum", 0);
            Log.d("ScreenOnReceiver", "formattedAppUsageTime: " + formattedAppUsageTime);
            Log.d("ScreenOnReceiver", "finishBooknum: " + finishBooknum);
            Log.d("ScreenOnReceiver", "timeValue: " + timeValue);
            Log.d("ScreenOnReceiver", "workNum: " + workNum);

            if (Long.parseLong(formattedAppUsageTime) < timeValue || finishBooknum < workNum) {
                Intent lockScreenIntent = new Intent(context.getApplicationContext(), lockscreen.class);
                lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lockScreenIntent);
            }
        }
    }
}