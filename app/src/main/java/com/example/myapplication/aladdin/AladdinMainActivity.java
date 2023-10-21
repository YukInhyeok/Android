package com.example.myapplication.aladdin;

import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

public class AladdinMainActivity extends AppCompatActivity {
    private static final String BASE_URL = "http://www.aladdin.co.kr/ttb/api/ItemSearch.aspx?";
    private EditText searchEditText;
    private ImageButton searchButton;
    private Button categoryButton;

    private ListView resultListView;
    private ImageAdapter adapter;
    private ImageView closeBtn;

    private List<Item> searchResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aladdin_main);

        // 하단 메뉴바 삭제
        Utils.deleteMenuButton(this);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        resultListView = findViewById(R.id.resultListView);
        categoryButton = findViewById(R.id.categoryButton);
        closeBtn = findViewById(R.id.closeBtn);

        adapter = new ImageAdapter(this, searchResults);
        resultListView.setAdapter(adapter);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchWord = searchEditText.getText().toString();
                if(searchWord.isEmpty()){
                    Toast.makeText(getApplicationContext(), "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
                }
                else {
                    try {
                        String url = GetUrl(searchWord);
                        new AladdinOpenAPIAsyncTask().execute(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("MyApp", "예외 발생: " + e.getMessage());
                    }
                }
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // 검색 동작을 실행하는 코드를 여기에 추가
                    String searchWord = searchEditText.getText().toString();
                    if (searchWord.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            String url = GetUrl(searchWord);
                            new AladdinOpenAPIAsyncTask().execute(url);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("MyApp", "예외 발생: " + e.getMessage());
                        }
                    }
                        return true; // 이벤트가 처리되었음을 나타냄
                    }
                    return false; // 이벤트가 처리되지 않았음을 나타냄
                }
        });

        categoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CharSequence[] categories = {"어휘력", "독해력", "문법"};

                AlertDialog.Builder builder = new AlertDialog.Builder(AladdinMainActivity.this);
                builder.setTitle("카테고리 선택");
                builder.setItems(categories, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedCategory = categories[which].toString();
                        // TODO: 여기서 선택된 카테고리로 검색을 수행하거나 다른 작업을 수행하세요.
                        Toast.makeText(getApplicationContext(), selectedCategory + " 선택됨", Toast.LENGTH_SHORT).show();

                        // 서브 카테고리 목록 표시
                        showSubcategories(selectedCategory);
                    }
                });
                builder.show();
            }
        });
    }
    private void showSubcategories(String category) {
        try {
            // AssetManager 객체 가져오기
            AssetManager manager = getAssets();

            // Book_list.json 파일 열기
            InputStream is = manager.open("Book_list.json");

            // InputStream에서 문자열 가져오기
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            // JSON 문자열 파싱하기
            JSONObject jsonObject = new JSONObject(jsonString);

            JSONArray subcategoriesArray;
            if(jsonObject.has(category)){
                subcategoriesArray=jsonObject.getJSONArray(category);
            }else{
                throw new JSONException("Category not found in the json file");
            }

            final CharSequence[] subcategories=new CharSequence[subcategoriesArray.length()];
            for(int i=0;i<subcategoriesArray.length();i++){
                subcategories[i]=subcategoriesArray.getString(i);
            }

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(category + "추천 도서");
            dialogBuilder.setItems(subcategories, (dialog, which) -> {
                String selectedSubcategory = subcategories[which].toString();
                performSearch(selectedSubcategory);
            });
            AlertDialog alertDialogObject = dialogBuilder.create();
            alertDialogObject.show();

        } catch (IOException | JSONException e) {
            Log.e(TAG,"Error reading/parsing the json file",e);
        }
    }


    private void performSearch(String keyword) {
        try {
            String url = GetUrl(keyword);
            new AladdinOpenAPIAsyncTask().execute(url);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MyApp", "예외 발생: " + e.getMessage());
        }
    }
    private class AladdinOpenAPIAsyncTask extends AsyncTask<String, Void, List<Item>> {
        @Override
        protected List<Item> doInBackground(String... params) {
            try {
                String xmlUrl = params[0];
                Log.d("MyApp", "XML URL: " + xmlUrl);
                List<Item> items = new ArrayList<>();
                AladdinOpenAPIHandler api = new AladdinOpenAPIHandler(items);
                api.parseXml(xmlUrl);
                return items;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("MyApp", "예외 발생: " + e.getMessage());
                return null;
            }
        }
        @Override
        protected void onPostExecute(List<Item> items) {
            super.onPostExecute(items);
            if (items != null) {
                Log.d("MyApp", "검색 결과 개수: " + items.size());

                searchResults.clear();
                searchResults.addAll(items);

                adapter.notifyDataSetChanged();  // ListView에 데이터 변경 알림

                if(items.size()==0){
                    Toast.makeText(getApplicationContext(), "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public String GetUrl(String searchWord) throws Exception {
        Map<String, String> hm = new HashMap<String, String>();
        hm.put("ttbkey", "ttbddolmin51551109001");
        hm.put("Query", URLEncoder.encode(searchWord, "UTF-8"));
        hm.put("QueryType", "Title");
        hm.put("MaxResults", "20");
        hm.put("start", "1");
        hm.put("SearchTarget", "Book");
        hm.put("output", "xml");

        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = hm.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            String val = hm.get(key);
            sb.append(key).append("=").append(val).append("&");
        }
        return BASE_URL + sb.toString();
    }
}
