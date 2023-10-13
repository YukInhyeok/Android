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
        // 작업이 성공적으로 완료되었음을 알립니다.
        return Result.success();
    }

    private void resetWeeklyData() {
        int zeroValue = 0;
        Map<String, Object> data = new HashMap<>();
        data.put("average", zeroValue);
        data.put("literacy", zeroValue);
        data.put("read", zeroValue);
        data.put("vocabulary", zeroValue);

        // 'WeekChart' 컬렉션의 문서 목록
        String[] weekdays = {"Mon", "Tues", "Wed", "Thurs", "Fri"};
        for (int i = 0; i < weekdays.length; i++) {
            DocumentReference weekDocRef = db.collection("WeekChart").document(weekdays[i]);
            weekDocRef.set(data, SetOptions.merge());
        }
    }
}
