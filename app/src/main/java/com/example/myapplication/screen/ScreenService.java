package com.example.myapplication.screen;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.widget.Button;

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
                // 여기에서 사용시간을 업데이트하거나 다른 작업을 수행 할 수 있습니다. 1초마다 이 메서드가 불리워집니다.
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
        // 여기에서 사용 시간 'lastUsage'에 대한 작업을 수행하거나 필요한 곳에 저장하십시오.

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }
}