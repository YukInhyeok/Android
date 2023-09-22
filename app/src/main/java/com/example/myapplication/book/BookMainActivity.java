package com.example.myapplication.book;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.myapplication.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;

public class BookMainActivity extends AppCompatActivity implements OnDatabaseCallback{

    private static final String TAG = "BookMainActivity";

    //네비게이션 바
    private BottomNavigationView bottomNavigationView;
    Fragment1 fragment1;
    Fragment2 fragment2;
    BookDatabase database;

    //독후감 등록 개수 카운트
    private static final String PREFS_NAME = "book_count_prefs";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_BOOK_COUNT = "book_count";

    private FirebaseFirestore firestore;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_main);

        // 메뉴 하단바 삭제
        Utils.deleteMenuButton(this);

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

        // 독후감 갯수 카운트
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        firestore = FirebaseFirestore.getInstance();

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
        updateBookCount();
        Toast.makeText(getApplicationContext(), "독후감 정보를 추가했습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public ArrayList<BookInfo> selectAll() {
        ArrayList<BookInfo> result = database.selectAll();
        Toast.makeText(getApplicationContext(), "독후감 정보를 조회했습니다.", Toast.LENGTH_SHORT).show();
        return result;
    }

//    //독후감 갯수 카운트
private void updateBookCount() {
    // final로 선언된 변수
    final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // 독후감 갯수 가져오기
    final int savedBookCount = prefs.getInt(KEY_BOOK_COUNT, 0);

    // 독후감 갯수 1 증가
    final int updatedBookCount = savedBookCount + 1;

    // Firestore에 독후감 갯수 업데이트
    DocumentReference docRef = firestore.collection("Book").document("finish");

    firestore.runTransaction(new Transaction.Function<Void>() {
        @Override
        public Void apply(Transaction transaction) throws FirebaseFirestoreException {
            transaction.update(docRef, "booknum", updatedBookCount);
            return null;
        }
    }).addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void unused) {
            Log.d(TAG, "Book count successfully updated in Firestore!");
            // 독후감 갯수를 SharedPreferences에 저장
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_BOOK_COUNT, updatedBookCount);
            editor.apply();
        }
    }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            Log.w(TAG, "Error updating Book count", e);
        }
    });
}





}