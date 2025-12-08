// First, add this new class for DatabaseHelper
package com.petfilament.recycler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bluetooth_logs.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and columns
    private static final String TABLE_LOGS = "bluetooth_logs";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_DIRECTION = "direction"; // "IN" or "OUT"
    private static final String COLUMN_MESSAGE = "message";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_LOGS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TIMESTAMP + " TEXT NOT NULL, " +
                COLUMN_DIRECTION + " TEXT NOT NULL, " +
                COLUMN_MESSAGE + " TEXT NOT NULL);";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        onCreate(db);
    }

    // Method to insert a log entry
    public void insertLog(String direction, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_DIRECTION, direction);
        values.put(COLUMN_MESSAGE, message);
        db.insert(TABLE_LOGS, null, values);
        db.close();
    }

    // Method to get all logs in descending order (newest first)
    public ArrayList<LogEntry> getAllLogs() {
        ArrayList<LogEntry> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOGS, null, null, null, null, null, COLUMN_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                String direction = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DIRECTION));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE));
                logs.add(new LogEntry(id, timestamp, direction, message));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return logs;
    }

    // Inner class to represent a log entry
    public static class LogEntry {
        public long id;
        public String timestamp;
        public String direction;
        public String message;

        public LogEntry(long id, String timestamp, String direction, String message) {
            this.id = id;
            this.timestamp = timestamp;
            this.direction = direction;
            this.message = message;
        }
    }
}