package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.book.BookMainActivity;
import com.example.myapplication.book.ResetCountReceiver;
import com.example.myapplication.screen.MyForegroundService;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{
    //네비게이션바
    private BottomNavigationView bottomNavigationView;
    //포인트 관련
    private EditText editTextNumber;
    private Button point;
    private TextView pointNum;
    // 포인트 잔액
    private int cashValue;
    //firebase
    private FirebaseFirestore db;
    //독후감
    private TextView bookNum;
    private int finishBooknum;
    //제한 시간
    private TextView limitTime;

    // 핸드폰 사용시간
// 멤버 변수로 screenOnReceiver 등록
    private BroadcastReceiver screenOnReceiver;
    private boolean isScreenOn = false;
    private long screenOnTime = 0;
    private long screenOffTime = 0;
    long usedTimeInMinutes = 0;
    private Handler updateHandler;
    private Runnable updateRunnable;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //firebase
        db = FirebaseFirestore.getInstance();

        //Foreground 설정
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // BottomNavigationView 초기화
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // 네비게이션 아이템 클릭 이벤트 처리
                int itemId = item.getItemId();

                if (itemId == R.id.menu_gpt) {
                    // "GPT" 아이템 클릭 시 동작
                    Intent intent1 = new Intent(MainActivity.this, ChatGpt.class);
                    startActivity(intent1);
                } else if (itemId == R.id.menu_book) {
                    // "Book" 아이템 클릭 시 동작
                    Intent intent2 = new Intent(MainActivity.this, BookMainActivity.class);
                    startActivity(intent2);
                } else if (itemId == R.id.menu_info) {
                    // "정보확인" 아이템 클릭 시 동작
                    Intent intent3 = new Intent(MainActivity.this, MyInfo.class);
                    startActivity(intent3);
                }
                return true;
            }
        });

        // 초기 선택된 네비게이션 아이템 설정.
        bottomNavigationView.setSelectedItemId(R.id.menu_home);

        // 레이더 차트 추가
        RadarChart radarChart = findViewById(R.id.chart);
        setData(radarChart);

        // 포인트 관련
        editTextNumber = findViewById(R.id.editTextNumber);
        point = findViewById(R.id.point);
        pointNum = findViewById(R.id.point_num);

        //독후감 관련
        bookNum = findViewById(R.id.book_text);
        fetchFinishBookNum();
        setAlarmToResetCount();

        // 제한 시간
        limitTime = findViewById(R.id.limit_time);
        fetchLimitTime();

        //핸드폰 사용 시간



        initPointListener();
        point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputPoint = Integer.parseInt(editTextNumber.getText().toString());

                if (inputPoint <= cashValue) {
                    int remainingPoint = cashValue - inputPoint;

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("UserCoin").document("Coin")
                            .update("point", remainingPoint)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("MainActivity", "DocumentSnapshot successfully updated!");
                                    showMessage(inputPoint + "포인트가 사용되었습니다"); // 메시지 표시

                                    // 사용된 포인트를 UseCoin 컬렉션에 추가
                                    Map<String, Object> usedPointData = new HashMap<>();
                                    usedPointData.put("usedPoint", inputPoint);
                                    usedPointData.put("timestamp", FieldValue.serverTimestamp()); // 서버 타임스탬프로 시간 기록

                                    db.collection("UseCoin")
                                            .add(usedPointData)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    Log.d("MainActivity", "Used point added to UseCoin collection");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w("MainActivity", "Error adding used point to UseCoin collection", e);
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("MainActivity", "Error updating document", e);
                                }
                            });
                    editTextNumber.setText(""); // 입력한 숫자 지우기
                } else {
                    showMessage("보유한 포인트보다 많은 포인트를 사용할 수 없습니다.");
                }
            }
        });

        //핸드폰 사용 시간
