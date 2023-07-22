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

import javax.xml.transform.Result;

public class WeeklyResetWorker extends Worker {

    private FirebaseFirestore db;

    public WeeklyResetWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
        db = FirebaseFirestore.getInstance();
    }
    @NonNull
    @Override
    public Result doWork() {
        // 주간 데이터 초기화 작업을 수행합니다.
        resetWeeklyData();

        // 작업이 성공적으로 완료되었음을 알립니다.
        return Result.success();
    }

    private void resetWeeklyData() {
        // 주간 데이터 초기화 로직 구현: 기능을 하나의 메소드로 구현하여 호출하십시오.
        int zeroValue = 0;
        Map<String, Object> data = new HashMap<>();
        data.put("average", zeroValue);

        // 'WeekChart' 컬렉션의 문서 목록을 가져옵니다.
        String[] weekdays = {"Mon", "Tues", "Wed", "Thurs", "Fri"};
        for (int i = 0; i < weekdays.length; i++) {
            DocumentReference weekDocRef = db.collection("WeekChart").document(weekdays[i]);

            // 문서에 "average" 필드 값을 0으로 업데이트하거나 문서가 없는 경우 생성합니다.
            weekDocRef.set(data, SetOptions.merge());
        }
    }
}