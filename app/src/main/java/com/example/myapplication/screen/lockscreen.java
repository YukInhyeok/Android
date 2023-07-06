package com.example.myapplication.screen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.ChatGpt;
import com.example.myapplication.R;

public class lockscreen extends AppCompatActivity {

    private Button goingbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lockscreen);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        goingbtn = findViewById(R.id.going_btn);

        goingbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(lockscreen.this, ChatGpt.class);
                startActivity(intent);
            }
        });
    }
}