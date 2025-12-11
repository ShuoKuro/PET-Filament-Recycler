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

/**
 * DatabaseHelper class manages SQLite database for Bluetooth logs.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Database name.
     */
    private static final String DATABASE_NAME = "bluetooth_logs.db";

    /**
     * Database version.
     */
    private static final int DATABASE_VERSION = 1;

    // Table name and columns
    /**
     * Table name for logs.
     */
    private static final String TABLE_LOGS = "bluetooth_logs";

    /**
     * Column for ID.
     */
    private static final String COLUMN_ID = "_id";

    /**
     * Column for timestamp.
     */
    private static final String COLUMN_TIMESTAMP = "timestamp";

    /**
     * Column for direction ("IN" or "OUT").
     */
    private static final String COLUMN_DIRECTION = "direction";

    /**
     * Column for message.
     */
    private static final String COLUMN_MESSAGE = "message";

    /**
     * Constructor.
     * @param context Context.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creates the database table.
     * @param db SQLite database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_LOGS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TIMESTAMP + " TEXT NOT NULL, " +
                COLUMN_DIRECTION + " TEXT NOT NULL, " +
                COLUMN_MESSAGE + " TEXT NOT NULL);";
        db.execSQL(createTable);
    }

    /**
     * Upgrades the database by dropping and recreating the table.
     * @param db SQLite database.
     * @param oldVersion Old version.
     * @param newVersion New version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        onCreate(db);
    }

    /**
     * Inserts a log entry with timestamp.
     * @param direction Direction ("IN" or "OUT").
     * @param message Message.
     */
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

    /**
     * Gets all logs in descending order (newest first).
     * @return List of LogEntry.
     */
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

    /**
     * Inner class representing a log entry.
     */
    public static class LogEntry {
        /**
         * ID.
         */
        public long id;

        /**
         * Timestamp.
         */
        public String timestamp;

        /**
         * Direction.
         */
        public String direction;

        /**
         * Message.
         */
        public String message;

        /**
         * Constructor.
         * @param id ID.
         * @param timestamp Timestamp.
         * @param direction Direction.
         * @param message Message.
         */
        public LogEntry(long id, String timestamp, String direction, String message) {
            this.id = id;
            this.timestamp = timestamp;
            this.direction = direction;
            this.message = message;
        }
    }
}