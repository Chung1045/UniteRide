package com.chung.a9rushtobus.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.chung.a9rushtobus.UserPreferences;
import com.chung.a9rushtobus.elements.BusRouteStopItem;

import java.util.ArrayList;
import java.util.List;

public class SavedRoutesManager {
    private static final String TAG = "SavedRoutesManager";
    private final DatabaseHelper dbHelper;

    public SavedRoutesManager(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public boolean saveRouteStop(BusRouteStopItem busRouteStopItem) {
        // First check if this route stop is already saved
        if (isRouteStopSaved(busRouteStopItem)) {
            Log.d(TAG, "Route stop already saved: " + busRouteStopItem.getRoute() + " stop " + busRouteStopItem.getStopID());
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(DatabaseHelper.Tables.USER_SAVED.COLUMN_ROUTE_ID, busRouteStopItem.getRoute());
        values.put(DatabaseHelper.Tables.USER_SAVED.COLUMN_COMPANY_ID, busRouteStopItem.getCompany());
        values.put(DatabaseHelper.Tables.USER_SAVED.COLUMN_ROUTE_BOUND, busRouteStopItem.getBound());
        values.put(DatabaseHelper.Tables.USER_SAVED.COLUMN_ROUTE_SERVICE_TYPE, busRouteStopItem.getServiceType());
        values.put(DatabaseHelper.Tables.USER_SAVED.COLUMN_STOP_ID, busRouteStopItem.getStopID());
        
        long result = db.insert(DatabaseHelper.Tables.USER_SAVED.TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean isRouteStopSaved(BusRouteStopItem busRouteStopItem) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.Tables.USER_SAVED.TABLE_NAME +
                " WHERE " + DatabaseHelper.Tables.USER_SAVED.COLUMN_ROUTE_ID + " = ?" +
                " AND " + DatabaseHelper.Tables.USER_SAVED.COLUMN_COMPANY_ID + " = ?" +
                " AND " + DatabaseHelper.Tables.USER_SAVED.COLUMN_STOP_ID + " = ?";
                
        String[] selectionArgs = {
                busRouteStopItem.getRoute(),
                busRouteStopItem.getCompany(),
                busRouteStopItem.getStopID()
        };
        
        Cursor cursor = db.rawQuery(query, selectionArgs);
        boolean isSaved = cursor.getCount() > 0;
        cursor.close();
        return isSaved;
    }

    public boolean removeRouteStop(BusRouteStopItem busRouteStopItem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String whereClause = DatabaseHelper.Tables.USER_SAVED.COLUMN_ROUTE_ID + " = ? AND " +
                DatabaseHelper.Tables.USER_SAVED.COLUMN_COMPANY_ID + " = ? AND " +
                DatabaseHelper.Tables.USER_SAVED.COLUMN_STOP_ID + " = ?";
                
        String[] whereArgs = {
                busRouteStopItem.getRoute(),
                busRouteStopItem.getCompany(),
                busRouteStopItem.getStopID()
        };
        
        int result = db.delete(DatabaseHelper.Tables.USER_SAVED.TABLE_NAME, whereClause, whereArgs);
        return result > 0;
    }

    public List<BusRouteStopItem> getSavedRouteStops() {
        List<BusRouteStopItem> savedStops = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Check if UserPreferences.sharedPref is initialized
        if (UserPreferences.sharedPref == null) {
            Log.e(TAG, "UserPreferences.sharedPref is null! Cannot check onboarding status.");
            return savedStops;
        }

        boolean onboardingComplete = UserPreferences.sharedPref.getBoolean(UserPreferences.ONBOARDING_COMPLETE, false);
        Log.d(TAG, "Onboarding complete status: " + onboardingComplete);

        // For debugging purposes, temporarily bypass the onboarding check
        // if (!onboardingComplete) {
        //     Log.d(TAG, "Bypassing onboarding check for debugging");
        //     onboardingComplete = true;
        // }

        if (onboardingComplete) {
            // If the user has completed onboarding, we can fetch saved stops
            Log.d(TAG, "User has completed onboarding, fetching saved stops");
        } else {
            Log.d(TAG, "User has not completed onboarding, skipping saved stops fetch");
            return savedStops;
        }
        
        // Check if the table exists
        try {
            String tableCheckQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + 
                                    DatabaseHelper.Tables.USER_SAVED.TABLE_NAME + "'";
            Cursor tableCheckCursor = db.rawQuery(tableCheckQuery, null);
            boolean tableExists = tableCheckCursor.getCount() > 0;
            tableCheckCursor.close();
            
            if (!tableExists) {
                Log.e(TAG, "Table " + DatabaseHelper.Tables.USER_SAVED.TABLE_NAME + " does not exist!");
                return savedStops;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if table exists: " + e.getMessage());
            return savedStops;
        }
        
        String query = "SELECT * FROM " + DatabaseHelper.Tables.USER_SAVED.TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        
        Log.d(TAG, "getSavedRouteStops: Found " + cursor.getCount() + " saved stops");
        
        if (cursor.moveToFirst()) {
            do {
                String routeId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.USER_SAVED.COLUMN_ROUTE_ID));
                String companyId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.USER_SAVED.COLUMN_COMPANY_ID));
                String routeBound = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.USER_SAVED.COLUMN_ROUTE_BOUND));
                String serviceType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.USER_SAVED.COLUMN_ROUTE_SERVICE_TYPE));
                String stopId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.USER_SAVED.COLUMN_STOP_ID));
                
                // We need to fetch stop name data from respective company databases
                BusRouteStopItem item;
                if (companyId.equals("gmb")) {
                    // For GMB routes
                    Log.d(TAG, "Processing GMB stop - routeId: " + routeId + ", stopId: " + stopId);
                    item = fetchGMBStopDetails(routeId, serviceType, stopId);
                } else {
                    // For KMB and CTB routes
                    item = fetchKMBCTBStopDetails(routeId, routeBound, serviceType, stopId, companyId);
                }
                
                if (item != null) {
                    savedStops.add(item);
                }
                
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return savedStops;
    }
    
    // Helper method to fetch KMB or CTB stop details
    private BusRouteStopItem fetchKMBCTBStopDetails(String routeId, String routeBound, String serviceType, 
                                                 String stopId, String companyId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String stopsTable, routeStopsTable;
        
        String query;
        
        if (companyId.equals("kmb")) {
            stopsTable = KMBDatabase.Tables.KMB_STOPS.TABLE_NAME;
            routeStopsTable = KMBDatabase.Tables.KMB_ROUTE_STOPS.TABLE_NAME;
            
            query = "SELECT s.stop_name_en, s.stop_name_tc, s.stop_name_sc FROM " + 
                   stopsTable + " s JOIN " + routeStopsTable + " rs " +
                   "ON s.stop_id = rs.stop_id " +
                   "WHERE rs.route = ? AND rs.bound = ? AND rs.service_type = ? AND rs.stop_id = ?";
        } else {
            stopsTable = CTBDatabase.Tables.CTB_STOPS.TABLE_NAME;
            routeStopsTable = CTBDatabase.Tables.CTB_ROUTE_STOPS.TABLE_NAME;
            
            // CTB has different column names for stop names and doesn't use service_type
            query = "SELECT s.name_en, s.name_tc, s.name_sc FROM " + 
                   stopsTable + " s JOIN " + routeStopsTable + " rs " +
                   "ON s.stop_id = rs.stop_id " +
                   "WHERE rs.route = ? AND rs.bound = ? AND rs.stop_id = ?";
        }
        
        Cursor cursor;
        if (companyId.equals("kmb")) {
            String[] selectionArgs = {routeId, routeBound, serviceType, stopId};
            cursor = db.rawQuery(query, selectionArgs);
        } else {
            // CTB doesn't use service_type
            String[] selectionArgs = {routeId, routeBound, stopId};
            cursor = db.rawQuery(query, selectionArgs);
        }
        
        BusRouteStopItem item = null;
        if (cursor.moveToFirst()) {
            String nameEn = cursor.getString(0);
            String nameTc = cursor.getString(1);
            String nameSc = cursor.getString(2);
            
            item = new BusRouteStopItem(routeId, routeBound, serviceType, nameEn, nameTc, nameSc, stopId, companyId);
        }
        
        cursor.close();
        return item;
    }
    
    // Helper method to fetch GMB stop details
    private BusRouteStopItem fetchGMBStopDetails(String routeId, String serviceType, String stopId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Log.d(TAG, "fetchGMBStopDetails - Fetching details for routeId: " + routeId + ", stopId: " + stopId);
        
        BusRouteStopItem item = null;
        
        try {
            // Get stop details from route_stops table
            String stopsQuery = "SELECT " + 
                              GMBDatabase.Tables.GMB_ROUTE_STOPS.STOP_NAME_EN + ", " +
                              GMBDatabase.Tables.GMB_ROUTE_STOPS.STOP_NAME_TC + ", " +
                              GMBDatabase.Tables.GMB_ROUTE_STOPS.STOP_NAME_SC + ", " +
                              GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_STOP_SEQ + ", " +
                              GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_ROUTE_SEQ +
                              " FROM " + GMBDatabase.Tables.GMB_ROUTE_STOPS.TABLE_NAME + 
                              " WHERE " + GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_ROUTE_ID + " = ? AND " +
                              GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_STOP_ID + " = ?";
            String[] stopsSelectionArgs = {routeId, stopId};
            Cursor stopsCursor = db.rawQuery(stopsQuery, stopsSelectionArgs);
            
            if (stopsCursor != null && stopsCursor.moveToFirst()) {
                String nameEn = stopsCursor.getString(0);
                String nameTc = stopsCursor.getString(1);
                String nameSc = stopsCursor.getString(2);
                
                Log.d(TAG, "Found GMB stop names - EN: " + nameEn + ", TC: " + nameTc);
                
                // Get stop sequence
                String gmbStopSeqQuery = "SELECT " + GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_STOP_SEQ + 
                                       " FROM " + GMBDatabase.Tables.GMB_ROUTE_STOPS.TABLE_NAME + 
                                       " WHERE " + GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_ROUTE_ID + " = ? AND " +
                                       GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_STOP_ID + " = ?";
                String[] gmbSelectionArgs = {routeId, stopId};
                Cursor gmbCursor = db.rawQuery(gmbStopSeqQuery, gmbSelectionArgs);
                
                String stopSeq = "0";
                if (gmbCursor != null && gmbCursor.moveToFirst()) {
                    stopSeq = gmbCursor.getString(0);
                    Log.d(TAG, "Found GMB stop sequence: " + stopSeq);
                } else {
                    Log.d(TAG, "No stop sequence found for GMB stop");
                }
                
                if (gmbCursor != null) gmbCursor.close();
                
                String routeSeq = stopsCursor.getString(3);
                
                // Create item with found data
                item = new BusRouteStopItem(routeId, routeSeq, serviceType, nameEn, nameTc, nameSc, stopId, "gmb", stopSeq);
                Log.d(TAG, "Created GMB BusRouteStopItem: " + routeId + " - " + nameEn);
            } else {
                Log.e(TAG, "No stop location data found for GMB stop ID: " + stopId);
            }
            
            if (stopsCursor != null) stopsCursor.close();
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching GMB stop details: " + e.getMessage());
            e.printStackTrace();
        }
        
        return item;
    }
}
