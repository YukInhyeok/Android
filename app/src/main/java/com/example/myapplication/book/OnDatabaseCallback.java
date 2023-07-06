package com.example.myapplication.book;

import java.util.ArrayList;

public interface OnDatabaseCallback {
    public void insert(String name, String author, String contents);
    public ArrayList<BookInfo> selectAll();
}