////         SCREEN_ON 시간 측정을 위한 BroadcastReceiver 등록
        screenOnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    isScreenOn = true;
                    screenOnTime = System.currentTimeMillis();
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    isScreenOn = false;
                    screenOffTime = System.currentTimeMillis();
                    usedTimeInMinutes += TimeUnit.MILLISECONDS.toMinutes(screenOffTime - screenOnTime);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOnReceiver, intentFilter);

        // 주기적으로 사용 시간 업데이트를 위한 Handler 초기화
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUsedTime(); // 사용 시간 업데이트 메서드를 여기에서 호출합니다.
                updateHandler.postDelayed(this, (1000 * 60)); // 업데이트 간격 설정 (1000 밀리세컨드 = 1초)
            }
        };

    }

    //레이더 차트
    private void setData(RadarChart radarChart) {
        fetchData(new MyInfo.FirestoreCallback() {
            @Override
            public void onDataLoaded(ArrayList<RadarEntry> entries) {
                RadarDataSet dataSet = new RadarDataSet(entries, "주간 데이터");
                dataSet.setColor(Color.RED);
                RadarData data = new RadarData(dataSet);
                radarChart.setData(data);
                radarChart.invalidate();
            }
        });
    }

    private void initPointListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("UserCoin").document("Coin")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            cashValue = documentSnapshot.getLong("point").intValue();
                            pointNum.setText(Integer.toString(cashValue));
                            editTextNumber.setHint("최대 " + cashValue);
                        }
                    }
                });
    }

    //클릭 메소드
    private void showMessage(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    // 다른 앱 위에 표시 권한
    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("다른 앱 위에 표시 권한 요청");
        builder.setMessage("이 앱은 다른 앱 위에 표시되는 기능이 있습니다. 허용하시겠습니까?");

        builder.setPositiveButton("허용", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestOverlayPermission();
            }
        });

        builder.setNegativeButton("거부", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 거절 시 처리 코드
                finish();
            }
        });

        builder.show();
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 100);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showPermissionDialog();
            }
        }
    }


    public void onBackPressed(){

    }

    //Chart
    private void fetchData(MyInfo.FirestoreCallback callback) {
        db.collection("Chart").orderBy("label")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<RadarEntry> entries = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                float value = document.getDouble("value").floatValue();
                                int label = document.getLong("label").intValue();
                                entries.add(new RadarEntry(value, label));
                            }

                            callback.onDataLoaded(entries);
                        } else {
                            Log.e("MyInfo", "Error fetching data", task.getException());
                        }
                    }
                });
    }
    // 콜백 인터페이스 추가
    private interface FirestoreCallback {
        void onDataLoaded(ArrayList<RadarEntry> entries);
    }

    // 독후감 책 관련 메서드
    private void fetchWorkNum() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Book").document("report")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            int workNum = documentSnapshot.getLong("worknum").intValue();
                            bookNum.setText("책: " + finishBooknum + " / " + workNum + " 권"); // TextView에 반영
                        }
                    }
                });
    }

    private void fetchFinishBookNum() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Book").document("finish")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            finishBooknum = documentSnapshot.getLong("booknum").intValue();
                            // finishBooknum 값을 가져온 후 fetchWorkNum 메소드를 호출합니다.
                            fetchWorkNum();
                        }
                    }
                });
    }

    //제한 시간 메서드
    private void fetchLimitTime() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Time").document("SetTime")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("MainActivity", "listen:error", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String timeString = documentSnapshot.getString("time");
                            int timeValue = convertTimeStringToMinutes(timeString);
                            limitTime.setText("시간: " + usedTimeInMinutes + " / " + timeValue + "분");

                        }
                    }
                });
    }


    private int convertTimeStringToMinutes(String timeString) {
        String[] timeParts = timeString.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        return hours * 60 + minutes;
    }




    //핸드폰 사용 시간

        @Override
    protected void onResume() {
        super.onResume();
        isScreenOn = true;
        screenOnTime = System.currentTimeMillis();
        usedTimeInMinutes = loadUsedTime(); // onResume 에서 저장된 사용 시간 불러오기
        updateUsedTime();
        updateHandler.post(updateRunnable); // Runnable 시작
    }


    @Override
    protected void onPause() {
        super.onPause();
        updateUsedTime();
        saveUsedTime(); // onPause 에서 사용 시간 저장하기
        isScreenOn = false;
        updateHandler.removeCallbacks(updateRunnable); // Runnable 중지
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateUsedTime();
        saveUsedTime(); // onStop 에서 사용 시간 저장하기
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // BroadcastReceiver 해제
        unregisterReceiver(screenOnReceiver);
    }

    private void updateUsedTime() {
        long currentTime = System.currentTimeMillis();
        if (isScreenOn) {
            usedTimeInMinutes += (currentTime - screenOnTime) / 60000; // 분 단위로 사용 시간 증가
            screenOnTime = currentTime;
        }
        fetchLimitTime(); // fetchLimitTime() 메서드가 사용 시간을 업데이트하므로 이 부분에서 호출해주세요.
    }

    private void saveUsedTime() {
        SharedPreferences sharedPreferences = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("USED_TIME", usedTimeInMinutes);
        editor.apply();
    }
    private long loadUsedTime() {
        SharedPreferences sharedPreferences = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        return sharedPreferences.getLong("USED_TIME", 0);
    }

    //추가
    private void setAlarmToResetCount() {
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ResetCountReceiver.class);

        PendingIntent alarmIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }

}
