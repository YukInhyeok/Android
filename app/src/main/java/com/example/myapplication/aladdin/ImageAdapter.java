package com.example.myapplication.aladdin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;

import java.util.List;

public class ImageAdapter extends ArrayAdapter<Item> {
    private final Context context;
    private final List<Item> items;
    public ImageAdapter(Context context, List<Item> items) {
        super(context, R.layout.my_list_item_layout, items);
        this.context = context;
        this.items = items;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.my_list_item_layout, parent, false);

        TextView textViewTitle = rowView.findViewById(R.id.textViewTitle);
        ImageView imageViewCoverArt = rowView.findViewById(R.id.imageViewCoverArt);

        textViewTitle.setText(items.get(position).Title);

        final Item currentItem = getItem(position);

        Log.d("test1", "URL at getView: " + currentItem.Link);

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = currentItem.Link;
                Log.d("test1", "URL: " + url);
                Intent intent = new Intent(context, WebActivity.class);
                intent.putExtra("url", url);
                context.startActivity(intent);
            }
        });
        // 이미지 로딩 라이브러리 Glide를 사용하여 이미지를 설정합니다.
        if (items.get(position).imageUrl != null) {
            Glide.with(context).load(items.get(position).imageUrl).into(imageViewCoverArt);
        } else {
            // 대체 이미지 로드 혹은 ImageView 숨기기 등의 처리
            imageViewCoverArt.setVisibility(View.GONE);
        }
        return rowView;
    }
}
