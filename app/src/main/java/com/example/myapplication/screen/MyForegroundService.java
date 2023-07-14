package com.example.myapplication.screen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.util.concurrent.TimeUnit;

public class MyForegroundService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private boolean isScreenOn;
    private long screenOnTime;
    private long screenOffTime;
    private long usedTimeInMinutes;

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                isScreenOn = true;
                screenOnTime = System.currentTimeMillis();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                isScreenOn = false;
                screenOffTime = System.currentTimeMillis();
                usedTimeInMinutes += TimeUnit.MILLISECONDS.toMinutes(screenOffTime - screenOnTime);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOnReceiver, filter);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenOnReceiver);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("ForegroundServiceChannel", "Foreground Service Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void sendUsedTimeToMainActivity(long usedTimeInMinutes) {
        Intent intent = new Intent("UPDATE_USED_TIME");
        intent.putExtra("usedTimeInMinutes", usedTimeInMinutes);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        // Add this flag based on your requirements
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, "ForegroundServiceChannel")
                .setContentTitle("Screen Time Tracking")
                .setContentText("Tracking screen usage time.")
                .setSmallIcon(R.drawable._528)
                .setContentIntent(pendingIntent)
                .build();
    }
//    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
//                isScreenOn = true;
//                screenOnTime = System.currentTimeMillis();
//            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
//                isScreenOn = false;
//                screenOffTime = System.currentTimeMillis();
//                usedTimeInMinutes += TimeUnit.MILLISECONDS.toMinutes(screenOffTime - screenOnTime);
//
//                // 로그 출력
//                Log.d("MyForegroundService", "Used Time in Minutes: " + usedTimeInMinutes);
//
//                sendUsedTimeToMainActivity(usedTimeInMinutes);
//            }
//        }
//    };
}
