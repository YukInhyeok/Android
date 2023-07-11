package com.example.myapplication.book;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class ResetCountReceiver extends BroadcastReceiver {
    private static final String TAG = "ResetCountReceiver";
    private static final String PREFS_NAME = "book_count_prefs";
    private static final String KEY_BOOK_COUNT = "book_count";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_BOOK_COUNT, 0);
        editor.apply();
        Log.i(TAG, "Book count reset to 0");
    }
}