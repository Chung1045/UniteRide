package com.chung.a9rushtobus;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database version and name
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MyAppDatabase.db";

    // Table creation constants
    private static final String SQL_CREATE_ENTRIES_TABLE =
            "CREATE TABLE " + Tables.Entry.TABLE_NAME + " (" +
                    Tables.Entry._ID + " INTEGER PRIMARY KEY," +
                    Tables.Entry.COLUMN_NAME_TITLE + " TEXT," +
                    Tables.Entry.COLUMN_NAME_SUBTITLE + " TEXT)";

    private static final String SQL_CREATE_USERS_TABLE =
            "CREATE TABLE " + Tables.Users.TABLE_NAME + " (" +
                    Tables.Users._ID + " INTEGER PRIMARY KEY," +
                    Tables.Users.COLUMN_USERNAME + " TEXT," +
                    Tables.Users.COLUMN_EMAIL + " TEXT)";

    // Table deletion constants
    private static final String SQL_DELETE_ENTRIES_TABLE =
            "DROP TABLE IF EXISTS " + Tables.Entry.TABLE_NAME;

    private static final String SQL_DELETE_USERS_TABLE =
            "DROP TABLE IF EXISTS " + Tables.Users.TABLE_NAME;

    // Static inner class to hold table contract details
    public static class Tables {
        // Entry table contract
        public static class Entry implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "entries";
            public static final String COLUMN_NAME_TITLE = "title";
            public static final String COLUMN_NAME_SUBTITLE = "subtitle";
        }

        // Users table contract
        public static class Users implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "users";
            public static final String COLUMN_USERNAME = "username";
            public static final String COLUMN_EMAIL = "email";
        }
    }

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES_TABLE);
        db.execSQL(SQL_CREATE_USERS_TABLE);
    }

    // Handle database upgrades
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Delete existing tables
        db.execSQL(SQL_DELETE_ENTRIES_TABLE);
        db.execSQL(SQL_DELETE_USERS_TABLE);

        // Recreate tables
        onCreate(db);
    }

    // Optional: Handle database downgrades
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    // Example method to add a generic table later
    public void addNewTable(SQLiteDatabase db, String createTableSQL) {
        db.execSQL(createTableSQL);
    }
}