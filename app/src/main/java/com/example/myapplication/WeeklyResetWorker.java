package com.example.myapplication;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class WeeklyResetWorker extends Worker {

    private FirebaseFirestore db;

    public WeeklyResetWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
        db = FirebaseFirestore.getInstance();
    }
    @NonNull
    @Override
    public Result doWork() {
        // 주간 데이터 초기화
        resetWeeklyData();
<<<<<<< HEAD
=======
        // 작업이 성공적으로 완료되었음을 알립니다.
>>>>>>> 60e38a4169f0136a8cf2ede011983f0dd440c75d
        return Result.success();
    }

    private void resetWeeklyData() {
        int zeroValue = 0;
        Map<String, Object> data = new HashMap<>();
        data.put("average", zeroValue);

        // 'WeekChart' 컬렉션의 문서 목록
        String[] weekdays = {"Mon", "Tues", "Wed", "Thurs", "Fri"};
        for (int i = 0; i < weekdays.length; i++) {
            DocumentReference weekDocRef = db.collection("WeekChart").document(weekdays[i]);
            weekDocRef.set(data, SetOptions.merge());
        }
    }
}
