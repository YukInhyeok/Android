package com.example.myapplication.screen;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MidnightResetWorker extends Worker {

    public MidnightResetWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 여기에서 핸드폰 사용량을 초기화하는 작업을 수행합니다.
        // 자정에 실행될 코드를 구현하세요.
        resetAppUsageTime(getApplicationContext());

        // 다음 자정에 작업을 다시 예약합니다.
        scheduleNextMidnightReset();

        resetBookCount();
        checkScheduledWork(getApplicationContext());

        // 작업이 성공적으로 완료되면 Result.success()를 반환합니다.
        return Result.success();
    }

    private void resetAppUsageTime(Context context) {
        // 자정에 핸드폰 사용량을 초기화하는 코드를 구현합니다.
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                "app_usage_time_pref", Context.MODE_PRIVATE);
        // 사용량 초기화
        sharedPreferences.edit().putLong("app_usage_time", 0).apply();
    }

    private void scheduleNextMidnightReset() {
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

    //책 초기화
    private void resetBookCount() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference docRef = firestore.collection("Book").document("finish");

        // 독후감 갯수를 0으로 초기화
        docRef.set(new HashMap<String, Object>() {{
                    put("booknum", 0);
                }}, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("MidnightResetWorker", "Book count reset to 0 in Firestore.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("MidnightResetWorker", "Error resetting Book count in Firestore.", e);
                    }
                });
    }


    private void checkScheduledWork(Context context) {
        // WorkManager 초기화
        WorkManager workManager = WorkManager.getInstance(context);

        // 예약된 작업 조회
        workManager.getWorkInfosForUniqueWork("midnight_reset_work")
                .addListener(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("MyForeground", "Work scheduled");
                    }
                }, Executors.newSingleThreadExecutor());
    }
}