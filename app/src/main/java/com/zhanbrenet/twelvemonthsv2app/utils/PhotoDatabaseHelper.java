package com.zhanbrenet.twelvemonthsv2app.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PhotoDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "photo_db.db";
    private static final int DATABASE_VERSION = 3; // incrémentation pour mettre à jour la nouvelle database

    public static final String TABLE_NAME = "photos";
    public static final String COL_ID = "id";
    public static final String COL_FILENAME = "filename";
    public static final String COL_DATE = "date";
    public static final String COL_ADDRESS = "address";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_FILENAME + " TEXT, " +
                    COL_DATE + " TEXT, " +
                    COL_ADDRESS + " TEXT, " +
                    COL_LATITUDE + " REAL, " +
                    COL_LONGITUDE + " REAL)";


    public PhotoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.close();
    }
}
