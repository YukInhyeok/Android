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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.util.concurrent.TimeUnit;

public class MyForegroundService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String APP_USAGE_TIME_PREF = "app_usage_time_pref";
    public static final String APP_USAGE_TIME_KEY = "app_usage_time";

    private static final int NOTIFICATION_ID = 1;

    private SharedPreferences sharedPreferences;
    private Handler handler;
    private long lastRecordedTime;
    private BroadcastReceiver screenOnReceiver;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "My Foreground Service";
            String description = "Foreground service is running.";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("My Foreground Service")
                .setContentText("Service is running...")
                .setSmallIcon(R.drawable.home_24)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        sharedPreferences = getSharedPreferences(APP_USAGE_TIME_PREF, MODE_PRIVATE);

        handler = new Handler();
        lastRecordedTime = SystemClock.elapsedRealtime();

        screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    saveAppUsageTime();
                    handler.removeCallbacks(updateAppUsageTimeRunnable);
                } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    lastRecordedTime = SystemClock.elapsedRealtime();
                    handler.post(updateAppUsageTimeRunnable);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOnReceiver, filter);

        handler.post(updateAppUsageTimeRunnable);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenOnReceiver != null) {
            unregisterReceiver(screenOnReceiver);
        }
        saveAppUsageTime();
    }

    private void saveAppUsageTime() {
        long currentTime = SystemClock.elapsedRealtime();
        long elapsedTime = currentTime - lastRecordedTime;
        lastRecordedTime = currentTime;
        long currentAppUsageTime = getAppUsageTime() + elapsedTime;
        sharedPreferences.edit().putLong(APP_USAGE_TIME_KEY, currentAppUsageTime).apply();
    }

    private long getAppUsageTime() {
        return sharedPreferences.getLong(APP_USAGE_TIME_KEY, 0);
    }

    private Runnable updateAppUsageTimeRunnable = new Runnable() {
        @Override
        public void run() {
            saveAppUsageTime();
            sendAppUsageTimeBroadcast();
            handler.postDelayed(this, (1000));
        }
    };

    private void sendAppUsageTimeBroadcast() {
        Intent appUsageTimeIntent = new Intent("app_usage_time_intent_action");
        appUsageTimeIntent.putExtra(APP_USAGE_TIME_KEY, getAppUsageTime());
        sendBroadcast(appUsageTimeIntent);
    }

}