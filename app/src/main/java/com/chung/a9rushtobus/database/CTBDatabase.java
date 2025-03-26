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
            "CREATE TABLE " + Tables.CTB_ROUTES.TABLE_NAME +
                    "(" + Tables.CTB_ROUTES._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Tables.CTB_ROUTES.COLUMN_ROUTE + " TEXT, " + Tables.CTB_ROUTES.COLUMN_ORIGIN_EN +
                    " TEXT, " + Tables.CTB_ROUTES.COLUMN_ORIGIN_TC + " TEXT, "
                    + Tables.CTB_ROUTES.COLUMN_ORIGIN_SC + " TEXT, " + Tables.CTB_ROUTES.COLUMN_DEST_EN
                    + " TEXT, " + Tables.CTB_ROUTES.COLUMN_DEST_TC + " TEXT, " + Tables.CTB_ROUTES.COLUMN_DEST_SC + " TEXT)";

    public static final String SQL_DELETE_CTB_ROUTES_TABLE = "DROP TABLE IF EXISTS " + Tables.CTB_ROUTES.TABLE_NAME;

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

}
