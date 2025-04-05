package com.chung.a9rushtobus.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.chung.a9rushtobus.elements.BusRouteStopItem;

import java.util.ArrayList;
import java.util.List;

public class SavedRoutesManager {
    private static final String TAG = "SavedRoutesManager";
    private final DatabaseHelper dbHelper;

    public SavedRoutesManager(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Save a bus route stop to the user_saved table
     * @param busRouteStopItem The bus route stop to save
     * @return True if save was successful, false otherwise
     */
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

    /**
     * Check if a bus route stop is already saved
     * @param busRouteStopItem The bus route stop to check
     * @return True if already saved, false otherwise
     */
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

    /**
     * Remove a saved bus route stop
     * @param busRouteStopItem The bus route stop to remove
     * @return True if removal was successful, false otherwise
     */
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

    /**
     * Get all saved bus route stops
     * @return List of saved BusRouteStopItem objects
     */
    public List<BusRouteStopItem> getSavedRouteStops() {
        List<BusRouteStopItem> savedStops = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String query = "SELECT * FROM " + DatabaseHelper.Tables.USER_SAVED.TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        
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
        
        // For GMB routes, we need to retrieve route sequence
        String gmbRouteSeqQuery = "SELECT route_sequence FROM gmb_route_stops WHERE route_id = ? AND stop_id = ?";
        String[] gmbSelectionArgs = {routeId, stopId};
        Cursor gmbCursor = db.rawQuery(gmbRouteSeqQuery, gmbSelectionArgs);
        
        BusRouteStopItem item = null;
        if (gmbCursor.moveToFirst()) {
            String gmbRouteSeq = gmbCursor.getString(0);
            
            // Now get stop names from the stop locations table
            // Using direct table and column names to avoid potential issues
            String stopsQuery = "SELECT name_en, name_tc, name_sc FROM gmb_stop_locations WHERE stop_id = ?";
            String[] stopsSelectionArgs = {stopId};
            Cursor stopsCursor = db.rawQuery(stopsQuery, stopsSelectionArgs);
            
            if (stopsCursor.moveToFirst()) {
                String nameEn = stopsCursor.getString(0);
                String nameTc = stopsCursor.getString(1);
                String nameSc = stopsCursor.getString(2);
                
                item = new BusRouteStopItem(routeId, "0", serviceType, nameEn, nameTc, nameSc, stopId, routeId, gmbRouteSeq);
            }
            
            stopsCursor.close();
        }
        
        gmbCursor.close();
        return item;
    }
}
