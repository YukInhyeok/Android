package com.example.myapplication.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class WeeklyResetReceiver extends BroadcastReceiver {
    private FirebaseFirestore db;

    public WeeklyResetReceiver() {
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
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
