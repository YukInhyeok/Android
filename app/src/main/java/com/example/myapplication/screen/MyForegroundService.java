package com.example.myapplication.screen;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// 포그라운드
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

        // 자정에 실행될 코드를 추가합니다.
        scheduleMidnightReset();
        checkScheduledWork(getApplicationContext());



        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOnReceiver, filter);

        handler.post(updateAppUsageTimeRunnable);
    }

    //자정 초기화 코드
    private void scheduleMidnightReset() {
        // WorkManager 초기화
        WorkManager workManager = WorkManager.getInstance(getApplicationContext());

        // 작업 요청 생성
        OneTimeWorkRequest midnightResetWork = new OneTimeWorkRequest.Builder(MidnightResetWorker.class)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build();

        // 작업 예약
        workManager.enqueueUniqueWork(
                "midnight_reset_work",  // 작업 이름 (고유해야 함)
                ExistingWorkPolicy.REPLACE, // 작업 충돌 시 정책 설정
                midnightResetWork); // 작업 요청
    }

    // 자정까지의 시간을 계산하여 초기 예약 지연을 설정합니다.
    private long calculateInitialDelay() {
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        long currentTimeMillis = System.currentTimeMillis();
        long midnightMillis = midnight.getTimeInMillis();

        if (midnightMillis <= currentTimeMillis) {
            // 자정이 이미 지났으면 내일 자정까지 대기
            midnight.add(Calendar.DAY_OF_YEAR, 1);
            midnightMillis = midnight.getTimeInMillis();
        }

        return midnightMillis - currentTimeMillis;
    }
    // 워크매니저 예약 확인
    private void checkScheduledWork(Context context) {
        // WorkManager 초기화
        WorkManager workManager = WorkManager.getInstance(context);

        // 예약된 작업 조회
        workManager.getWorkInfosForUniqueWork("midnight_reset_work")
                .addListener(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("MyForegroundService", "Work scheduled");
                    }
                }, Executors.newSingleThreadExecutor());
    }





    //========================================================================================
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