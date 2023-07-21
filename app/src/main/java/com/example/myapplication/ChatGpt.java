package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.myapplication.socket.SocketClient;
import com.github.mikephil.charting.data.RadarEntry;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private FirebaseFirestore db;

    Button start_btn;


    List<Message> messageList;
    MessageAdapter messageAdapter;
    JSONArray messages = new JSONArray();


    JSONArray assistantMessages = new JSONArray();

    private Button Btn1;


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

        Btn1 = findViewById(R.id.btn1);

        Btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int lastScore = findLastScoreFromAssistantMsg(assistantMessages);

                if (lastScore != -1) {
                    // 찾은 마지막 점수를 사용하여 필요한 작업 수행
                    // 예를 들어, 결과를 TextView에 반영:
                    Log.d("test score","마지막 점수: " + lastScore);
                } else {
                    // 점수를 찾지 못한 경우 처리
                    Log.d("test score","점수를 찾지 못했습니다.");
                }
            }
        });

        // 전송 버튼 클릭 이벤트 핸들러를 등록합니다.
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 기존 코드
                String question = et_msg.getText().toString().trim();
                addToChat(question, Message.SENT_BY_ME);
                et_msg.setText("");

                // 추가되는 코드
//                socketClient.sendToPythonServer(question);

                // API를 호출하여 GPT에게 사용자의 메시지를 전달합니다.
                callAPI(question);

                // 환영 메시지를 감춥니다.
                tv_welcome.setVisibility(View.GONE);
            }
        });


        start_btn.setOnClickListener(new View.OnClickListener() {
            // 파일에서 JSON 객체를 로드합니다.
            JSONArray jsonArray = loadJsonArrayFromFile("Question.json");
            @Override
            public void onClick(View view) {
                if (jsonArray != null && jsonArray.length() > 0) {
                    // 랜덤 인덱스를 생성합니다.
                    Random random = new Random();
                    int index = random.nextInt(jsonArray.length());

                    try {
                        // JSON 배열에서 랜덤한 요소를 가져옵니다.
                        JSONObject randomQuestion = jsonArray.getJSONObject(index);

                        // 선택된 JSON 객체의 내용을 화면에 출력합니다.
                        // 예시: 여기에서는 "content"라는 키를 사용하여 값을 가져옵니다.
                        String questionContent = randomQuestion.getString("content");
                        addResponse("문제: " + questionContent);

                        // 이 내용을 GPT에게 전달하는 함수 호출
                        callAPI(questionContent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 파일 로드에 실패한 경우 또는 배열의 길이가 0인 경우 오류 메시지를 출력합니다.
                    addResponse("Failed to load or empty Questions. Please check the file.");
                }
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
        if (messageList.size() > 0) {
            messageList.remove(messageList.size() - 1);
        }
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
            baseAi.put("content", "당신은 국어선생님 입니다. 객관식 문제 5가지를 출제해야합니다. 문제는 100점 만점이며 한 문제당 20점의 배점을 가지고 있습니다. 사용자의 점수를 판단하세요. 반드시 문제를 출제할 때에는 절대 답을 알려주어서는 안되고, 사용자가 답을 입력한 후에 정답을 알려줍니다. 사용자의 답이 정답과 다르다면 오답 처리를 해야합니다.");
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

                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");

                        // 채팅창에 추가한 뒤에, 메시지 기록에 AI의 응답을 저장합니다.
                        addResponse(result.trim());
                        JSONObject assistantMsg = new JSONObject();
                        try {
                            assistantMsg.put("role", "assistant");
                            assistantMsg.put("content", result.trim());
                            messages.put(assistantMsg);
                            assistantMessages.put(assistantMsg);
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

    // 추가
    public JSONArray loadJsonArrayFromFile(String Question) {
        try {
            InputStream is = getAssets().open(Question);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            return new JSONArray(jsonStr);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void fetchData(int lastScore, MyInfo.FirestoreCallback callback) {
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

                            // lastScore를 파이어베이스에 저장
                            if (lastScore != -1) {
                                db.collection("Chart").add(
                                        new RadarEntry((float) lastScore, 0) // 임의의 인덱스값 (0)을 사용하였습니다.
                                );
                            }

                            callback.onDataLoaded(entries);
                        } else {
                            Log.e("MyInfo", "Error fetching data", task.getException());
                        }
                    }
                });
    }


    // assistantMsg 배열값을 사용하면 될듯
    private int findScoreFromAssistantMsg(String content) {
        int lastScore = -1;
        int index = content.lastIndexOf("점");

        if (index != -1) {
            String subStrBeforePoint = content.substring(0, index);
            Matcher m = Pattern.compile("\\d+").matcher(subStrBeforePoint);

            while (m.find()) {
                lastScore = Integer.parseInt(m.group());
            }
        }
        return lastScore;
    }


    private int findLastScoreFromAssistantMsg(JSONArray assistantMessages) {
        int lastScore = -1;
        for (int i = assistantMessages.length() - 1; i >= 0; i--) {
            try {
                JSONObject msg = assistantMessages.getJSONObject(i);
                if (msg.getString("role").equals("assistant")) {
                    String content = msg.getString("content");
                    int score = findScoreFromAssistantMsg(content);
                    if (score != -1) { // 점수를 찾았을 때만 기록
                        lastScore = score;
                        break; // 내부 반복문 종료
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return lastScore;
    }


}
