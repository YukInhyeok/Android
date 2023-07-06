package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.book.BookMainActivity;
import com.example.myapplication.screen.ScreenService;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //네비게이션바
    private BottomNavigationView bottomNavigationView;
    //포인트 관련

    private EditText editTextNumber;
    private Button point;
    private TextView pointNum;
    // 포인트 잔액
    private int cashValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //LookScreen 설정
        startService(new Intent(MainActivity.this, ScreenService.class));

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

        updatePointNum(); // 데이터를 가져와 화면을 업데이트하는 코드 호출
        point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputPoint = Integer.parseInt(editTextNumber.getText().toString());
                int remainingPoint = cashValue - inputPoint;

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("UserCoin").document("Coin")
                        .update("point", remainingPoint)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("MainActivity", "DocumentSnapshot successfully updated!");
                                updatePointNum(); // 포인트 사용 후 화면 업데이트
                                showMessage(inputPoint + "포인트가 사용되었습니다"); // 메시지 표시
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("MainActivity", "Error updating document", e);
                            }
                        });
            }
        });
    }

    private void updatePointNum() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("UserCoin").document("Coin")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                cashValue = document.getLong("point").intValue();
                                pointNum.setText(Integer.toString(cashValue));
                                editTextNumber.setHint(Integer.toString(cashValue)); // 힌트로 출력
                                // 가져온 Cash 값을 사용할 수 있습니다.
                            } else {
                                Log.d("MainActivity", "No such document");
                            }
                        } else {
                            Log.d("MainActivity", "get failed with ", task.getException());
                        }
                    }
                });
    }

    //클릭 메소드
    private void showMessage(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }


    private void setData(RadarChart radarChart) {
        ArrayList<RadarEntry> entries = new ArrayList<>();
        entries.add(new RadarEntry(4f, 0));
        entries.add(new RadarEntry(3f, 1));
        entries.add(new RadarEntry(2f, 2));
        entries.add(new RadarEntry(5f, 3));
        entries.add(new RadarEntry(3f, 4));

        RadarDataSet dataSet = new RadarDataSet(entries, "주간 데이터");
        dataSet.setColor(Color.RED); // 색상을 빨간색으로 설정
        RadarData data = new RadarData(dataSet);
        radarChart.setData(data);
        XAxis xAxis = radarChart.getXAxis();
        final String[] labels = new String[]{"Label 1", "Label 2", "Label 3", "Label 4", "Label 5"};
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return labels[(int) value % labels.length];
            }
        });
        radarChart.invalidate();


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
}
