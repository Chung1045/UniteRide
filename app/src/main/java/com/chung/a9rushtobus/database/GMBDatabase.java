package com.chung.a9rushtobus.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

public class GMBDatabase {

    private static SQLiteDatabase db;

    public static String SQL_CREATE_GMB_ROUTES_TABLES = "CREATE TABLE IF NOT EXISTS " +
            Tables.GMB_ROUTES.TABLE_NAME + " (" + Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER + " TEXT, " +
            Tables.GMB_ROUTES.COLUMN_ROUTE_REGION + " TEXT, " +
            "UNIQUE (" + Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER + ", " + Tables.GMB_ROUTES.COLUMN_ROUTE_REGION + ")" + ");";

    public static final String SQL_CREATE_GMB_ROUTES_INFO_TABLE =
            "CREATE TABLE IF NOT EXISTS " + Tables.GMB_ROUTES_INFO.TABLE_NAME + " (" +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ID + " INTEGER, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_NUMBER + " TEXT NOT NULL, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_REGION + " TEXT NOT NULL CHECK (" +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_REGION + " IN ('HKI', 'KLN', 'NT')), " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_TYPE + " TEXT NOT NULL, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_EN + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_TC + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_SC + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_EN + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_TC + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_SC + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_EN + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_TC + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_SC + " TEXT, " +
                    Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_SEQ + " INTEGER NOT NULL, " +
                    "UNIQUE (" + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ID + ", " + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_SEQ + ")" +
                    ");";

    public static String SQL_DELETE_GMB_ROUTES_TABLES = "DROP TABLE IF EXISTS " + Tables.GMB_ROUTES.TABLE_NAME;
    public static String SQL_DELETE_GMB_ROUTES_INFO_TABLE = "DROP TABLE IF EXISTS " + Tables.GMB_ROUTES_INFO.TABLE_NAME;

    public GMBDatabase(SQLiteOpenHelper helper) {
        db = helper.getWritableDatabase();
    }

    public static class Tables {

        public static final class GMB_ROUTES implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "gmb_routes";
            public static final String COLUMN_ROUTE_NUMBER = "route_number";
            public static final String COLUMN_ROUTE_REGION = "route_region"; // HKI / KLN / NT
        }

