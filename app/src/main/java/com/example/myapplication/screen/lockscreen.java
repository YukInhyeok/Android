package com.example.myapplication.screen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.Utils;

// 잠금화면
public class lockscreen extends AppCompatActivity {

    private Button goingbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lockscreen);

        // 메뉴 하단바 삭제
        Utils.deleteMenuButton(this);

        // 홈 버튼이나 뒤로가기 버튼 비활성화를 위한 플래그 추가
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        goingbtn = findViewById(R.id.going_btn);

        goingbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(lockscreen.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    protected void onBackpressed(){

    }
}