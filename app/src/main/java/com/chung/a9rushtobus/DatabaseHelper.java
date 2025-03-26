package com.chung.a9rushtobus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database version and name
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MyAppDatabase.db";

    // Table creation constants
    private static final String SQL_CREATE_KMB_ROUTES_TABLE = "CREATE TABLE " + Tables.KMB_ROUTES.TABLE_NAME + " (" + Tables.KMB_ROUTES.COLUMN_ROUTE + " TEXT," + Tables.KMB_ROUTES.COLUMN_BOUND + " TEXT," + Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + " TEXT," + Tables.KMB_ROUTES.COLUMN_ORIGIN_EN + " TEXT," + Tables.KMB_ROUTES.COLUMN_ORIGIN_TC + " TEXT," + Tables.KMB_ROUTES.COLUMN_ORIGIN_SC + " TEXT," + Tables.KMB_ROUTES.COLUMN_DEST_EN + " TEXT," + Tables.KMB_ROUTES.COLUMN_DEST_TC + " TEXT," + Tables.KMB_ROUTES.COLUMN_DEST_SC + " TEXT" + ");";
    private static final String SQL_CREATE_KMB_STOPS_TABLE = "CREATE TABLE " + Tables.KMB_STOPS.TABLE_NAME + " (" + Tables.KMB_STOPS.COLUMN_STOP_ID + " TEXT," + Tables.KMB_STOPS.COLUMN_STOP_NAME_EN + " TEXT," + Tables.KMB_STOPS.COLUMN_STOP_NAME_TC + " TEXT," + Tables.KMB_STOPS.COLUMN_STOP_NAME_SC + " TEXT," + Tables.KMB_STOPS.COLUMN_LATITUDE + " TEXT," + Tables.KMB_STOPS.COLUMN_LONGITUDE + " TEXT" + ");";
    private static final String SQL_CREATE_RTHK_NEWS_TABLE = "CREATE TABLE " + Tables.RTHK_NEWS.TABLE_NAME + " (" + Tables.RTHK_NEWS.COLUMN_CONTENT + " TEXT," + Tables.RTHK_NEWS.COLUMN_DATE + " TEXT" + ");";
    private static final String SQL_CREATE_KMB_ROUTE_STOPS_TABLE = "CREATE TABLE " + Tables.KMB_ROUTE_STOPS.TABLE_NAME + " (" + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + " TEXT," + Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE + " TEXT," + Tables.KMB_ROUTE_STOPS.COLUMN_BOUND + " TEXT," + Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE + " TEXT," + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + " TEXT" + ");";

    // Table deletion constants
    private static final String SQL_DELETE_KMB_ROUTES_TABLE = "DROP TABLE IF EXISTS " + Tables.KMB_ROUTES.TABLE_NAME;
    private static final String SQL_DELETE_KMB_STOPS_TABLE = "DROP TABLE IF EXISTS " + Tables.KMB_STOPS.TABLE_NAME;
    private static final String SQL_DELETE_RTHK_NEWS_TABLE = "DROP TABLE IF EXISTS " + Tables.RTHK_NEWS.TABLE_NAME;
    private static final String SQL_DELETE_KMB_ROUTE_STOPS_TABLE = "DROP TABLE IF EXISTS " + Tables.KMB_ROUTE_STOPS.TABLE_NAME;


    // Static inner class to hold table contract details
    public static class Tables {
        // Entry table contract

        public static class KMB_ROUTES implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "kmb_all_routes";
            public static final String COLUMN_ROUTE = "route";
            public static final String COLUMN_BOUND = "bound";
            public static final String COLUMN_SERVICE_TYPE = "service_type";
            public static final String COLUMN_ORIGIN_EN = "origin_en";
            public static final String COLUMN_ORIGIN_TC = "origin_tc";
            public static final String COLUMN_ORIGIN_SC = "origin_sc";
            public static final String COLUMN_DEST_EN = "dest_en";
            public static final String COLUMN_DEST_TC = "dest_tc";
            public static final String COLUMN_DEST_SC = "dest_sc";
        }

        public static class KMB_STOPS implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "kmb_all_stops";
            public static final String COLUMN_STOP_ID = "stop_id";
            public static final String COLUMN_STOP_NAME_EN = "stop_name_en";
            public static final String COLUMN_STOP_NAME_TC = "stop_name_tc";
            public static final String COLUMN_STOP_NAME_SC = "stop_name_sc";
            public static final String COLUMN_LATITUDE = "latitude";
            public static final String COLUMN_LONGITUDE = "longitude";
        }

        public static class KMB_ROUTE_STOPS implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "kmb_route_stops";
            public static final String COLUMN_STOP_ID = "stop_id";
            public static final String COLUMN_ROUTE = "route";
            public static final String COLUMN_BOUND = "bound";
            public static final String COLUMN_SERVICE_TYPE = "service_type";
            public static final String COLUMN_STOP_SEQ = "stop_seq";
        }

        public static class RTHK_NEWS implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "rthk_news";
            public static final String COLUMN_CONTENT = "content";
            public static final String COLUMN_DATE = "date";
        }

    }

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void removeAllValues(){
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("DatabaseHelper", "Attempting to remove all values");
        db.execSQL("DELETE FROM " + Tables.KMB_ROUTES.TABLE_NAME);
        db.execSQL("DELETE FROM " + Tables.KMB_STOPS.TABLE_NAME);
        db.execSQL("DELETE FROM " + Tables.RTHK_NEWS.TABLE_NAME);
        db.execSQL("DELETE FROM " + Tables.KMB_ROUTE_STOPS.TABLE_NAME);
        Log.d("DatabaseHelper", "All values removed");
    }

    public long updateKMBRoute(String route, String bound, String serviceType, String originEn, String originTc, String originSc, String destEn, String destTc, String destSc) {

        Log.d("DatabaseHelper", "Attempting to update database with: " + route + " " + bound + " " + serviceType);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Tables.KMB_ROUTES.COLUMN_ORIGIN_EN, originEn);
        values.put(Tables.KMB_ROUTES.COLUMN_ORIGIN_TC, originTc);
        values.put(Tables.KMB_ROUTES.COLUMN_ORIGIN_SC, originSc);
        values.put(Tables.KMB_ROUTES.COLUMN_DEST_EN, destEn);
        values.put(Tables.KMB_ROUTES.COLUMN_DEST_TC, destTc);
        values.put(Tables.KMB_ROUTES.COLUMN_DEST_SC, destSc);

        // Define the WHERE clause
        String whereClause = Tables.KMB_ROUTES.COLUMN_ROUTE + "=? AND " + Tables.KMB_ROUTES.COLUMN_BOUND + "=? AND " + Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + "=?";
        String[] whereArgs = {route, bound, serviceType};

        // Try updating the row
        int rowsAffected = db.update(Tables.KMB_ROUTES.TABLE_NAME, values, whereClause, whereArgs);

        // If no rows were updated, insert a new row
        if (rowsAffected == 0) {
            values.put(Tables.KMB_ROUTES.COLUMN_ROUTE, route);
            values.put(Tables.KMB_ROUTES.COLUMN_BOUND, bound);
            values.put(Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE, serviceType);
            long result = db.insert(Tables.KMB_ROUTES.TABLE_NAME, null, values);
            Log.d("DatabaseHelper", "Inserted new record, ID: " + result);
            db.close();
            return result;
        }

        Log.d("DatabaseHelper", "Updated record, rows affected: " + rowsAffected);
        return rowsAffected;
    }

    public void updateKMBStop(String stopId, String stopNameEn, String stopNameTc, String stopNameSc, String latitude, String longitude) {

        Log.d("DatabaseHelperSTOP", "Attempting to update database with: " + stopId + " " + stopNameEn + " " + stopNameTc + " " + stopNameSc + " " + latitude + " " + longitude);
        SQLiteDatabase db = this.getReadableDatabase(); // Use readable db for querying

        ContentValues values = new ContentValues();
        values.put(Tables.KMB_STOPS.COLUMN_STOP_ID, stopId);
        values.put(Tables.KMB_STOPS.COLUMN_STOP_NAME_EN, stopNameEn);
        values.put(Tables.KMB_STOPS.COLUMN_STOP_NAME_TC, stopNameTc);
        values.put(Tables.KMB_STOPS.COLUMN_STOP_NAME_SC, stopNameSc);
        values.put(Tables.KMB_STOPS.COLUMN_LATITUDE, latitude);
        values.put(Tables.KMB_STOPS.COLUMN_LONGITUDE, longitude);

        long result = db.insertWithOnConflict(Tables.KMB_STOPS.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        Log.d("DatabaseHelperSTOP", "Insert result: Position " + result);
        Log.d("DatabaseHelperSTOP", "Insert content: " + stopNameEn + " " + stopNameTc + " " + stopNameSc + " " + latitude + " " + longitude);
    }

    public void updateKMBRouteStops(String stopId, String route, String bound, String serviceType, String stopSeq) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID, stopId);
        values.put(Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE, route);
        values.put(Tables.KMB_ROUTE_STOPS.COLUMN_BOUND, bound);
        values.put(Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE, serviceType);
        values.put(Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ, stopSeq);

        // Use insertWithOnConflict to insert or replace
        long result = db.insertWithOnConflict(
                Tables.KMB_ROUTE_STOPS.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);

        if (result == -1) {
            // Handle error
            Log.e("Database", "Insert or update failed");
        } else {
            // Successfully inserted or updated
            Log.d("Database", "Record inserted or updated successfully");
        }
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
            db.close();
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
        db.execSQL(SQL_CREATE_KMB_ROUTES_TABLE);
        db.execSQL(SQL_CREATE_RTHK_NEWS_TABLE);
        db.execSQL(SQL_CREATE_KMB_STOPS_TABLE);
        db.execSQL(SQL_CREATE_KMB_ROUTE_STOPS_TABLE);
    }

    // Handle database upgrades
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Delete existing tables
        db.execSQL(SQL_DELETE_KMB_ROUTES_TABLE);
        db.execSQL(SQL_DELETE_RTHK_NEWS_TABLE);
        db.execSQL(SQL_DELETE_KMB_STOPS_TABLE);
        db.execSQL(SQL_DELETE_KMB_ROUTE_STOPS_TABLE);

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