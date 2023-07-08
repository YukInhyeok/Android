package com.example.myapplication.book;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.R;

public class Fragment1 extends Fragment {
    EditText editText;
    EditText editText2;
    EditText editText3;
    OnDatabaseCallback callback;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        callback = (OnDatabaseCallback)getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_fragment1, container, false);
        editText = rootView.findViewById(R.id.editText1);
        editText2 = rootView.findViewById(R.id.editText2);
        editText3 = rootView.findViewById(R.id.editText3);
        Button button = rootView.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                String name = editText.getText().toString();
                String author = editText2.getText().toString();
                String contents = editText3.getText().toString();

                callback.insert(name, author, contents);
                Toast.makeText(getContext(), "책 정보를 추가했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        return rootView;
    }
}