package com.example.myapplication.book;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.myapplication.ChatGpt;
import com.example.myapplication.MainActivity;
import com.example.myapplication.MyInfo;
import com.example.myapplication.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class BookMainActivity extends AppCompatActivity implements OnDatabaseCallback{

    private static final String TAG = "BookMainActivity";

    //네비게이션 바
    private BottomNavigationView bottomNavigationView;
    Fragment1 fragment1;
    Fragment2 fragment2;
    BookDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_main);

        fragment1 = new Fragment1();
        fragment2 = new Fragment2();

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment1).commit();

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText("입력"));
        tabs.addTab(tabs.newTab().setText("조회"));

        tabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Log.d("BookMainActivity", "선택된 탭 : " + position);

                Fragment selected = null;
                if (position == 0) {
                    selected = fragment1;
                } else if (position == 1) {
                    selected = fragment2;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.container, selected).commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        if (database != null) {
            database.close();
            database = null;
        }
        database = BookDatabase.getInstance(this);
        boolean isOpen = database.open();
        if (isOpen) {
            Log.d(TAG, "Book database is open.");
        } else {
            Log.d(TAG, "Book database is not open.");
        }


        // 네비게이션 바 설정
        // BottomNavigationView 초기화
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // 네비게이션 아이템 클릭 이벤트 처리
                int itemId = item.getItemId();

                if (itemId == R.id.menu_home) {
                    // "Home" 아이템 클릭 시 동작
                    Intent intent1 = new Intent(BookMainActivity.this, MainActivity.class);
                    startActivity(intent1);
                } else if (itemId == R.id.menu_gpt) {
                    // "Gpt" 아이템 클릭 시 동작
                    Intent intent2 = new Intent(BookMainActivity.this, ChatGpt.class);
                    startActivity(intent2);
                } else if (itemId == R.id.menu_info) {
                    // "정보확인" 아이템 클릭 시 동작
                    Intent intent3 = new Intent(BookMainActivity.this, MyInfo.class);
                    startActivity(intent3);
                }
                return true;
            }
        });

        // 초기 선택된 네비게이션 아이템 설정
        bottomNavigationView.setSelectedItemId(R.id.menu_book);

    }

    protected void onDestroy(){
        if (database != null) {
            database.close();
            database = null;
        }

        super.onDestroy();
    }

    @Override
    public void insert(String name, String author, String contents) {
        database.insertRecord(name, author, contents);
        Toast.makeText(getApplicationContext(), "책 정보를 추가했습니다.", Toast.LENGTH_LONG).show();
    }

    @Override
    public ArrayList<BookInfo> selectAll() {
        ArrayList<BookInfo> result = database.selectAll();
        Toast.makeText(getApplicationContext(), "책 정보를 조회했습니다.", Toast.LENGTH_LONG).show();
        return result;
    }
}