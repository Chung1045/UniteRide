package com.chung.a9rushtobus.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KMBDatabase {
    
    public SQLiteDatabase db;
    public static final String SQL_CREATE_KMB_ROUTES_TABLE = "CREATE TABLE IF NOT EXISTS "
            + KMBDatabase.Tables.KMB_ROUTES.TABLE_NAME + " ("
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_TC + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_SC + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_EN + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_TC + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_SC + " TEXT" + ");";
    public static final String SQL_CREATE_KMB_STOPS_TABLE = "CREATE TABLE IF NOT EXISTS "
            + KMBDatabase.Tables.KMB_STOPS.TABLE_NAME + " ("
            + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_ID + " TEXT PRIMARY KEY,"
            + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN + " TEXT,"
            + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC + " TEXT,"
            + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC + " TEXT,"
            + KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE + " TEXT,"
            + KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE + " TEXT"
            + ");";
    public static final String SQL_CREATE_KMB_ROUTE_STOPS_TABLE = "CREATE TABLE IF NOT EXISTS "
            + KMBDatabase.Tables.KMB_ROUTE_STOPS.TABLE_NAME + " ("
            + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_BOUND + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE + " TEXT,"
            + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + " TEXT" + ");";
    public static final String SQL_DELETE_KMB_ROUTES_TABLE = "DROP TABLE IF EXISTS " + KMBDatabase.Tables.KMB_ROUTES.TABLE_NAME;
    public static final String SQL_DELETE_KMB_STOPS_TABLE = "DROP TABLE IF EXISTS " + KMBDatabase.Tables.KMB_STOPS.TABLE_NAME;
    public static final String SQL_DELETE_KMB_ROUTE_STOPS_TABLE = "DROP TABLE IF EXISTS " + KMBDatabase.Tables.KMB_ROUTE_STOPS.TABLE_NAME;
    public static class Tables {

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

    }

    public static class Queries {
        public static final String QUERY_GET_STOP_FROM_ROUTE =
                "SELECT DISTINCT rs." + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + ", " +
                "rs." + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + ", " +
                "s." + Tables.KMB_STOPS.COLUMN_STOP_NAME_EN + ", " +
                "s." + Tables.KMB_STOPS.COLUMN_STOP_NAME_TC + ", " +
                "s." + Tables.KMB_STOPS.COLUMN_STOP_NAME_SC + ", " +
                "s." + Tables.KMB_STOPS.COLUMN_LATITUDE + ", " +
                "s." + Tables.KMB_STOPS.COLUMN_LONGITUDE +
                " FROM " + Tables.KMB_ROUTE_STOPS.TABLE_NAME + " rs" +
                " JOIN " + Tables.KMB_STOPS.TABLE_NAME + " s" +
                " ON rs." + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID +
                " = s." + Tables.KMB_STOPS.COLUMN_STOP_ID +
                " WHERE rs." + Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE + " = ?" +
                " AND rs." + Tables.KMB_ROUTE_STOPS.COLUMN_BOUND + " = ?" +
                " AND rs." + Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE + " = ?" +
                " GROUP BY rs." + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID +
                " ORDER BY CAST(rs." + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + " AS INTEGER)";

        public static final String QUERY_GET_STOPID_FROM_ROUTEBOUND =
                "SELECT " + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + ", " +
                        Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID +
                        " FROM " + Tables.KMB_ROUTE_STOPS.TABLE_NAME +
                        " WHERE " + Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE + " =?" +
                        " AND " + Tables.KMB_ROUTE_STOPS.COLUMN_BOUND + " =?" +
                        " ORDER BY CAST(" + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + " AS INTEGER)";

    }
    
    public KMBDatabase(SQLiteOpenHelper helper) {
        db = helper.getWritableDatabase();
    }

    public synchronized long updateKMBRoute(String route, String bound, String serviceType, String originEn, String originTc, String originSc, String destEn, String destTc, String destSc) {

        Log.d("KMBDatabase", "Attempting to update database with: " + route + " " + bound + " " + serviceType);

        ContentValues values = new ContentValues();
        values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN, originEn);
        values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_TC, originTc);
        values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_SC, originSc);
        values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_EN, destEn);
        values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_TC, destTc);
        values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_SC, destSc);

        // Define the WHERE clause
        String whereClause = KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE + "=? AND " + KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND + "=? AND " + KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + "=?";
        String[] whereArgs = {route, bound, serviceType};

        // Try updating the row
        int rowsAffected = db.update(KMBDatabase.Tables.KMB_ROUTES.TABLE_NAME, values, whereClause, whereArgs);

        // If no rows were updated, insert a new row
        if (rowsAffected == 0) {
            values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE, route);
            values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND, bound);
            values.put(KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE, serviceType);
            long result = db.insert(KMBDatabase.Tables.KMB_ROUTES.TABLE_NAME, null, values);
            Log.d("KMBDatabase", "Inserted new record, ID: " + result);
            return result;
        }

        Log.d("KMBDatabase", "Updated record, rows affected: " + rowsAffected);
        return rowsAffected;
    }

    public synchronized void updateKMBStop(String stopId, String stopNameEn, String stopNameTc, String stopNameSc, String latitude, String longitude) {

        Log.d("KMBDatabaseSTOP", "Attempting to update database with: " + stopId + " " + stopNameEn + " " + stopNameTc + " " + stopNameSc + " " + latitude + " " + longitude);

        ContentValues values = new ContentValues();
        values.put(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_ID, stopId);
        values.put(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN, stopNameEn);
        values.put(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC, stopNameTc);
        values.put(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC, stopNameSc);
        values.put(KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE, latitude);
        values.put(KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE, longitude);

        long result = db.insertWithOnConflict(KMBDatabase.Tables.KMB_STOPS.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        Log.d("KMBDatabaseSTOP", "Insert result: Position " + result);
        Log.d("KMBDatabaseSTOP", "Insert content: " + stopNameEn + " " + stopNameTc + " " + stopNameSc + " " + latitude + " " + longitude);
    }

    public synchronized void updateKMBRouteStops(String stopId, String route, String bound, String serviceType, String stopSeq) {
        ContentValues values = new ContentValues();
        values.put(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID, stopId);
        values.put(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE, route);
        values.put(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_BOUND, bound);
        values.put(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE, serviceType);
        values.put(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ, stopSeq);

        // Use insertWithOnConflict to insert or replace
        long result = db.insertWithOnConflict(
                KMBDatabase.Tables.KMB_ROUTE_STOPS.TABLE_NAME,
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

    public String getRouteDestination(String route, String bound, String serviceType) {
        // Check for null parameters
        if (route == null || bound == null || serviceType == null) {
            Log.d("KMBDatabase", "getRouteDestination: One or more parameters are null");
            return null;
        }

        String query = "SELECT " + Tables.KMB_ROUTES.COLUMN_DEST_EN + ", " +
                Tables.KMB_ROUTES.COLUMN_DEST_TC + ", " +
                Tables.KMB_ROUTES.COLUMN_DEST_SC + " " +
                " FROM " + Tables.KMB_ROUTES.TABLE_NAME +
                " WHERE " + Tables.KMB_ROUTES.COLUMN_ROUTE + "=? AND " +
                Tables.KMB_ROUTES.COLUMN_BOUND + "=? AND " +
                Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + "=?";
        
        try (Cursor cursor = db.rawQuery(query, new String[]{route, bound, serviceType})) {
            if (cursor.moveToFirst()) {
                String destination = cursor.getString(0);

                String appLang = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
                Log.d("KMBDatabase", "Found destination: " + destination + " for route: " + route);

                switch (appLang) {
                    case "zh-rCN":
                        return cursor.getString(2);
                    case "zh-rHK":
                        return cursor.getString(1);
                    default: // "en" or any other case return cursor.getString(0);
                        return cursor.getString(0);
                }

            }
        } catch (Exception e) {
            Log.e("KMBDatabase", "Error querying destination for route: " + route + ", error: " + e.getMessage());
        }
        return null;
    }

    public Cursor queryNearbyRoutes(double latitude, double longitude, int radiusMeters) {
        // Calculate approximate bounding box for faster filtering
        // 0.009 degrees is roughly 1km at equator
        double latDelta = radiusMeters * 0.000009;
        double lonDelta = radiusMeters * 0.000009;
        
        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;
        
        // Simplified SQL query that:
        // 1. Filters stops roughly within the bounding box
        // 2. Joins with route information
        // 3. Uses a simpler distance calculation (squared Euclidean) that doesn't require trig functions
        String query =
                "SELECT DISTINCT " +
                        "r." + Tables.KMB_ROUTES.COLUMN_ROUTE + ", " +
                        "r." + Tables.KMB_ROUTES.COLUMN_BOUND + ", " +
                        "r." + Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + ", " +
                        "r." + Tables.KMB_ROUTES.COLUMN_ORIGIN_EN + ", " +
                        "r." + Tables.KMB_ROUTES.COLUMN_DEST_EN + ", " +
                        "s." + Tables.KMB_STOPS.COLUMN_STOP_NAME_EN + ", " +
                        "s." + Tables.KMB_STOPS.COLUMN_STOP_ID + ", " +
                        "s." + Tables.KMB_STOPS.COLUMN_LATITUDE + ", " +
                        "s." + Tables.KMB_STOPS.COLUMN_LONGITUDE + ", " +
                        // Simple squared distance for sorting (not accurate for long distances but works for nearby sorting)
                        "((CAST(s." + Tables.KMB_STOPS.COLUMN_LATITUDE + " AS REAL) - ?) * (CAST(s." + Tables.KMB_STOPS.COLUMN_LATITUDE + " AS REAL) - ?) + " +
                        "(CAST(s." + Tables.KMB_STOPS.COLUMN_LONGITUDE + " AS REAL) - ?) * (CAST(s." + Tables.KMB_STOPS.COLUMN_LONGITUDE + " AS REAL) - ?)) AS distance " +
                        "FROM " + Tables.KMB_STOPS.TABLE_NAME + " s " +
                        "JOIN " + Tables.KMB_ROUTE_STOPS.TABLE_NAME + " rs " +
                        "ON s." + Tables.KMB_STOPS.COLUMN_STOP_ID + " = rs." + Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + " " +
                        "JOIN " + Tables.KMB_ROUTES.TABLE_NAME + " r " +
                        "ON rs." + Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE + " = r." + Tables.KMB_ROUTES.COLUMN_ROUTE + " " +
                        "AND rs." + Tables.KMB_ROUTE_STOPS.COLUMN_BOUND + " = r." + Tables.KMB_ROUTES.COLUMN_BOUND + " " +
                        "AND rs." + Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE + " = r." + Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + " " +
                        "WHERE CAST(s." + Tables.KMB_STOPS.COLUMN_LATITUDE + " AS REAL) BETWEEN ? AND ? " +
                        "AND CAST(s." + Tables.KMB_STOPS.COLUMN_LONGITUDE + " AS REAL) BETWEEN ? AND ? " +
                        "GROUP BY r." + Tables.KMB_ROUTES.COLUMN_ROUTE + " " +
                        "ORDER BY distance";

        // Execute the query with the provided parameters
        Log.d("KMBDatabase", "Querying for stops within " + radiusMeters + "m of " + latitude + ", " + longitude);
        
        // Note: We've removed the HAVING clause that used the radius because we're using
        // a bounding box approach. The additional filtering can be done in Java code if needed.
        return db.rawQuery(query, new String[]{
            String.valueOf(latitude),    // For distance calculation 
            String.valueOf(latitude),    // For distance calculation
            String.valueOf(longitude),   // For distance calculation
            String.valueOf(longitude),   // For distance calculation
            String.valueOf(minLat),      // Bounding box min latitude
            String.valueOf(maxLat),      // Bounding box max latitude
            String.valueOf(minLon),      // Bounding box min longitude
            String.valueOf(maxLon)       // Bounding box max longitude
        });
    }
    
    public void importKMBStops(Context context) {
        File tempFile = null;
        FileOutputStream out = null;
        InputStream inputStream = null;
        SQLiteDatabase sourceDb = null;

        try {
            Log.d("KMBDatabase", "Starting database import process");
            
            // First ensure our tables exist
            db.execSQL(SQL_CREATE_KMB_ROUTES_TABLE);
            db.execSQL(SQL_CREATE_KMB_STOPS_TABLE);
            db.execSQL(SQL_CREATE_KMB_ROUTE_STOPS_TABLE);
            
            // Open the asset file
            inputStream = context.getAssets().open("defined-database/kmb_stops.db");
            Log.d("KMBDatabase", "Successfully opened asset file");

            // Create a temporary file
            tempFile = File.createTempFile("temp_kmb_stops", ".db", context.getCacheDir());
            tempFile.setReadable(true, false);
            tempFile.setWritable(true, false);
            Log.d("KMBDatabase", "Created temp file at: " + tempFile.getAbsolutePath());
            
            // Copy the database file
            out = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int length;
            long totalBytes = 0;
            
            while ((length = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
                totalBytes += length;
            }
            
            out.flush();
            out.close();
            inputStream.close();
            Log.d("KMBDatabase", "Copied " + totalBytes + " bytes to temp file");

            // Open source database directly
            sourceDb = SQLiteDatabase.openDatabase(tempFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            
            // Get tables from source
            List<String> sourceTables = new ArrayList<>();
            try (Cursor c = sourceDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null)) {
                while (c.moveToNext()) {
                    sourceTables.add(c.getString(0));
                }
            }
            
            Log.d("KMBDatabase", "Source tables found: " + sourceTables);

            // Copy data table by table
            db.beginTransaction();
            try {
                for (String sourceTable : sourceTables) {
                    String destTable;
                    switch (sourceTable) {
                        case "kmb_routes":
                            destTable = Tables.KMB_ROUTES.TABLE_NAME;
                            break;
                        case "kmb_stops":
                            destTable = Tables.KMB_STOPS.TABLE_NAME;
                            break;
                        case "kmb_route_stops":
                            destTable = Tables.KMB_ROUTE_STOPS.TABLE_NAME;
                            break;
                        default:
                            continue;
                    }

                    // Get all data from source table
                    try (Cursor cursor = sourceDb.rawQuery("SELECT * FROM " + sourceTable, null)) {
                        if (cursor.moveToFirst()) {
                            String[] columnNames = cursor.getColumnNames();
                            
                            do {
                                ContentValues values = new ContentValues();
                                for (String column : columnNames) {
                                    int columnIndex = cursor.getColumnIndex(column);
                                    if (!cursor.isNull(columnIndex)) {
                                        values.put(column, cursor.getString(columnIndex));
                                    }
                                }
                                db.insertWithOnConflict(destTable, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                            } while (cursor.moveToNext());
                            
                            Log.d("KMBDatabase", "Copied " + cursor.getCount() + " rows from " + sourceTable + " to " + destTable);
                        }
                    }
                }
                
                db.setTransactionSuccessful();
                Log.d("KMBDatabase", "Database import completed successfully");
            } finally {
                db.endTransaction();
            }

        } catch (IOException e) {
            Log.e("KMBDatabase", "IO Error during import", e);
            e.printStackTrace();
        } catch (SQLiteException e) {
            Log.e("KMBDatabase", "SQLite Error during import", e);
            e.printStackTrace();
        } finally {
            // Clean up resources
            try {
                if (out != null) out.close();
                if (inputStream != null) inputStream.close();
                if (sourceDb != null) sourceDb.close();
            } catch (IOException e) {
                Log.e("KMBDatabase", "Error closing streams", e);
            }
            
            // Delete temp file
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                Log.d("KMBDatabase", "Temp file deleted: " + deleted);
            }
        }
    }

}
