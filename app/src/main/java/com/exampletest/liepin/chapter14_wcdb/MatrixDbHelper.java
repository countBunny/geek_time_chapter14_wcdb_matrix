package com.exampletest.liepin.chapter14_wcdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MatrixDbHelper extends SQLiteOpenHelper {
    private static MatrixDbHelper sInstance;

    public MatrixDbHelper(Context context){
        super(context, "plain-text.db", null, 1, null);
    }

    public static MatrixDbHelper get(){
        if (null == sInstance) {
            synchronized (MatrixDbHelper.class){
                if (null == sInstance) {
                    sInstance = new MatrixDbHelper(MyApplication.getCtx());
                }
            }
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE message (content TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing.
    }
}
