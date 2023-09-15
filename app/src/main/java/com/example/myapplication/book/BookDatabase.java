package com.example.myapplication.book;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class BookDatabase {
    public static final String TAG = "BookDatabase";
    private static BookDatabase database;
    public static String DATABASE_NAME = "book.db";
    public static String TABLE_BOOK_INFO = "BOOK_INFO";
    public static int DATABASE_VERSION = 1;
    private DatabaseHelper dbHelper;

    private SQLiteDatabase db;
    private Context context;

    private BookDatabase(Context context){
        this.context = context;
    }

    public static BookDatabase getInstance(Context context){
        if (database == null){
            database = new BookDatabase(context);
        }
        return database;
    }

    public boolean open() {
        println("opening database [" + DATABASE_NAME + "].");
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return true;
    }

    public void close(){
        println("closing database [" + DATABASE_NAME+ "].");
        db.close();
        database = null;
    }

    public Cursor rawQuery(String SQL){
        println("\nexecuteQuery called.\n");
        Cursor c1 = null;
        try {
            c1 = db.rawQuery(SQL, null);
            println("cursor count : " + c1.getCount());
        } catch(Exception ex) {
            Log.e(TAG, "Exception in executeQuery", ex);
        }
        return c1;
    }

    public boolean execSQL(String SQL){
        println("\nexecute called.\n");
        try {
            Log.d(TAG, "SQL : " + SQL);
            db.execSQL(SQL);
        } catch(Exception ex) {
            Log.e(TAG, "Exception in executeQuery", ex);
            return false;
        }
        return true;
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase _db){
            println("creating table [" + TABLE_BOOK_INFO + "].");

            String DROP_SQL = "drop table if exists " + TABLE_BOOK_INFO;
            try {
                _db.execSQL(DROP_SQL);
            } catch(Exception ex) {
                Log.e(TAG, "Exception in DROP_SQL", ex);
            }

            String CREATE_SQL = "create table " + TABLE_BOOK_INFO + "("
                    + " _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "
                    + " NAME TEXT, "
                    + " AUTHOR TEXT, "
                    + " CONTENTS TEXT, "
                    + " CREATE_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP "
                    + ")";

            try {
                _db.execSQL(CREATE_SQL);
            } catch(Exception ex) {
                Log.e(TAG, "Exception in CREATE_SQL", ex);
            }

            insertRecord(_db, "Do it! 안드로이드 앱 프로그래밍", "정재곤", "안드로이드 기본서로 이지스퍼블리싱 출판사에서 출판.");
            insertRecord(_db, "Programming Android", "Mednieks, Zigurd", "Oreilly Associates Inc에서 2011년 04월에 출판.");
            insertRecord(_db, "센차터치 모바일 프로그래밍", "이병옥,최성민 공저", "에이콘출판사에서 2011년 10월에 출판.");
            insertRecord(_db, "시작하세요! 안드로이드 게임 프로그래밍", "마리오 제흐너 저", "위키북스에서 2011년 09월에 출판.");
            insertRecord(_db, "실전! 안드로이드 시스템 프로그래밍 완전정복", "박선호,오영환 공저", "DW Wave에서 2010년 10월에 출판.");

        }



        public void onOpen(SQLiteDatabase db){
            println("opened database [" + DATABASE_NAME + "].");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            println("Upgrading database from version " + oldVersion + " to " + newVersion + ".");

            if (oldVersion < 2){
            }
        }

        private void insertRecord(SQLiteDatabase _db, String name, String author, String contents){
            try {
                _db.execSQL( "insert into " + TABLE_BOOK_INFO + "(NAME, AUTHOR, CONTENTS) values ('" +
                        name + "', '" + author + "', '" + contents + "');" );
            } catch (Exception ex){
                Log.e(TAG, "Exception in executing insert SQL.", ex);
            }
        }
    }

    public void insertRecord(String name, String author, String contents){
        try {
            db.execSQL( "insert into " + TABLE_BOOK_INFO + "(NAME, AUTHOR, CONTENTS) values ('" + name
                    + "', '" + author + "', '" + contents + "');" );
        }catch (Exception ex){
            Log.e(TAG, "Exception in executing insert SQL.", ex);
        }
    }

    public ArrayList<BookInfo> selectAll(){
        ArrayList<BookInfo> result = new ArrayList<BookInfo>();
        try {
            Cursor cursor = db.rawQuery("select NAME, AUTHOR, CONTENTS, CREATE_DATE from " + TABLE_BOOK_INFO, null);
            for (int i = 0; i < cursor.getCount(); i++){
                cursor.moveToNext();
                String name = cursor.getString(0);
                String author = cursor.getString(1);
                String contents = cursor.getString(2);
                String createDate = cursor.getString(3);
                BookInfo info = new BookInfo(name, author, contents, createDate);
                result.add(info);
            }
        }catch (Exception ex){
            Log.e(TAG, "Exception in executing insert SQL.", ex);
        }
        return result;
    }

    private void println(String msg){
        Log.d(TAG, msg);
    }
}
