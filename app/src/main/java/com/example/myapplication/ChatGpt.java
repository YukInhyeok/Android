package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.example.myapplication.model.Message;
import com.google.android.gms.tasks.*;
import com.google.firebase.firestore.*;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatGpt extends AppCompatActivity {
    // API
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String MY_SECRET_KEY = BuildConfig.GPT_API;
    RecyclerView recycler_view;
    EditText et_msg;
    Button btn_send;
    Button InteractiveBtn;    // 대화형
    Button QuestionBtn;       // 문제형
    ImageButton finishBtn;
    TextView interactiveText, questionText;
    Button continueBtn;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    JSONArray messages = new JSONArray();
    JSONArray assistantMessages = new JSONArray();
    OkHttpClient client;
    // 독해, 문해, 어휘 저장 변수
    private String ability;
    // 요일 변수
    private String Week;
    private FirebaseFirestore db;
    private Handler mHandler;
    private Runnable mRunnable;
    private int ans = 0;
    private int wrong_ans = 0;
    private int Switch = 0;

    private String prompt_lit = "나와 이야기 하며 문해력 점수를 판단해줘. 시작";
    private String prompt_read = "당신은 국어선생님 입니다. 당신은 학생의 독해력을 테스트 해야합니다. 학생이 최근 읽은 책이 무엇인지 물어보고 그 내용을 잘 이해했는지 확인해 주세요. 시작";
    private String prompt_voc = "당신은 국어선생님 입니다. 당신은 학생의 어휘력을 테스트 해야합니다. 당신이 생각하기에 어려운 한국어 단어를 말하고 뜻을 물어봐주세요. 시작";


    //네비게이션바 설정

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gpt_main);
        // 하단 메뉴바 삭제
        Utils.deleteMenuButton(this);

        // firebase 초기화
        db = FirebaseFirestore.getInstance();

        recycler_view = findViewById(R.id.recycler_view);
        et_msg = findViewById(R.id.et_msg);
        btn_send = findViewById(R.id.btn_send);
        InteractiveBtn = findViewById(R.id.InteractiveBtn);
        QuestionBtn = findViewById(R.id.QuestionBtn);
        finishBtn = findViewById(R.id.finish_Btn);
        continueBtn = findViewById(R.id.continue_Btn);
        interactiveText = findViewById(R.id.InteractiveText);
        questionText = findViewById(R.id.QuestionText);

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
        Log.d("GPT", "Switch: " + Switch);

        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Switch == 1){
                    Log.d("GPT", "대화형 ability: " + ability);
                    int score = mark_interactive(assistantMessages);
                    DocumentReference dateRef = db.collection("Chart").document(ability);
                    dateRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Integer value = documentSnapshot.contains("value") ? documentSnapshot.getLong("value").intValue() : 0;
                                    Integer currentCount = documentSnapshot.contains("count") ? documentSnapshot.getLong("count").intValue() : 0;

                                    long newValue = (score + value) / 2;
                                    Map<String, Object> newData = new HashMap<>(documentSnapshot.getData());
                                    newData.put("value", Math.round(newValue));
                                    dateRef.set(newData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d("Firestore", "Value and count were successfully updated!");
                                                    // 업데이트 후에 평균을 계산하고 저장
                                                    saveWeeklyAverage();
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
                    finish();
                }

                else if (Switch == 2) {
                    Log.d("GPT", "문제형 ability: " + ability);
                    DocumentReference dateRef = db.collection("Chart").document(ability);
                    dateRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Integer ans = documentSnapshot.contains("ans") ? documentSnapshot.getLong("ans").intValue() : 0;
                                    Integer currentCount = documentSnapshot.contains("count") ? documentSnapshot.getLong("count").intValue() : 0;

                                    float newValue = (float) ans / (currentCount) * 100;

                                    // 데이터 갱신
                                    Map<String, Object> newData = new HashMap<>(documentSnapshot.getData());
                                    newData.put("value", Math.round(newValue));

                                    dateRef.set(newData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d("Firestore", "Value and count were successfully updated!");
                                                    // 업데이트 후에 평균을 계산하고 저장
                                                    saveWeeklyAverage();
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

                    finish();
                } else {

                    finish();
                }
            }
        });



        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Switch == 2) {
                    String question = et_msg.getText().toString().trim();
                    addToChat(question, Message.SENT_BY_ME);
                    et_msg.setText("");

                    callAPI_V2(question);
                    continueBtn.setVisibility(View.VISIBLE);
                }
                // 필요없는것 같음
                else if (Switch == 1) {
                    String question = et_msg.getText().toString().trim();
                    addToChat(question, Message.SENT_BY_ME);
                    et_msg.setText("");

                    callAPI(question);
                    continueBtn.setVisibility(View.GONE);
                }
            }
        });

        InteractiveBtn.setOnClickListener(new View.OnClickListener() {
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

                        if (vocabularyScore == minScore) lowScoringSubjects.add(prompt_voc);
                        if (literacyScore == minScore) lowScoringSubjects.add(prompt_lit);
                        if (readingScore == minScore) lowScoringSubjects.add(prompt_read);

                        InteractiveBtn.setVisibility(View.GONE);
                        QuestionBtn.setVisibility(View.GONE);
                        continueBtn.setVisibility(View.GONE);
                        interactiveText.setVisibility(View.GONE);
                        questionText.setVisibility(View.GONE);
                        Switch = 1;

                        if (lowScoringSubjects.size() > 0) {
                            // 랜덤하게 선택한 과목의 문제를 출제
                            Random random = new Random();
                            int randomIndex = random.nextInt(lowScoringSubjects.size());
                            String selectedPrompt = lowScoringSubjects.get(randomIndex);
                            callAPI(selectedPrompt);

                            // 선택된 과목을 ability 변수에 할당
                            if (selectedPrompt.equals(prompt_voc)) {
                                ability = "vocabulary";
                            } else if (selectedPrompt.equals(prompt_lit)) {
                                ability = "literacy";
                            } else if (selectedPrompt.equals(prompt_read)) {
                                ability = "read";
                            }
                        }
                    }
                });
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

                        if (lowScoringSubjects.size() > 0) {
                            String selectedSubjectFile = lowScoringSubjects.get(random.nextInt(lowScoringSubjects.size()));

                            // 선택된 파일에서 JSON 객체 로드하기
                            JSONArray jsonArray = loadJsonArrayFromFile(selectedSubjectFile);

                            if (jsonArray != null && jsonArray.length() > 0) {
                                int index = random.nextInt(jsonArray.length());

                                try {
                                    JSONObject randomQuestion = jsonArray.getJSONObject(index);
                                    String questionContent = randomQuestion.getString("content");
                                    addResponse("문제: " + questionContent);
                                    callAPI_V2(questionContent);  // 이 내용을 GPT에게 전달하는 함수 호출
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                addResponse("Failed to load or empty Questions. Please check the file.");
                            }
                            ability = selectedSubjectFile.replace(".json", "").toLowerCase();
                        } else {
                            addResponse("'No subjects found with minimum score.");
                        }

                        // 버튼 감추기
                        InteractiveBtn.setVisibility(View.GONE);
                        QuestionBtn.setVisibility(View.GONE);
                        interactiveText.setVisibility(View.GONE);
                        questionText.setVisibility(View.GONE);
                        Switch = 2;
                    }
                });
            }
        });

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mark_question(assistantMessages);
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

        try {
            // system message
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "당신은 한국어 선생님입니다. 유저가 문법에 맞지 않거나 주제와 상관없는 이야기를 할 때 점수를 20점씩 차감합니다. 항상 \"시작\"이라는 단어를 들으면 점수를 초기화 해야하며, 6마디의 대화를 나눈 뒤에는 반드시 점수를 알려주어야 합니다.");

            JSONObject userMessage1 = new JSONObject();
            userMessage1.put("role", "user");
            userMessage1.put("content", "시작");

            JSONObject assistantMsg1 = new JSONObject();
            assistantMsg1.put("role", "assistant");
            assistantMsg1.put("content", "1. 안녕하세요 당신의 어휘력을 평가하겠습니다. 금일 날씨는 어떤가요?");

            JSONObject userMessage2 = new JSONObject();
            userMessage2.put("role", "user");
            userMessage2.put("content", "오늘은 비가 많이 와");

            JSONObject assistantMsg2 = new JSONObject();
            assistantMsg2.put("role", "assistant");
            assistantMsg2.put("content", "2. 아, 그렇군요. 비가 많이 오면서 날씨가 추워진 것 같네요. 함께 어울릴만한 활동이 뭐가 있을까요?");

            JSONObject userMessage3 = new JSONObject();
            userMessage3.put("role", "user");
            userMessage3.put("content", "ㅁㄹ");

            JSONObject assistantMsg3 = new JSONObject();
            assistantMsg3.put("role", "assistant");
            assistantMsg3.put("content", "3. [죄송하지만, \"ㅁㄹ\"은 이해할 수 없는 단어입니다. 점수를 20점 차감 하겠습니다. 현재 당신의 점수는 80점입니다.] \n" +
                    "그러면 좋아하는 영화가 있나요? 있다면 어떤 장르도 좋아하나요?");

            JSONObject userMessage4 = new JSONObject();
            userMessage4.put("role", "user");
            userMessage4.put("content", "마블영화 좋아해");

            JSONObject assistantMsg4 = new JSONObject();
            assistantMsg4.put("role", "assistant");
            assistantMsg4.put("content", "4. 저도 마블 영화를 좋아합니다! 마블 시리즈 중에서 가장 좋아하는 영화는 무엇인가요?");

            JSONObject userMessage5 = new JSONObject();
            userMessage5.put("role", "user");
            userMessage5.put("content", "아이언맨");

            JSONObject assistantMsg5 = new JSONObject();
            assistantMsg5.put("role", "assistant");
            assistantMsg5.put("content", "5. 좋은 선택입니다. 아이언맨은 정말 재치 있는 캐릭터이죠. 아이언맨이 왜 좋은지 이유를 들을 수 있을까요?");

            JSONObject userMessage6 = new JSONObject();
            userMessage6.put("role", "user");
            userMessage6.put("content", "멋있잖아");

            JSONObject assistantMsg6 = new JSONObject();
            assistantMsg6.put("role", "assistant");
            assistantMsg6.put("content", "[종료] 평가가 종료되었습니다. 당신의 최종 점수는 [80점] 입니다. 고생하셨습니다.");

            JSONObject userMessage7 = new JSONObject();
            userMessage7.put("role", "user");
            userMessage7.put("content", "시작");

            JSONObject assistantMsg7 = new JSONObject();
            assistantMsg7.put("role", "assistant");
            assistantMsg7.put("content", "1. 안녕하세요 오늘 하루는 어땠나요?");

            JSONObject userMessage8 = new JSONObject();
            userMessage8.put("role", "user");
            userMessage8.put("content", "오늘은 날씨가 좋아서 오랜만에 산책하며 놀았어. 재미있었어");

            JSONObject assistantMsg8 = new JSONObject();
            assistantMsg8.put("role", "assistant");
            assistantMsg8.put("content", "2. 그래요, 좋은 날씨에 산책을 하면 정말 기분이 좋겠어요. 어디를 산책하며 놀았나요?");

            JSONObject userMessage9 = new JSONObject();
            userMessage9.put("role", "user");
            userMessage9.put("content", "배고파");

            JSONObject assistantMsg9 = new JSONObject();
            assistantMsg9.put("role", "assistant");
            assistantMsg9.put("content", "3. [질문과 맞지 않는 대답을 하셨네요. 점수를 20점 차감하도록 하겠습니다. 현재 당신의 점수는 80점 입니다.]\\n산책하면서 배고파진다면, 주변에 맛있는 음식점이나 카페가 있을 수도 있어요. 근처에 어떤 음식을 좋아하시나요? 저는 중식이나 양식을 좋아해요.");

            JSONObject userMessage10 = new JSONObject();
            userMessage10.put("role", "user");
            userMessage10.put("content", "나는 한식 좋아해");

            JSONObject assistantMsg10 = new JSONObject();
            assistantMsg10.put("role", "assistant");
            assistantMsg10.put("content", "4. 아, 저도 한식을 좋아해요! 한식 중에서 어떤 음식을 가장 좋아하시나요? 저는 불고기나 된장찌개를 좋아합니다.");

            JSONObject userMessage11 = new JSONObject();
            userMessage11.put("role", "user");
            userMessage11.put("content", "스시");

            JSONObject assistantMsg11 = new JSONObject();
            assistantMsg11.put("role", "assistant");
            assistantMsg11.put("content", "5. [스시는 한식 요리가 아니에요. 점수를 20점 차감하겠습니다. 현재 당신의 점수는 60점 입니다.] \n" + "스시를 왜 좋아하나요? ");

            JSONObject userMessage12 = new JSONObject();
            userMessage12.put("role", "user");
            userMessage12.put("content", "그냥 맛있잖아");

            JSONObject assistantMsg12 = new JSONObject();
            assistantMsg12.put("role", "assistant");
            assistantMsg12.put("content", "[종료] 평가가 종료되었습니다. 당신의 최종 점수는 [60점] 입니다. 고생하셨습니다.");

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", question);

            if (messages.length() == 0) {
                messages.put(systemMessage);
                messages.put(userMessage1);
                messages.put(assistantMsg1);
                messages.put(userMessage2);
                messages.put(assistantMsg2);
                messages.put(userMessage3);
                messages.put(assistantMsg3);
                messages.put(userMessage4);
                messages.put(assistantMsg4);
                messages.put(userMessage5);
                messages.put(assistantMsg5);
                messages.put(userMessage6);
                messages.put(assistantMsg6);
                messages.put(userMessage7);
                messages.put(assistantMsg7);
                messages.put(userMessage8);
                messages.put(assistantMsg8);
                messages.put(userMessage9);
                messages.put(assistantMsg9);
                messages.put(userMessage10);
                messages.put(assistantMsg10);
                messages.put(userMessage11);
                messages.put(assistantMsg11);
                messages.put(userMessage12);
                messages.put(assistantMsg12);
            }
            messages.put(userMsg);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject object = new JSONObject();
        try {
            //모델명
            object.put("model", "gpt-4");
            object.put("messages", messages);
            object.put("temperature",0.4);

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
            object.put("model", "gpt-4");
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

                    } else if (content.contains("오답")) { // 오답을 찾았을 때만 기록
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

        long test = c.getTimeInMillis() - System.currentTimeMillis();
        Log.d("test", "남은시간: " + test);
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
                                document.getReference().update("count", 0);
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
                                Log.d("GPT", "sum: " + sum);
                                Log.d("GPT", "count: " + count);
                            }

                            if (count > 0) {
                                double average = (double) sum / count;
                                Map<String, Object> data = new HashMap<>();
                                data.put("average", Math.round(average));

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