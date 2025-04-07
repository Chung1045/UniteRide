package com.chung.a9rushtobus.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.chung.a9rushtobus.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database version and name
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MyAppDatabase.db";
    private Context context;
    public KMBDatabase kmbDatabase = new KMBDatabase(this);
    public CTBDatabase ctbDatabase = new CTBDatabase(this);
    public GMBDatabase gmbDatabase = new GMBDatabase(this);
    public SavedRoutesManager savedRoutesManager = new SavedRoutesManager(this);

    // Table creation constants
    private static final String SQL_CREATE_RTHK_NEWS_TABLE = "CREATE TABLE IF NOT EXISTS " +
            Tables.RTHK_NEWS.TABLE_NAME + " (" + Tables.RTHK_NEWS.COLUMN_CONTENT + " TEXT," +
            Tables.RTHK_NEWS.COLUMN_DATE + " TEXT" + ");";
    private static final String SQL_CREATE_USER_SAVED_TABLE = "CREATE TABLE IF NOT EXISTS " +
            Tables.USER_SAVED.TABLE_NAME + " (" + Tables.USER_SAVED.COLUMN_ROUTE_ID + " TEXT," +
            Tables.USER_SAVED.COLUMN_COMPANY_ID + " TEXT," +
            Tables.USER_SAVED.COLUMN_ROUTE_BOUND + " TEXT," +
            Tables.USER_SAVED.COLUMN_ROUTE_SERVICE_TYPE + " TEXT," +
            Tables.USER_SAVED.COLUMN_STOP_ID + " TEXT" + ");";
    private static final String SQL_DELETE_RTHK_NEWS_TABLE = "DROP TABLE IF EXISTS " + Tables.RTHK_NEWS.TABLE_NAME;
    private static final String SQL_DELETE_USER_SAVED_TABLE = "DROP TABLE IF EXISTS " + Tables.USER_SAVED.TABLE_NAME;

    // Static inner class to hold table contract details
    public static class Tables {
        // Entry table contract
        public static class RTHK_NEWS implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "rthk_news";
            public static final String COLUMN_CONTENT = "content";
            public static final String COLUMN_DATE = "date";
        }

        public static class USER_SAVED implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "user_saved";
            public static final String COLUMN_ROUTE_ID = "route_id";
            public static final String COLUMN_COMPANY_ID = "company";
            public static final String COLUMN_ROUTE_BOUND = "route_bound";
            public static final String COLUMN_ROUTE_SERVICE_TYPE = "route_service_type";
            public static final String COLUMN_STOP_ID = "stop_id";
        }
    }

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void removeAllValues() {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("DatabaseHelper", "Attempting to remove all values 1");
        db.execSQL("DELETE FROM " + KMBDatabase.Tables.KMB_ROUTES.TABLE_NAME);
        Log.d("DatabaseHelper", "Attempting to remove all values 2");
        db.execSQL("DELETE FROM " + KMBDatabase.Tables.KMB_ROUTE_STOPS.TABLE_NAME);
        Log.d("DatabaseHelper", "Attempting to remove all values 3");
        db.execSQL("DELETE FROM " + CTBDatabase.Tables.CTB_ROUTES.TABLE_NAME);
        Log.d("DatabaseHelper", "Attempting to remove all values 4");
        db.execSQL("DELETE FROM " + CTBDatabase.Tables.CTB_ROUTE_STOPS.TABLE_NAME);
        Log.d("DatabaseHelper", "Attempting to remove all values 5");
        db.execSQL("DELETE FROM " + GMBDatabase.Tables.GMB_ROUTES.TABLE_NAME);
        Log.d("DatabaseHelper", "Attempting to remove all values 6");
        db.execSQL("DELETE FROM " + GMBDatabase.Tables.GMB_ROUTES_INFO.TABLE_NAME);
        Log.d("DatabaseHelper", "All values removed");
    }

    public long updateRTHKNews(String content, String date) {
        Log.d("DatabaseHelper", "Attempting to update database with: " + content + " " + date);
        SQLiteDatabase db = this.getReadableDatabase(); // Use readable db for querying

        // Step 1: Check if the entry already exists
        Cursor cursor = db.query(Tables.RTHK_NEWS.TABLE_NAME, new String[]{Tables.RTHK_NEWS.COLUMN_CONTENT}, // Just need the content column
                Tables.RTHK_NEWS.COLUMN_CONTENT + " = ? AND " + Tables.RTHK_NEWS.COLUMN_DATE + " = ?", new String[]{content, date}, null, null, null);

        boolean exists = cursor.getCount() > 0; // If count > 0, the entry exists
        cursor.close();

        if (exists) {
            Log.d("DatabaseHelper", "Entry already exists, skipping insert");
            return -1; // Indicate that no insertion was performed
        }

        // Step 2: If not exists, insert the new entry
        ContentValues values = new ContentValues();
        values.put(Tables.RTHK_NEWS.COLUMN_CONTENT, content);
        values.put(Tables.RTHK_NEWS.COLUMN_DATE, date);

        long result = db.insert(Tables.RTHK_NEWS.TABLE_NAME, null, values);

        Log.d("DatabaseHelper", "Insert result: " + result);
        return result;
    }

    // Create tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(KMBDatabase.SQL_CREATE_KMB_ROUTES_TABLE);
        db.execSQL(KMBDatabase.SQL_CREATE_KMB_ROUTE_STOPS_TABLE);
        db.execSQL(KMBDatabase.SQL_CREATE_KMB_STOPS_TABLE);
        db.execSQL(CTBDatabase.SQL_CREATE_CTB_ROUTES_TABLE);
        db.execSQL(CTBDatabase.SQL_CREATE_CTB_ROUTE_STOPS_TABLE);
        db.execSQL(CTBDatabase.SQL_CREATE_CTB_STOPS_TABLE);
        db.execSQL(GMBDatabase.SQL_CREATE_GMB_ROUTES_TABLES);
        db.execSQL(GMBDatabase.SQL_CREATE_GMB_ROUTES_INFO_TABLE);
        db.execSQL(GMBDatabase.SQL_CREATE_GMB_ROUTE_STOPS_TABLE);
        db.execSQL(GMBDatabase.SQL_CREATE_GMB_STOP_LOCATIONS_TABLE);
        db.execSQL(SQL_CREATE_RTHK_NEWS_TABLE);
        db.execSQL(SQL_CREATE_USER_SAVED_TABLE);
    }

    // Handle database upgrades
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Delete existing tables
        db.execSQL(KMBDatabase.SQL_DELETE_KMB_ROUTES_TABLE);
        db.execSQL(KMBDatabase.SQL_DELETE_KMB_ROUTE_STOPS_TABLE);
        db.execSQL(KMBDatabase.SQL_DELETE_KMB_STOPS_TABLE);
        db.execSQL(CTBDatabase.SQL_DELETE_CTB_ROUTES_TABLE);
        db.execSQL(CTBDatabase.SQL_DELETE_CTB_ROUTE_STOPS_TABLE);
        db.execSQL(CTBDatabase.SQL_DELETE_CTB_STOPS_TABLE);
        db.execSQL(GMBDatabase.SQL_DELETE_GMB_ROUTES_TABLES);
        db.execSQL(GMBDatabase.SQL_DELETE_GMB_ROUTES_INFO_TABLE);
        db.execSQL(GMBDatabase.SQL_DELETE_GMB_ROUTE_STOPS_TABLE);
        db.execSQL(GMBDatabase.SQL_DELETE_GMB_STOP_LOCATIONS_TABLE);
        db.execSQL(SQL_DELETE_RTHK_NEWS_TABLE);
        db.execSQL(SQL_DELETE_USER_SAVED_TABLE);

        // Recreate tables
        onCreate(db);
    }

    // Optional: Handle database downgrades
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}