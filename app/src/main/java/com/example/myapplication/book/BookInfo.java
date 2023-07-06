package com.example.myapplication.book;

import androidx.annotation.NonNull;

public class BookInfo {
    String name;
    String author;
    String contents;

    public BookInfo(String name, String author, String contents) {
        this.name = name;
        this.author = author;
        this.contents = contents;
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
