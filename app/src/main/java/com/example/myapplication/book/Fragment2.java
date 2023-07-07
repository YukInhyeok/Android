package com.example.myapplication.book;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;

import java.util.ArrayList;

public class Fragment2 extends Fragment {
    RecyclerView recyclerView;
    BookAdapter adapter;
    OnDatabaseCallback callback;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        callback = (OnDatabaseCallback) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_fragment2, container, false);
        recyclerView = rootView.findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new BookAdapter();
        recyclerView.setAdapter(adapter);
        ArrayList<BookInfo> result = callback.selectAll();
        adapter.setItems(result);

        adapter.setOnItemClickListener(new OnBookItemClickListener() {
            @Override
            public void onItemClick(BookAdapter.ViewHolder holder, View view, int position) {
                BookInfo item = adapter.getItem(position);

                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.book_detail_dialog);

                TextView tvName = dialog.findViewById(R.id.tv_name);
                TextView tvAuthor = dialog.findViewById(R.id.tv_author);
                TextView tvContents = dialog.findViewById(R.id.tv_contents);

                tvName.setText("책 제목:\n" + item.getName());
                tvAuthor.setText("\n저자: " + item.getAuthor());
                tvContents.setText("\n줄거리:\n" + item.getContents());

                dialog.show();
            }

        });

        Button button = rootView.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ArrayList<BookInfo> result = callback.selectAll();
                adapter.setItems(result);
                adapter.notifyDataSetChanged();
            }
        });
        return rootView;
    }
}