        public static final class GMB_ROUTES_INFO implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "gmb_routes_info";
            public static final String COLUMN_ROUTE_ID = "route_id"; // 7 digit number
            public static final String COLUMN_ROUTE_NUMBER = "route_number";
            public static final String COLUMN_ROUTE_REGION = "route_region"; // HKI / KLN / NT
            public static final String COLUMN_ROUTE_TYPE = "route_type"; // normal or special
            public static final String COLUMN_ROUTE_ORIGIN_EN = "route_origin_en";
            public static final String COLUMN_ROUTE_ORIGIN_TC = "route_origin_tc";
            public static final String COLUMN_ROUTE_ORIGIN_SC = "route_origin_sc";
            public static final String COLUMN_ROUTE_DEST_EN = "route_destination_en";
            public static final String COLUMN_ROUTE_DEST_TC = "route_destination_tc";
            public static final String COLUMN_ROUTE_DEST_SC = "route_destination_sc";
            public static final String COLUMN_REMARKS_EN = "remarks_en";
            public static final String COLUMN_REMARKS_TC = "remarks_tc";
            public static final String COLUMN_REMARKS_SC = "remarks_sc";
            public static final String COLUMN_ROUTE_SEQ = "route_sequence"; // 1 : inbound, 2 : outbound
        }

        public static class GMB_ROUTE_STOPS implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "gmb_route_stops";
            public static final String COLUMN_ROUTE_ID = "route_id";
            public static final String COLUMN_ROUTE_NUMBER = "route_number";
            public static final String COLUMN_ROUTE_REGION = "route_region";
            public static final String COLUMN_ROUTE_SEQ = "route_sequence";
            public static final String COLUMN_STOP_ID = "stop_id";
            public static final String COLUMN_LATITUDE = "lat";
            public static final String COLUMN_LONGITUDE = "lng";

        }

        public static String[] PROJECTION_GET_ALL_ROUTES_INFO = new String[]{
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ID,
                GMBDatabase.Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER,
                GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_ROUTE_SEQ,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_EN,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_TC,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_SC,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_EN,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_TC,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_SC,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_EN,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_TC,
                GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_SC
        };
    }

    public static final class Queries {
        public static final String QUERY_GET_ALL_ROUTES_INFO =
                "SELECT DISTINCT " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ID + ", " +
                        "r." + Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_SEQ + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_EN + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_TC + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_SC + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_EN + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_TC + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_SC + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_EN + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_TC + ", " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_SC + " " +
                        "FROM " + Tables.GMB_ROUTES_INFO.TABLE_NAME + " ri " +
                        "JOIN " + Tables.GMB_ROUTES.TABLE_NAME + " r ON " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_NUMBER + " = " +
                        "r." + Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER + " AND " +
                        "ri." + Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_REGION + " = " +
                        "r." + Tables.GMB_ROUTES.COLUMN_ROUTE_REGION ;
    }

    public void updateRoutes(JSONObject routesJson, BiConsumer<String, String> onSuccess, Consumer<String> onError) {
        Log.d("GMBDatabase", "updateRoutes");
        Log.d("GMBDatabase", "routesJson: " + routesJson.toString());

        try {
            // These are the 3 known route regions
            String[] regions = {"HKI", "NT", "KLN"};

            for (String region : regions) {
                Log.d("GMBDatabase", "Processing region: " + region);
                JSONArray routeArray = routesJson.getJSONArray(region);
                Log.d("GMBDatabase", "routeArray: " + routeArray);

                for (int i = 0; i < routeArray.length(); i++) {
                    String routeID = routeArray.getString(i);
                    ContentValues values = new ContentValues();
                    db.beginTransaction();
                    values.put(Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER, routeID);
                    values.put(Tables.GMB_ROUTES.COLUMN_ROUTE_REGION, region);
                    db.insertWithOnConflict(Tables.GMB_ROUTES.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    db.setTransactionSuccessful();
                    db.endTransaction();

                    onSuccess.accept(routeID, region);
                }
            }

        } catch (Exception e) {
            Log.e("GMBDatabase", "Error updating routes", e);
            onError.accept(e.getMessage());
        } finally {
            db.endTransaction();
        }
    }


    public void updateRouteInfo(String jsonData, BiConsumer<Integer, Integer> onSuccess) throws JSONException {
        Log.d("GMBDatabase", "updateRouteInfo");

        // Convert jsonData (String) into a JSONObject
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        // Loop through all routes
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject firstRoute = dataArray.getJSONObject(i);

            // Extract values
            String region = firstRoute.getString("region");
            String routeCode = firstRoute.getString("route_code");
            int routeId = firstRoute.getInt("route_id");
            String description = firstRoute.getString("description_en");
            String descriptionTC = firstRoute.getString("description_tc");

            Log.d("GMBDatabase", "region: " + region);
            Log.d("GMBDatabase", "routeCode: " + routeCode);
            Log.d("GMBDatabase", "routeId: " + routeId);
            Log.d("GMBDatabase", "description: " + description + " " + descriptionTC);

            // Get directions array
            JSONArray directionsArray = firstRoute.getJSONArray("directions");

            for (int j = 0; j < directionsArray.length(); j++) {
                JSONObject direction = directionsArray.getJSONObject(j);
                int routeSeq = direction.getInt("route_seq");
                String origEn = direction.getString("orig_en");
                String origTC = direction.getString("orig_tc");
                String origSC = direction.getString("orig_sc");
                String destEn = direction.getString("dest_en");
                String destTC = direction.getString("dest_tc");
                String destSC = direction.getString("dest_sc");

                String remarksEn = direction.getString("remarks_en");
                String remarksTC = direction.getString("remarks_tc");
                String remarksSC = direction.getString("remarks_sc");

                Log.d("GMBDatabase", "Direction: " + routeSeq + " | From: " + origEn + " | To: " + destEn);

                JSONArray headwaysArray = direction.getJSONArray("headways");
                Log.d("GMBDatabase", "headwaysArray: " + headwaysArray);

                for (int k = 0; k < headwaysArray.length(); k++) {
                    JSONObject headway = headwaysArray.getJSONObject(k);

                    boolean isPublicHolidayAvailable = headway.getString("public_holiday").equals("true");

                    Log.d("GMBDatabase", "isPublicHolidayAvailable: " + isPublicHolidayAvailable);

                    ContentValues values = new ContentValues();
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ID, routeId);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_NUMBER, routeCode);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_REGION, region);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_TYPE, "Placeholder");
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_EN, origEn);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_TC, origTC);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_SC, origSC);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_EN, destEn);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_TC, destTC);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_SC, destSC);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_EN, remarksEn);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_TC, remarksTC);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_SC, remarksSC);
                    values.put(Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_SEQ, routeSeq);

                    try {
                        db.beginTransaction();
                        // Using insertWithOnConflict with CONFLICT_IGNORE flag to implement "INSERT OR IGNORE"
                        db.insertWithOnConflict(
                                Tables.GMB_ROUTES_INFO.TABLE_NAME,
                                null,
                                values,
                                SQLiteDatabase.CONFLICT_IGNORE
                        );
                        db.setTransactionSuccessful();
                        onSuccess.accept(routeId, routeSeq);
                    } catch (Exception e) {
                        Log.e("GMBDatabase", "Error inserting route info", e);
                    } finally {
                        db.endTransaction();
                    }

                }

            }
        }
    }

    public void updateRouteStops(String jsonData) throws JSONException {
        Log.d("GMBDatabase", "updateRouteStops");

        // Convert jsonData (String) into a JSONObject
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONObject data = jsonObject.getJSONObject("data"); // Get "data" object
        JSONArray routeStops = data.getJSONArray("route_stops"); // Get "route_stops" array

        for (int i = 0; i < routeStops.length(); i++) {
            JSONObject stop = routeStops.getJSONObject(i); // Get each stop object

            int stopSeq = stop.getInt("stop_seq");
            int stopId = stop.getInt("stop_id");
            String nameTc = stop.getString("name_tc");
            String nameSc = stop.getString("name_sc");
            String nameEn = stop.getString("name_en");

            // Log or process the stop data
            Log.d("GMBDatabase", "Stop " + stopSeq + ": " + nameEn + " (ID: " + stopId + ")");
        }
    }


}