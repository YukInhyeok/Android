package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.book.BookMainActivity;
import com.example.myapplication.model.Message;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatGpt extends AppCompatActivity {
    // 필요한 변수 및 뷰들을 선언합니다.
    RecyclerView recycler_view;
    TextView tv_welcome;
    EditText et_msg;
    ImageButton btn_send;

    Button start_btn;

    List<Message> messageList;
    MessageAdapter messageAdapter;
    JSONArray messages = new JSONArray();


    // API 호출에 사용할 상수와 객체를 선언합니다.
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client;
    private static final String MY_SECRET_KEY = "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";

    //네비게이션바 설정
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gpt_main);

        // 뷰들을 초기화하고 필요한 설정을 합니다.
        recycler_view = findViewById(R.id.recycler_view);
        tv_welcome = findViewById(R.id.tv_welcome);
        et_msg = findViewById(R.id.et_msg);
        btn_send = findViewById(R.id.btn_send);
        start_btn = findViewById(R.id.start_btn);

        recycler_view.setHasFixedSize(true);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        recycler_view.setLayoutManager(manager);

        // 메시지 리스트와 어댑터를 초기화합니다.
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recycler_view.setAdapter(messageAdapter);

        // 전송 버튼 클릭 이벤트 핸들러를 등록합니다.
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 사용자가 입력한 메시지를 가져와서 채팅창에 추가합니다.
                String question = et_msg.getText().toString().trim();
                addToChat(question, Message.SENT_BY_ME);
                et_msg.setText("");

                // API를 호출하여 GPT에게 사용자의 메시지를 전달합니다.
                callAPI(question);

                // 환영 메시지를 감춥니다.
                tv_welcome.setVisibility(View.GONE);
            }
        });

        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // "시작" 단어를 GPT에게 전달하는 함수 호출
                callAPI("시작");
                // 버튼 감추기
                start_btn.setVisibility(View.GONE);
            }
        });

        // OkHttpClient 객체를 생성합니다.
        client = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // BottomNavigationView 초기화
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // 네비게이션 아이템 클릭 이벤트 처리
                int itemId = item.getItemId();

                if (itemId == R.id.menu_home) {
                    // "GPT" 아이템 클릭 시 동작
                    Intent intent1 = new Intent(ChatGpt.this, MainActivity.class);
                    startActivity(intent1);
                } else if (itemId == R.id.menu_book) {
                    // "Book" 아이템 클릭 시 동작
                    Intent intent2 = new Intent(ChatGpt.this, BookMainActivity.class);
                    startActivity(intent2);
                } else if (itemId == R.id.menu_info) {
                    // "정보확인" 아이템 클릭 시 동작
                    Intent intent3 = new Intent(ChatGpt.this, MyInfo.class);
                    startActivity(intent3);
                }
                return true;
            }
        });
        // 초기 선택된 네비게이션 아이템 설정
        bottomNavigationView.setSelectedItemId(R.id.menu_gpt);
    }

    // 대화 내용을 채팅창에 추가하는 메소드입니다.
    void addToChat(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                recycler_view.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    // GPT의 응답을 채팅창에 추가하는 메소드입니다.
    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }

    // GPT API를 호출하여 사용자의 메시지를 전달하고 응답을 받는 메소드입니다.
    void callAPI(String question) {
        //okhttp
        messageList.add(new Message("...", Message.SENT_BY_BOT));

        //추가된 내용
        JSONArray arr = new JSONArray();
        JSONObject baseAi = new JSONObject();
        JSONObject userMsg = new JSONObject();
        try {
            //AI 속성설정
            baseAi.put("role", "user");
            baseAi.put("content", "당신은 사용자와의 10가지의 국어 문제를 통해 사용자의 국어 능력을 테스트를 해야 합니다." +
                    "점수 측정은 100점이 기준입니다. 한 문제를 틀릴 때마다 10점씩 차감합니다." +
                    "10가지의 문제는 무조껀 한개씩 진행되어야 합니다." + "사용자의 대답을 듣고 정답 인지 오답 인지 판별하여 알려주어야합니다." +
                    "사용자가 대화를 보내기 전까지 정답을 절대 알려주면 안됩니다." +
                    "그 후 두번 째 문제를 내고 사용자의 대답을 듣고, 정답 인지 오답 인지 판별 하고 점수를 계산하는 행동을 열 번째 문제까지 반복합니다." +
                    "10가지의 문제가 끝나면 사용자에게 점수를 꼭 알려주어야 합니다.");
            //유저 메세지
            userMsg.put("role", "user");
            userMsg.put("content", question);
            //array로 담아서 한번에 보낸다
            if (messages.length() == 0)
                messages.put(baseAi);
            messages.put(userMsg);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject object = new JSONObject();
        try {
            //모델명 변경
            object.put("model", "gpt-3.5-turbo");
            object.put("messages", messages);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(object.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")  //url 경로 수정됨
                .header("Authorization", "Bearer " + MY_SECRET_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        // 아래 result 받아오는 경로가 좀 수정되었다.
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");

                        // 채팅창에 추가한 뒤에, 메시지 기록에 AI의 응답을 저장합니다.
                        addResponse(result.trim());
                        JSONObject assistantMsg = new JSONObject();
                        try {
                            assistantMsg.put("role", "assistant");
                            assistantMsg.put("content", result.trim());
                            messages.put(assistantMsg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    addResponse("Failed to load response due to " + response.body().string());
                }
            }
        });
    }
}