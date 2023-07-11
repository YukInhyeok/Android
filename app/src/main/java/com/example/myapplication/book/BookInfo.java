package com.example.myapplication.book;

import androidx.annotation.NonNull;

public class BookInfo {
    String name;
    String author;
    String contents;

    private String createDate;

    public BookInfo(String name, String author, String contents, String createDate) {
        this.name = name;
        this.author = author;
        this.contents = contents;
        this.createDate = createDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getContents() {
        return contents;
    }

    // getter와 setter를 추가합니다.
    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }


    @NonNull
    @Override
    public String toString() {
        return "BookInfo{" +
                "name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", contents='" + contents + '\'' +
                '}';


    }
}