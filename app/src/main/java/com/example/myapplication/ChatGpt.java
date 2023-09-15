package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.book.BookMainActivity;
import com.example.myapplication.model.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    RecyclerView recycler_view;
    TextView tv_welcome;
    EditText et_msg;
    ImageButton btn_send;

    Button InteractiveBtn;    // 대화형
    Button QuestionBtn;       // 문제형
    Button finishBtn;
    Button continueBtn;

    List<Message> messageList;
    MessageAdapter messageAdapter;
    JSONArray messages = new JSONArray();

    JSONArray assistantMessages = new JSONArray();

    // 독해, 문해, 어휘 저장 변수
    private String ability;
    // 요일 변수
    private String Week;

    private FirebaseFirestore db;

    private Handler mHandler;
    private Runnable mRunnable;

    private int ans = 0;
    private int wrong_ans = 0;

    // API
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client;
    private static final String MY_SECRET_KEY = "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";

    //네비게이션바 설정
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gpt_main);
        // 하단 메뉴바 삭제
        Utils.deleteMenuButton(this);

        // firebase 초기화
        db = FirebaseFirestore.getInstance();

        recycler_view = findViewById(R.id.recycler_view);
        tv_welcome = findViewById(R.id.tv_welcome);
        et_msg = findViewById(R.id.et_msg);
        btn_send = findViewById(R.id.btn_send);
        InteractiveBtn = findViewById(R.id.InteractiveBtn);
        QuestionBtn = findViewById(R.id.QuestionBtn);
        finishBtn = findViewById(R.id.finish_Btn);
        continueBtn = findViewById(R.id.continue_Btn);

        recycler_view.setHasFixedSize(true);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        recycler_view.setLayoutManager(manager);

        // 메시지 리스트 어댑터 초기화
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recycler_view.setAdapter(messageAdapter);

        // 자정이 지나면 chart 값 0 메서드 호출
        resetValuesAtMidnight();

        // 주간 데이터
        Week = getDayOfWeek();

        //종료 버튼 감추기
        finishBtn.setVisibility(View.GONE);

        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int lastScore = mark_question(assistantMessages);
                Log.d("GPT","정답 개수: "+ ans);
                Log.d("GPT","ability: "+ ability);

                DocumentReference dateRef = db.collection("Chart").document(ability);

                if (lastScore != -1) {
                    dateRef
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Integer ans = documentSnapshot.contains("ans") ? documentSnapshot.getLong("ans").intValue() : 0;
                                    Integer currentCount = documentSnapshot.contains("count") ? documentSnapshot.getLong("count").intValue() : 0;
                                    Integer wrong_ans = documentSnapshot.contains("wrong_ans") ? documentSnapshot.getLong("wrong_ans").intValue() : 0;
                                    // 날짜 변경 여부 확인
                                    String last_updated = documentSnapshot.contains("last_updated") ? documentSnapshot.getString("last_updated") : "";
                                    String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

                                    // 새로운 평균 계산
//                                    currentCount = currentCount + 1;

                                    float newValue = (float)ans / (currentCount) * 100;
                                    Log.d("GPT", "Val_1: "+ newValue);

                                    // 날짜가 변경되었을 경우 count를 0으로 초기화
                                    if (!last_updated.equals(today)) {
                                        Log.d("GPT","ability: "+ today);
                                        Log.d("GPT","ability: "+ last_updated);
                                        currentCount = 0;
                                        ans = 0;
                                        wrong_ans = 0;
                                    }
                                    Log.d("GPT", "Val_2: "+ newValue);
                                    // 데이터 갱신
                                    Map<String, Object> newData = new HashMap<>(documentSnapshot.getData());
                                    newData.put("value", newValue);
                                    newData.put("count", currentCount);
                                    newData.put("ans", ans);
                                    newData.put("wrong_ans", wrong_ans);
                                    newData.put("last_updated", today); // 오늘 날짜 추가
                                    dateRef
                                            .set(newData)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d("Firestore", "Value and count were successfully updated!");

                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w("Firestore", "Error updating data", e);
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("Firestore", "Error getting document", e);
                                }
                            });

                    Intent intent = new Intent(ChatGpt.this, MainActivity.class);
                    startActivity(intent);

                } else {
                    // 점수를 찾지 못한 경우
                    Log.d("test score", "점수를 찾지 못했습니다.");
                    Toast.makeText(getApplicationContext(), "점수를 찾을 수 없습니다. 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                }
                saveWeeklyAverage();
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String question = et_msg.getText().toString().trim();
                addToChat(question, Message.SENT_BY_ME);
                et_msg.setText("");

                callAPI(question);

                tv_welcome.setVisibility(View.GONE);
                finishBtn.setVisibility(View.VISIBLE);
                continueBtn.setVisibility(View.VISIBLE);
            }
        });


        InteractiveBtn.setOnClickListener(new View.OnClickListener() {
            JSONArray jsonArray = loadJsonArrayFromFile("Literacy.json");
            @Override
            public void onClick(View view) {
                if (jsonArray != null && jsonArray.length() > 0) {
                    // 랜덤 인덱스 생성
                    Random random = new Random();
                    int index = random.nextInt(jsonArray.length());

                    try {
                        JSONObject randomQuestion = jsonArray.getJSONObject(index);
                        Log.d("question", "index: " + randomQuestion);

                        String questionContent = randomQuestion.getString("content");
                        addResponse("문제: " + questionContent);
                        Log.d("question", "문제: " + questionContent);

                        callAPI(questionContent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    addResponse("Failed to load or empty Questions. Please check the file.");
                }
//                VocabularyBtn.setVisibility(View.GONE);
                InteractiveBtn.setVisibility(View.GONE);
                QuestionBtn.setVisibility(View.GONE);
                ability = "literacy";
            }
        });


        QuestionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // 각 과목별 점수 읽기
                Task<DocumentSnapshot> taskVocabulary = db.collection("Chart").document("vocabulary").get();
                Task<DocumentSnapshot> taskLiteracy = db.collection("Chart").document("literacy").get();
                Task<DocumentSnapshot> taskReading = db.collection("Chart").document("read").get();

                Tasks.whenAllSuccess(taskVocabulary, taskLiteracy, taskReading).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
                    @Override
                    public void onSuccess(List<Object> objects) {
                        int vocabularyScore = ((DocumentSnapshot) objects.get(0)).getLong("value").intValue();
                        int literacyScore = ((DocumentSnapshot) objects.get(1)).getLong("value").intValue();
                        int readingScore = ((DocumentSnapshot) objects.get(2)).getLong("value").intValue();

                        // 가장 낮은 점수 찾기
                        int minScore = Math.min(vocabularyScore, Math.min(literacyScore, readingScore));

                        List<String> lowScoringSubjects = new ArrayList<>();

                        if (vocabularyScore == minScore) lowScoringSubjects.add("Vocabulary.json");
                        if (literacyScore == minScore) lowScoringSubjects.add("Literacy.json");
                        if (readingScore == minScore) lowScoringSubjects.add("Read.json");

                        // 가장 낮은 점수의 과목 중 하나를 랜덤하게 선택하기
                        Random random = new Random();

                        if(lowScoringSubjects.size()>0){
                            String selectedSubjectFile = lowScoringSubjects.get(random.nextInt(lowScoringSubjects.size()));

                            // 선택된 파일에서 JSON 객체 로드하기
                            JSONArray jsonArray = loadJsonArrayFromFile(selectedSubjectFile);

                            if (jsonArray != null && jsonArray.length() > 0) {
                                int index = random.nextInt(jsonArray.length());

                                try {
                                    JSONObject randomQuestion = jsonArray.getJSONObject(index);
                                    String questionContent = randomQuestion.getString("content");
                                    addResponse("문제: " + questionContent);
                                    callAPI(questionContent);  // 이 내용을 GPT에게 전달하는 함수 호출
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                addResponse("Failed to load or empty Questions. Please check the file.");
                            }
                            ability = selectedSubjectFile.replace(".json","").toLowerCase();
                        }else{
                            addResponse("'No subjects found with minimum score.");
                        }

                        // 버튼 감추기
                        InteractiveBtn.setVisibility(View.GONE);
                        QuestionBtn.setVisibility(View.GONE);
                    }
                });
            }
        });

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int ans = mark_question(assistantMessages);
                Log.d("GPT","ANS: "+ ans);

                DocumentReference dateRef = db.collection("Chart").document(ability);

                dateRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Integer currentCount = documentSnapshot.contains("count") ? documentSnapshot.getLong("count").intValue() : 0;
                        currentCount++;

                        Map<String, Object> newData = new HashMap<>(documentSnapshot.getData());
                        newData.put("count", currentCount);

                        dateRef.set(newData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("Firestore", "Count was successfully updated!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("Firestore", "Error updating data", e);
                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Error getting document", e);
                    }
                });
                callAPI_V2("다음문제를 내주세요");
                continueBtn.setVisibility(View.GONE);
            }

        });

        // OkHttpClient 객체 생성
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

    // 대화 내용을 채팅창에 추가
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

    // GPT의 응답을 채팅창에 추가

    void addResponse(String response) {
        if (messageList.size() > 0) {
            messageList.remove(messageList.size() - 1);
        }
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI(String question) {
        //okhttp
        messageList.add(new Message("...", Message.SENT_BY_BOT));

        JSONObject baseAi = new JSONObject();
        JSONObject userMsg = new JSONObject();
        try {
            //AI 속성설정
            baseAi.put("role", "user");
            baseAi.put("content", "당신은 국어선생님 입니다. 주어진 지문을 가지고 객관식 문제 1문제를 출제해야합니다. 반드시 문제를 출제할 때에는 절대 답을 알려주어서는 안되고, 사용자가 답을 입력한 후에 정답을 알려줍니다. 사용자의 답이 정답과 다르다면 오답 처리를 해야합니다. 당신은 정답과 오답을 항상 '정답입니다.' 또는 '오답입니다.' 라는 문장으로만 판단할 수 있습니다.");
            //유저 메세지
            userMsg.put("role", "user");
            userMsg.put("content", question);

            if (messages.length() == 0)
                messages.put(baseAi);
            messages.put(userMsg);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject object = new JSONObject();
        try {
            //모델명
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

                        // 배열에 AI 답변 저장
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

    // 문제형
    void callAPI_V2(String question) {
        //okhttp
        messageList.add(new Message("...", Message.SENT_BY_BOT));

        JSONObject baseAi = new JSONObject();
        JSONObject userMsg = new JSONObject();
        try {
            //AI 속성설정
            baseAi.put("role", "user");
            baseAi.put("content", "당신은 국어선생님 입니다. 주어진 지문을 가지고 객관식 문제 1문제를 출제해야합니다. 반드시 문제를 출제할 때에는 절대 답을 알려주어서는 안되고, 사용자가 답을 입력한 후에 정답을 알려줍니다. 사용자의 답이 정답과 다르다면 오답 처리를 해야합니다. 당신은 정답과 오답을 항상 '정답입니다.' 또는 '오답입니다.' 라는 문장으로만 판단할 수 있습니다.");
            //유저 메세지
            userMsg.put("role", "user");
            userMsg.put("content", question);

            if (messages.length() == 0)
                messages.put(baseAi);
            messages.put(userMsg);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject object = new JSONObject();
        try {
            //모델명
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

                        // 배열에 AI 답변 저장
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

    //대화형 관련
    // 최종 점수 관련 메서드
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

    // assistantMsg에서 점수 추출
    private int mark_interactive(JSONArray assistantMessages) {
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


    //문제형 관련
    private int mark_question(JSONArray assistantMessages) {
        DocumentReference dateRef = db.collection("Chart").document(ability);

        for (int i = assistantMessages.length() - 1; i >= 0; i--) {
            try {
                JSONObject msg = assistantMessages.getJSONObject(i);
                if (msg.getString("role").equals("assistant")) {
                    String content = msg.getString("content");
                    if (content.contains("정답")) { // 정답을 찾았을 때만 기록
                        ans += 1;
                        updateFirestoreField(dateRef, "ans", ans);
                        break; // 내부 반복문 종료

                    } else if(content.contains("오답")) { // 오답을 찾았을 때만 기록
                        wrong_ans += 1;
                        updateFirestoreField(dateRef, "wrong_ans", wrong_ans);
                        break; // 내부 반복문 종료
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return ans;
    }

    private void updateFirestoreField(DocumentReference docRef, String fieldName, int value) {
        docRef.update(fieldName, FieldValue.increment(1))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore", fieldName + " was successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Error updating data", e);
                    }
                });
    }


    // 자정이 지나면 Chart 컬렉션의 value값 0으로 설정

    private long getMillisUntilMidnight() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTimeInMillis() - System.currentTimeMillis();
    }

    private void resetChartValues() {
        db.collection("Chart")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().update("value", 0);
                                document.getReference().update("ans", 0);
                                document.getReference().update("wrong_ans", 0);
                            }
                        } else {
                            Log.w("Firestore", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void resetValuesAtMidnight() {
        mHandler = new Handler(Looper.getMainLooper());
        mRunnable = new Runnable() {
            @Override
            public void run() {
                resetChartValues();
                mHandler.postDelayed(this, TimeUnit.DAYS.toMillis(1));
            }
        };
        mHandler.postDelayed(mRunnable, getMillisUntilMidnight());
    }

    //주간 데이터
    private String getDayOfWeek() {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_WEEK);
        return dayToString(day);
    }
    private String dayToString(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return "Mon";
            case Calendar.TUESDAY:
                return "Tues";
            case Calendar.WEDNESDAY:
                return "Wed";
            case Calendar.THURSDAY:
                return "Thurs";
            case Calendar.FRIDAY:
                return "Fri";
            case Calendar.SATURDAY:
                return "Satur";
            case Calendar.SUNDAY:
                return "Sun";
            default:
                return "";
        }
    }
    private void saveWeeklyAverage() {
        db.collection("Chart")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int sum = 0;
                            int count = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                sum += document.getLong("value");
                                count++;
                            }

                            if (count > 0) {
                                int average = (int) sum / count;
                                Map<String, Object> data = new HashMap<>();
                                data.put("average", average);

                                DocumentReference weekDocRef = db.collection("WeekChart").document(Week);
                                    weekDocRef.update(data)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d("Firestore", "Weekly average was successfully written!");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w("Firestore", "Error adding document", e);
                                            }
                                        });

                            } else {
                                Log.d("Firestore", "No documents found with a value");
                            }
                        } else {
                            Log.w("Firestore", "Error getting documents.", task.getException());
                        }
                    }
                });
    }
}
