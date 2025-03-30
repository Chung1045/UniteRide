package com.chung.a9rushtobus.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class KMBDatabase {
    
    private SQLiteDatabase db;
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

    public long updateKMBRoute(String route, String bound, String serviceType, String originEn, String originTc, String originSc, String destEn, String destTc, String destSc) {

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

    public void updateKMBStop(String stopId, String stopNameEn, String stopNameTc, String stopNameSc, String latitude, String longitude) {

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

    public void updateKMBRouteStops(String stopId, String route, String bound, String serviceType, String stopSeq) {
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
}
