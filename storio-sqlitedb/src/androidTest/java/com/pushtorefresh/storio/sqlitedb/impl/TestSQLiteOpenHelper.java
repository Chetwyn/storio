package com.pushtorefresh.storio.sqlitedb.impl;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

class TestSQLiteOpenHelper extends SQLiteOpenHelper {
    public TestSQLiteOpenHelper(@NonNull Context context) {
        super(context, "test_db", null, 1);
    }

    @Override public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(User.CREATE_TABLE);
        db.execSQL(Tweet.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
