package com.example.myapplication.screen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;

// 휴대폰의 화면 꺼짐 및 켜짐 상태 확인하는 리시버

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

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            if (Long.parseLong(formattedAppUsageTime) < timeValue || finishBooknum < workNum) {
                Intent lockScreenIntent = new Intent(context.getApplicationContext(), lockscreen.class);
                lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lockScreenIntent);

                db.collection("Screen").document("Lock").update("state", 0);
            } else {
                db.collection("Screen").document("Lock").update("state", 1);
            }
        }
    }
}