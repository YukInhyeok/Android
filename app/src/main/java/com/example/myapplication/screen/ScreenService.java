package com.example.myapplication.screen;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Calendar;

public class ScreenService extends Service {
    private ScreenReceiver mReceiver = null;
    private CountDownTimer timer;
    private Handler handler;
    private Runnable resetRunnable;
    private long startTime;
    private long usageTime;

    private final IBinder binder = new ScreenServiceBinder();

    public class ScreenServiceBinder extends Binder {
        @SuppressLint("WrongConstant")
        public ScreenService getService() {
            return ScreenService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);

        handler = new Handler();
        resetRunnable = new Runnable() {
            @Override
            public void run() {
                resetUsageTime();
            }
        };

        // Schedule the initial reset at midnight
        scheduleResetAtMidnight();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTime = System.currentTimeMillis();
        timer = new CountDownTimer(Long.MAX_VALUE, 60000) {
            @Override
            public void onTick(long millisUntilFinished) {
                usageTime = System.currentTimeMillis() - startTime;
                int roundedTime = (int) (usageTime / (60 * 1000));
                sendUsageTimeBroadcast(roundedTime);
            }

            @Override
            public void onFinish() {

            }
        };
        timer.start();

        // Rest of the code remains the same
        // ...

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop the timer and calculate the last usage time
        if (timer != null) {
            timer.cancel();
        }
        long lastUsage = System.currentTimeMillis() - startTime;
        sendUsageTimeBroadcast(lastUsage);

        // Unregister the receiver
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }

        // Remove the reset runnable from the handler
        if (handler != null && resetRunnable != null) {
            handler.removeCallbacks(resetRunnable);
        }
    }

    private void sendUsageTimeBroadcast(long usageTime) {
        Intent intent = new Intent("com.example.myapplication.USAGE_TIME_UPDATE");
        intent.putExtra("usageTime", usageTime);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void resetUsageTime() {
        startTime = System.currentTimeMillis();
        scheduleResetAtMidnight();
    }

    private void scheduleResetAtMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long timeUntilMidnight = calendar.getTimeInMillis() - System.currentTimeMillis();

        // Schedule the reset runnable at midnight
        if (handler != null && resetRunnable != null) {
            handler.removeCallbacks(resetRunnable); // Remove any existing callbacks
            handler.postDelayed(resetRunnable, timeUntilMidnight);
        }
    }
}
