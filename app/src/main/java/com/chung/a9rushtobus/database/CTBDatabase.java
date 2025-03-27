package com.chung.a9rushtobus.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CTBDatabase {

    private SQLiteDatabase db;

    public CTBDatabase(SQLiteOpenHelper helper) {
        db = helper.getWritableDatabase();
    }

    public static final String SQL_CREATE_CTB_ROUTES_TABLE =
            "CREATE TABLE " + Tables.CTB_ROUTES.TABLE_NAME + " (" +
                    Tables.CTB_ROUTES._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Tables.CTB_ROUTES.COLUMN_ROUTE + " TEXT NOT NULL, " +
                    Tables.CTB_ROUTES.COLUMN_ORIGIN_EN + " TEXT, " +
                    Tables.CTB_ROUTES.COLUMN_ORIGIN_TC + " TEXT, " +
                    Tables.CTB_ROUTES.COLUMN_ORIGIN_SC + " TEXT, " +
                    Tables.CTB_ROUTES.COLUMN_DEST_EN + " TEXT, " +
                    Tables.CTB_ROUTES.COLUMN_DEST_TC + " TEXT, " +
                    Tables.CTB_ROUTES.COLUMN_DEST_SC + " TEXT, " +
                    "UNIQUE (" + Tables.CTB_ROUTES.COLUMN_ROUTE + ") ON CONFLICT REPLACE" +
                    ");";


    public static final String SQL_CREATE_CTB_ROUTE_STOPS_TABLE =
            "CREATE TABLE " + Tables.CTB_ROUTE_STOPS.TABLE_NAME + " (" +
                    Tables.CTB_ROUTE_STOPS._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Tables.CTB_ROUTE_STOPS.COLUMN_STOP_ID + " TEXT NOT NULL, " +
                    Tables.CTB_ROUTE_STOPS.COLUMN_ROUTE + " TEXT NOT NULL, " +
                    Tables.CTB_ROUTE_STOPS.COLUMN_BOUND + " TEXT NOT NULL, " +
                    Tables.CTB_ROUTE_STOPS.COLUMN_STOP_SEQ + " TEXT NOT NULL, " +
                    "UNIQUE (" + Tables.CTB_ROUTE_STOPS.COLUMN_STOP_ID + ") ON CONFLICT REPLACE" +
                    ");";

    public static final String SQL_CREATE_CTB_STOPS_TABLE =
            "CREATE TABLE " + Tables.CTB_STOPS.TABLE_NAME + " (" +
                    Tables.CTB_STOPS._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Tables.CTB_STOPS.COLUMN_STOP_ID + " TEXT NOT NULL, " +
                    Tables.CTB_STOPS.COLUMN_NAME_EN + " TEXT NOT NULL, " +
                    Tables.CTB_STOPS.COLUMN_NAME_TC + " TEXT NOT NULL, " +
                    Tables.CTB_STOPS.COLUMN_NAME_SC + " TEXT NOT NULL, " +
                    Tables.CTB_STOPS.COLUMN_LATITUDE + " TEXT NOT NULL, " +
                    Tables.CTB_STOPS.COLUMN_LONGITUDE + " TEXT NOT NULL, " +
                    "UNIQUE (" + Tables.CTB_STOPS.COLUMN_STOP_ID + ") ON CONFLICT REPLACE" + ");";


    public static final String SQL_DELETE_CTB_ROUTES_TABLE = "DROP TABLE IF EXISTS " + Tables.CTB_ROUTES.TABLE_NAME;
    public static final String SQL_DELETE_CTB_ROUTE_STOPS_TABLE = "DROP TABLE IF EXISTS " + Tables.CTB_ROUTE_STOPS.TABLE_NAME;
    public static final String SQL_DELETE_CTB_STOPS_TABLE = "DROP TABLE IF EXISTS " + Tables.CTB_STOPS.TABLE_NAME;

    public static class Tables {

        public static class CTB_ROUTES implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "ctb_all_routes";
            public static final String COLUMN_ROUTE = "route";
            public static final String COLUMN_ORIGIN_EN = "origin_en";
            public static final String COLUMN_ORIGIN_TC = "origin_tc";
            public static final String COLUMN_ORIGIN_SC = "origin_sc";
            public static final String COLUMN_DEST_EN = "dest_en";
            public static final String COLUMN_DEST_TC = "dest_tc";
            public static final String COLUMN_DEST_SC = "dest_sc";
        }

        public static class CTB_ROUTE_STOPS implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "ctb_route_stops";
            public static final String COLUMN_STOP_ID = "stop_id";
            public static final String COLUMN_ROUTE = "route";
            public static final String COLUMN_BOUND = "bound";
            public static final String COLUMN_STOP_SEQ = "stop_seq";
        }

        public static class CTB_STOPS implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "ctb_all_stops";
            public static final String COLUMN_STOP_ID = "stop_id";
            public static final String COLUMN_NAME_EN = "name_en";
            public static final String COLUMN_NAME_TC = "name_tc";
            public static final String COLUMN_NAME_SC = "name_sc";
            public static final String COLUMN_LATITUDE = "latitude";
            public static final String COLUMN_LONGITUDE = "longitude";
        }
    }

    public void updateCTBRoute(String route, String originEn, String originTc, String originSc, String destEn, String destTc, String destSc) {

        Log.d("CTBDatabase", "Attempting to update database with: " + route);

        ContentValues values = new ContentValues();
        values.put(CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_EN, originEn);
        values.put(CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_TC, originTc);
        values.put(CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_SC, originSc);
        values.put(CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_EN, destEn);
        values.put(CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_TC, destTc);
        values.put(CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_SC, destSc);

        // Corrected WHERE clause
        String whereClause = CTBDatabase.Tables.CTB_ROUTES.COLUMN_ROUTE + " = ?";
        String[] whereArgs = {route};

        // Try updating the row
        int rowsAffected = db.update(CTBDatabase.Tables.CTB_ROUTES.TABLE_NAME, values, whereClause, whereArgs);

        // If no rows were updated, insert a new row
        if (rowsAffected == 0) {
            values.put(CTBDatabase.Tables.CTB_ROUTES.COLUMN_ROUTE, route);
            long result = db.insert(CTBDatabase.Tables.CTB_ROUTES.TABLE_NAME, null, values);
            Log.d("CTBDatabase", "Inserted new record, ID: " + result);
            return;
        }

        Log.d("CTBDatabase", "Updated record, rows affected: " + rowsAffected);
    }

    public void updateCTBRouteStops(String stopId, String route, String bound, String stopSeq) {
        ContentValues values = new ContentValues();
        values.put(Tables.CTB_ROUTE_STOPS.COLUMN_STOP_ID, stopId);
        values.put(Tables.CTB_ROUTE_STOPS.COLUMN_ROUTE, route);
        values.put(Tables.CTB_ROUTE_STOPS.COLUMN_BOUND, bound);
        values.put(Tables.CTB_ROUTE_STOPS.COLUMN_STOP_SEQ, stopSeq);

        // Use insertWithOnConflict to insert or replace
        long result = db.insertWithOnConflict(
                Tables.CTB_ROUTE_STOPS.TABLE_NAME,
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

    public void updateCTBStops(String stopId, String nameEn, String nameTc, String nameSc, String latitude, String longitude) {
        ContentValues values = new ContentValues();
        values.put(Tables.CTB_STOPS.COLUMN_STOP_ID, stopId);
        values.put(Tables.CTB_STOPS.COLUMN_NAME_EN, nameEn);
        values.put(Tables.CTB_STOPS.COLUMN_NAME_TC, nameTc);
        values.put(Tables.CTB_STOPS.COLUMN_NAME_SC, nameSc);
        values.put(Tables.CTB_STOPS.COLUMN_LATITUDE, latitude);
        values.put(Tables.CTB_STOPS.COLUMN_LONGITUDE, longitude);

        // Use insertWithOnConflict to insert or replace
        long result = db.insertWithOnConflict(
                Tables.CTB_STOPS.TABLE_NAME,
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

}
