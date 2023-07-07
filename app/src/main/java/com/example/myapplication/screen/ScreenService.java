package com.example.myapplication.screen;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ScreenService extends Service {
    private ScreenReceiver mReceiver = null;

    // 사용 시간 추적을 위한 변수들
    private long startTime;
    private long usageTime;
    private CountDownTimer timer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTime = System.currentTimeMillis();
        timer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                usageTime = System.currentTimeMillis() - startTime;
                int roundedTime = (int) (usageTime / 1000);
                sendUsageTimeBroadcast(roundedTime); // MainActivity로 사용 시간 전달
            }

            @Override
            public void onFinish() {

            }
        };
        timer.start();

        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            if (intent.getAction() == null) {
                if (mReceiver == null) {
                    mReceiver = new ScreenReceiver();
                    IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                    registerReceiver(mReceiver, filter);
                }
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 타이머 중지 및 사용 시간 기록
        if (timer != null) {
            timer.cancel();
        }
        long lastUsage = System.currentTimeMillis() - startTime;
        sendUsageTimeBroadcast(lastUsage); // MainActivity로 마지막 사용 시간 전달

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    private void sendUsageTimeBroadcast(long usageTime) {
        Intent intent = new Intent("com.example.myapplication.USAGE_TIME_UPDATE");
        intent.putExtra("usageTime", usageTime);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}