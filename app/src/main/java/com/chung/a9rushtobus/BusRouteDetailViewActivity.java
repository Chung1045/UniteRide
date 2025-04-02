package com.chung.a9rushtobus;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.database.CTBDatabase;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.database.GMBDatabase;
import com.chung.a9rushtobus.database.KMBDatabase;
import com.chung.a9rushtobus.elements.BusRouteStopItem;
import com.chung.a9rushtobus.elements.BusRouteStopItemAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class BusRouteDetailViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BusRouteStopItemAdapter adapter;
    private RecyclerView busRouteStopRecyclerView;
    private List<BusRouteStopItem> busRouteStopItems;
    private String routeNumber, routeDestination, gmbRouteID, gmbRouteSeq, routeBound, routeServiceType, busCompany, description;
    private Integer initialStopSeqView = 0;
    private Utils utils;
    private DataFetcher dataFetcher;
    private DatabaseHelper databaseHelper;
    private Handler handler = new Handler();
    private List<LatLng> pendingStopPositions = null;
    public static final String TAG = "BusRouteDetailView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bus_route_detail_view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_bus_detail_constraint_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View rootView = findViewById(R.id.activity_bus_detail_constraint_layout);
        utils = new Utils(this, rootView, this);

        // Retrieve intent extras
        routeNumber = getIntent().getStringExtra("route");
        routeDestination = getIntent().getStringExtra("destination");
        initialStopSeqView = getIntent().getIntExtra("initialStopSeqView", 0);
        busCompany = getIntent().getStringExtra("company");

        // Log all intent extras for debugging
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log.d(TAG, "Intent Extra - " + key + ": " + extras.get(key));
            }
        }

        assert busCompany != null;
        Log.d(TAG, "Bus Company: " + busCompany);
        if (busCompany.equals("GMB")) {
            gmbRouteID = getIntent().getStringExtra("gmbRouteID");
            gmbRouteSeq = getIntent().getStringExtra("gmbRouteSeq");
            Log.d(TAG, "GMB Route ID: " + gmbRouteID);
            Log.d(TAG, "GMB Route Seq: " + gmbRouteSeq);
            
            if (gmbRouteID == null || gmbRouteID.isEmpty()) {
                Log.e(TAG, "WARNING: GMB Route ID is missing in the intent!");
            }
        }


        description = getIntent().getStringExtra("description");
        if (description == null || description.isEmpty()) {
            description = "Normal Route";
        }
        // Determine the correct bound value for later queries ("O" for outbound, "I" for inbound)
        String boundExtra = getIntent().getStringExtra("bound");
        Log.e("BusRouteDetailView", "routeBound For Intent Extra: " + boundExtra);
        if (Objects.equals(boundExtra, "O") || Objects.equals(boundExtra, "outbound")) {
            routeBound = "outbound";
        } else {
            routeBound = "inbound";
        }
        routeServiceType = getIntent().getStringExtra("serviceType");

        dataFetcher = new DataFetcher(this);
        databaseHelper = new DatabaseHelper(this);

        initView();
        initListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop ETA updates
        if (dataFetcher != null) {
            dataFetcher.shutdown();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        boolean darkTheme = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_DARK, false);
        boolean followSystem = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_FOLLOW_SYSTEM, false);
        if (darkTheme || (followSystem && uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES)) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_night_theme));
        }

        // Default location if no data is available yet
        LatLng defaultPoint = new LatLng(22.345415, 114.192640);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPoint, 16f));

        if (pendingStopPositions != null) {
            drawRouteOnMap(pendingStopPositions);
            pendingStopPositions = null;
        }
    }

    public void initView() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        busRouteStopRecyclerView = findViewById(R.id.recyclerView2);

        TextView busRouteNumber = findViewById(R.id.bus_detail_activity_route_number);
        TextView busRouteDestination = findViewById(R.id.bus_detail_activity_route_dest);
        TextView busRouteRemarks = findViewById(R.id.bus_detail_activity_route_remarks);

        busRouteDestination.setSingleLine(true);
        busRouteDestination.setMarqueeRepeatLimit(-1);
        busRouteDestination.setScroller(new Scroller(this));
        busRouteDestination.setMovementMethod(LinkMovementMethod.getInstance());
        busRouteDestination.setSelected(true);

        busRouteNumber.setText(routeNumber);
        busRouteDestination.setText(routeDestination);
        Log.d("LogBusRouteDetailView", "Route: " + routeNumber + " Destination: " + routeDestination +
                " Bound: " + routeBound + " Service Type: " + routeServiceType);

        busRouteRemarks.setText(description);

        // Initialize the RecyclerView with an empty list
        busRouteStopItems = new ArrayList<>();
        adapter = new BusRouteStopItemAdapter(this, busRouteStopItems, utils);
        getLifecycle().addObserver(adapter);
        busRouteStopRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        busRouteStopRecyclerView.setAdapter(adapter);

        // Load the data from database
        loadBusRouteStopData();
    }

    public void initListener() {
        ImageView backBtn = findViewById(R.id.bus_detail_activity_back_button);
        backBtn.setOnClickListener(v -> finish());
    }
    
    public void loadBusRouteStopData() {
        Log.d(TAG, "Loading bus route stops");
        busRouteStopItems.clear();
        final List<LatLng> stopPositions = new ArrayList<>();
        final String adjustedBound = routeBound.equalsIgnoreCase("outbound") ? "O" : "I";
        Log.d(TAG, "Using bound: " + routeBound + " (adjusted to: " + adjustedBound + ")");
        
        // Show loading indicator
        handler.post(() -> {
            // Add a loading indicator if available in your layout
            // For example: progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(BusRouteDetailViewActivity.this, "Loading bus stops...", Toast.LENGTH_SHORT).show();
        });

        // Use executor service for background processing
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                String query;
                String[] selectionArgs;

                Log.d(TAG, "Bus company: " + busCompany);

                // Set up query based on bus company
                if (Objects.equals(busCompany, "kmb")) {
                    query = KMBDatabase.Queries.QUERY_GET_STOP_FROM_ROUTE;
                    selectionArgs = new String[]{routeNumber, adjustedBound, routeServiceType};
                    Log.d(TAG, "Using KMB query with route: " + routeNumber + ", bound: " + adjustedBound + ", service type: " + routeServiceType);
                } else if (Objects.equals(busCompany, "ctb")) {
                    query = CTBDatabase.Queries.QUERY_GET_STOP_FROM_ROUTE;
                    selectionArgs = new String[]{routeNumber, adjustedBound};
                    Log.d(TAG, "Using CTB query with route: " + routeNumber + ", bound: " + adjustedBound);
                } else if (Objects.equals(busCompany, "GMB")) { // should be in upper case for gmb
                    query = GMBDatabase.Queries.QUERY_STOPS_BY_ROUTE_ID;
                    // Convert bound from "inbound"/"outbound" to "1"/"2" for GMB database
                    String gmbSeq = routeBound.equalsIgnoreCase("inbound") ? "1" : "2";
                    selectionArgs = new String[]{gmbRouteID, gmbSeq};
                    Log.d(TAG, "Using GMB query with route: " + routeNumber + ", seq: " + gmbSeq);
                    Log.d(TAG, "Using GMB query with route: " + routeNumber + ", bound: " +  routeBound);
                } else {
                    Log.e(TAG, "Unknown bus company: " + busCompany);
                    handler.post(() -> Toast.makeText(BusRouteDetailViewActivity.this,
                            "Unknown bus company: " + busCompany, Toast.LENGTH_LONG).show());
                    return;
                }

                Log.d(TAG, "Query Args: " + Arrays.toString(selectionArgs));

                List<BusRouteStopItem> stops = new ArrayList<>();
                List<MarkerOptions> markers = new ArrayList<>();

                // Create a CountDownLatch to wait for async operations if any
                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicBoolean usesFallback = new AtomicBoolean(false);

                // Query database and process results
                try (Cursor cursor = db.rawQuery(query, selectionArgs)) {
                    int index = 0;
                    Log.d(TAG, "Found " + cursor.getCount() + " stops for route " + routeNumber);

                    if (cursor.getCount() > 0) {
                        // Process cursor data and ensure it's complete

                        boolean isMatchOrOffline = false;
                        try {
                            if (busCompany.equalsIgnoreCase("kmb") || busCompany.equalsIgnoreCase("ctb")) {
                                isMatchOrOffline = dataFetcher.isStopNumberMatch(cursor.getCount(), routeNumber, routeBound, routeServiceType, busCompany);
                            } else if (busCompany.equalsIgnoreCase("gmb")) {
                                // For GMB, we'll consider the data valid if we have stops with location data
                                // Otherwise, we might need to implement a specific check for GMB
                                isMatchOrOffline = true; // Default to using local data for GMB
                                Log.d(TAG, "Using local database data for GMB route");
                            }
                        } catch (Exception e) {
                            // Handle any exceptions during the check and proceed with local data
                            Log.e(TAG, "Error checking stop count match: " + e.getMessage());
                            isMatchOrOffline = true; // Default to using local data if check fails
                        }

                        if (isMatchOrOffline) {
                            Log.d(TAG, "Using local database data - count matches or offline");
                            // Clear lists first to prevent duplication
                            stops.clear();
                            stopPositions.clear();
                            markers.clear();

                            processStopsFromCursor(cursor, stops, stopPositions, markers, index);
                            latch.countDown(); // Signal that processing is complete
                        } else {
                            // Stop count didn't match, need to refetch
                            Log.d(TAG, "Stop count mismatch detected. API reports different number of stops than database. Refetching from API...");
                            usesFallback.set(true);

                            // Use a separate thread for the fallback API call
                            Thread fallbackThread = new Thread(() -> {
                                try {
                                    fallbackToApiForStops(db, adjustedBound);

                                    // After fallback is complete, get the updated data
                                    // Clear lists before adding any data to prevent duplication
                                    stops.clear();
                                    stopPositions.clear();
                                    markers.clear();

                                    try (Cursor fallbackCursor = db.rawQuery(query, selectionArgs)) {
                                        if (fallbackCursor.getCount() > 0) {
                                            Log.d(TAG, "Using fresh data from API fetch with " + fallbackCursor.getCount() + " stops");
                                            processStopsFromCursor(fallbackCursor, stops, stopPositions, markers, 0);
                                        } else {
                                            Log.e(TAG, "No stops found even after API fallback for count mismatch");

                                            // Fallback to the original data if API fetch fails
                                            Log.d(TAG, "Falling back to original database data");
                                            cursor.moveToPosition(-1); // Reset cursor position
                                            processStopsFromCursor(cursor, stops, stopPositions, markers, index);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in fallback API call for count mismatch: " + e.getMessage());

                                    // Show a toast with the error
                                    handler.post(() -> Toast.makeText(BusRouteDetailViewActivity.this,
                                        "Network error: Using local data", Toast.LENGTH_SHORT).show());

                                    // Use the original data as fallback - Ensure lists are clear first
                                    stops.clear();
                                    stopPositions.clear();
                                    markers.clear();

                                    try {
                                        cursor.moveToPosition(-1); // Reset cursor position
                                        processStopsFromCursor(cursor, stops, stopPositions, markers, index);
                                    } catch (Exception cursorEx) {
                                        Log.e(TAG, "Error processing original cursor: " + cursorEx.getMessage());
                                    }
                                } finally {
                                    latch.countDown(); // Signal that fallback is complete
                                }
                            });
                            fallbackThread.start();
                        }
                    } else {
                        usesFallback.set(true);
                        // Handle fallback in a way that respects the latch
                        Thread fallbackThread = new Thread(() -> {
                            try {
                                fallbackToApiForStops(db, adjustedBound);
                                // After fallback is complete, get the updated data
                                try (Cursor fallbackCursor = db.rawQuery(query, selectionArgs)) {
                                    if (fallbackCursor.getCount() > 0) {
                                        processStopsFromCursor(fallbackCursor, stops, stopPositions, markers, 0);
                                    } else {
                                        Log.e(TAG, "No stops found even after API fallback");
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error in fallback API call", e);
                            } finally {
                                latch.countDown(); // Signal that fallback is complete
                            }
                        });
                        fallbackThread.start();
                    }
                }

                // Wait for all processing to complete
                try {
                    // Wait with timeout to avoid deadlocks
                    boolean completed = latch.await(30, TimeUnit.SECONDS);
                    if (!completed) {
                        Log.w(TAG, "Timeout waiting for stop data to load completely");
                    }
                    
                    // Special case for GMB with no results - try direct API fallback
                    if (busCompany.equalsIgnoreCase("gmb") && stops.isEmpty()) {
                        Log.d(TAG, "No GMB stops found in database, trying direct API fallback");
                        
                        // Convert bound from "outbound"/"inbound" to "2"/"1" for GMB API
                        String gmbRouteSeq = routeBound.equalsIgnoreCase("inbound") ? "1" : "2";
                        
                        // Ensure we have the gmbRouteID from intent
                        if (gmbRouteID == null || gmbRouteID.isEmpty()) {
                            gmbRouteID = getIntent().getStringExtra("gmbRouteID");
                            Log.d(TAG, "Re-fetched GMB Route ID from intent: " + gmbRouteID);
                        }
                        
                        if (gmbRouteID != null && !gmbRouteID.isEmpty()) {
                            fallbackForGMB(db, gmbRouteID, gmbRouteSeq);
                            // Skip the rest of the processing since fallbackForGMB handles UI updates
                            return;
                        } else {
                            Log.e(TAG, "Missing GMB route ID for API fallback");
                            handler.post(() -> Toast.makeText(BusRouteDetailViewActivity.this,
                                "Missing route information for GMB", Toast.LENGTH_SHORT).show());
                        }
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for stop data", e);
                    Thread.currentThread().interrupt();
                }

                // Log the results for debugging
                Log.d(TAG, "Loaded " + stops.size() + " stops, " + markers.size() + " markers, "
                      + stopPositions.size() + " positions. Used fallback: " + usesFallback.get());

                // Update UI on main thread after all data is ready
                handler.post(() -> {

                    if (stops.isEmpty()) {
                        Toast.makeText(BusRouteDetailViewActivity.this,
                                "No bus stops found for this route", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Debug info for stops and coordinates
                    Log.d(TAG, "Found " + stops.size() + " stops, " + stopPositions.size() + " have coordinates");
                    if (busCompany.equals("gmb") && stopPositions.isEmpty() && !stops.isEmpty()) {
                        Toast.makeText(BusRouteDetailViewActivity.this,
                                "GMB stops loaded but no location data available", Toast.LENGTH_SHORT).show();

                        // Debug what's in the GMB_STOP_LOCATIONS table
                        debugGMBStopLocations(db);
                    }

                    busRouteStopItems.addAll(stops);
                    adapter.notifyDataSetChanged();

                    // Add all markers to map at once
                    if (mMap != null) {
                        for (MarkerOptions marker : markers) {
                            mMap.addMarker(marker);
                        }
                    }

                    // Connect stops on map and draw the route if we have coordinates
                    if (!stopPositions.isEmpty()) {
                        connectPointsOnMap(stopPositions);

                        // Center map on the first stop if available
                        if (mMap != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopPositions.get(0), 15f));
                        }
                    } else if (busCompany.equals("gmb")) {
                        // If no coordinates are available for GMB stops but we have stops, try to fetch coordinates
                        if (!stops.isEmpty()) {
                            Log.d(TAG, "No GMB stop coordinates available, but have " + stops.size() + " stops");
                            Toast.makeText(BusRouteDetailViewActivity.this,
                                    "GMB stops found but location data is missing", Toast.LENGTH_SHORT).show();
                            // You might want to implement a function to fetch GMB stop locations here
                            // fetchGMBStopLocations(stops);
                        }

                        // Center on a default Hong Kong location
                        Log.d(TAG, "Using default map location for GMB");
                        if (mMap != null) {
                            LatLng defaultHongKong = new LatLng(22.302711, 114.177216);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultHongKong, 12f));
                        }
                    }

                    Toast.makeText(BusRouteDetailViewActivity.this,
                            "Loaded " + stops.size() + " bus stops", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching route stops from database", e);
                handler.post(() -> {
                    // Hide loading indicator if you added one
                    // progressBar.setVisibility(View.GONE);

                    Toast.makeText(BusRouteDetailViewActivity.this,
                            "Failed to load bus stops: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                executor.shutdown();
            }
        });
    }

    // Helper method to process stops from cursor
    private void processStopsFromCursor(Cursor cursor, List<BusRouteStopItem> stops,
                                        List<LatLng> stopPositions, List<MarkerOptions> markers, int startIndex) {
        int index = startIndex;
        while (cursor.moveToNext()) {
            String stopId = null, stopNameEn = null, stopNameTc = null , stopNameSc = null , latitude = null, longitude = null;

            // Extract data based on bus company
            if (Objects.equals(busCompany, "kmb")) {
                stopId = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID));
                stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN));
                stopNameTc = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC));
                stopNameSc = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC));
                latitude = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE));
                longitude = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE));
            } else if (busCompany.equalsIgnoreCase("ctb")){ // "ctb" branch
                stopId = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_ROUTE_STOPS.COLUMN_STOP_ID));
                stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_NAME_EN));
                stopNameTc = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_NAME_TC));
                stopNameSc = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_NAME_SC));
                latitude = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_LATITUDE));
                longitude = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_LONGITUDE));
            } else if (busCompany.equals("GMB")){
                try {
                    stopId = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_ROUTE_STOPS.COLUMN_STOP_ID));
                    stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_ROUTE_STOPS.STOP_NAME_EN));
                    stopNameTc = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_ROUTE_STOPS.STOP_NAME_TC));
                    stopNameSc = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_ROUTE_STOPS.STOP_NAME_SC));

                    // Log all column names to help debug
                    String[] columnNames = cursor.getColumnNames();
                    Log.d(TAG, "GMB cursor columns: " + Arrays.toString(columnNames));

                    try {
                        // Get location data from the join with GMB_STOP_LOCATIONS
                        latitude = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_LATITUDE));
                        longitude = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_LONGITUDE));
                        Log.d(TAG, "Found GMB stop location data: lat=" + latitude + ", lng=" + longitude);
                    } catch (Exception e) {
                        // Handle case where location data might be missing
                        Log.e(TAG, "Missing location data for GMB stop " + stopId + ": " + e.getMessage());
                        latitude = null;
                        longitude = null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing GMB stop data: " + e.getMessage());
                    // Set default values to prevent null pointer exceptions
                    stopId = "unknown";
                    stopNameEn = "Unknown Stop";
                    stopNameTc = "未知站";
                    stopNameSc = "未知站";
                    latitude = null;
                    longitude = null;
                }
            }

            Log.d(TAG, "Processing stop " + index + ": ID=" + stopId + ", Name=" + stopNameEn);

            // Create the stop item
            String adjustedBound = (routeBound.equalsIgnoreCase("outbound") || routeBound.equalsIgnoreCase("O")) ? "O" : "I";
            Log.d(TAG, "Stop " + index + " using bound: " + routeBound + " (adjusted to: " + adjustedBound + ")");

            if (busCompany.equalsIgnoreCase("kmb") || busCompany.equalsIgnoreCase("ctb")) {
                stops.add(new BusRouteStopItem(
                        routeNumber, adjustedBound, routeServiceType,
                        stopNameEn, stopNameTc, stopNameSc, stopId, busCompany));
            } else {

                Log.d("LogBusRouteDetailView", "Bus Company: " + busCompany);
                Log.d("LogBusRouteDetailView", "GMB Route ID: " + gmbRouteID);
                Log.d("LogBusRouteDetailView", "GMB Route Seq: " + gmbRouteSeq);

                stops.add(new BusRouteStopItem(
                        routeNumber, adjustedBound, routeServiceType,
                        stopNameEn, stopNameTc, stopNameSc, stopId,
                        gmbRouteID, gmbRouteSeq));
            }

                // Process coordinates if available
                if (latitude != null && !latitude.isEmpty() && longitude != null && !longitude.isEmpty()) {
                    try {
                        double lat = Double.parseDouble(latitude);
                        double lng = Double.parseDouble(longitude);
                        LatLng stopPosition = new LatLng(lat, lng);
                        stopPositions.add(stopPosition);

                        // Create marker for later addition to map
                        float markerHue = (index == 0) ? BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_RED;
                        markers.add(new MarkerOptions()
                                .position(stopPosition)
                                .title(stopNameEn)
                                .icon(BitmapDescriptorFactory.defaultMarker(markerHue)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid coordinates for stop " + stopId + ": " + e.getMessage());
                    }
                }
                index++;
        }
    }

    private void fallbackForGMB(SQLiteDatabase db, String gmbRouteID, String gmbRouteSeq) {
        Log.d(TAG, "Performing API fallback for GMB route ID: " + gmbRouteID + ", sequence: " + gmbRouteSeq);
        
        // Double-check that we have valid route ID
        if (gmbRouteID == null || gmbRouteID.isEmpty()) {
            Log.e(TAG, "GMB Route ID is null or empty in fallbackForGMB method!");
            runOnUiThread(() -> Toast.makeText(this,
                "Error: Missing GMB route information", Toast.LENGTH_SHORT).show());
            return;
        }
        
        try {
            
            // Show loading toast on main thread
            runOnUiThread(() -> Toast.makeText(
                BusRouteDetailViewActivity.this, 
                "Loading GMB stops from API...", 
                Toast.LENGTH_SHORT).show());
            
            // Using a CountDownLatch to block until API response is processed
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean apiSuccess = new AtomicBoolean(false);
            final List<BusRouteStopItem> freshStops = Collections.synchronizedList(new ArrayList<>());
            final List<LatLng> stopPositions = Collections.synchronizedList(new ArrayList<>());
            
            // Create a custom callback handler for the API call
            Consumer<String> onSuccess = jsonData -> {
                try {
                    Log.d(TAG, "GMB API response received: " + (jsonData != null ? "data available" : "no data"));
                    
                    if (jsonData != null && !jsonData.isEmpty()) {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        
                        if (jsonObject.has("data") && !jsonObject.isNull("data")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            
                            if (data.has("route_stops")) {
                                JSONArray routeStops = data.getJSONArray("route_stops");
                                Log.d(TAG, "Found " + routeStops.length() + " GMB stops from API");
                                
                                // Process each stop
                                for (int i = 0; i < routeStops.length(); i++) {
                                    JSONObject stop = routeStops.getJSONObject(i);
                                    
                                    String stopId = stop.optString("stop_id", "unknown");
                                    String stopNameEn = stop.optString("name_en", "Unknown Stop");
                                    String stopNameTc = stop.optString("name_tc", "未知站");
                                    String stopNameSc = stop.optString("name_sc", "未知站");
                                    
                                    // Create stop item
                                    String adjustedBound = (routeBound.equalsIgnoreCase("outbound") || 
                                                         routeBound.equalsIgnoreCase("O")) ? "O" : "I";

                                    Log.d(TAG, "Before insert to database, gmbRouteID: " + gmbRouteID + ", gmbRouteSeq: " + gmbRouteSeq);

                                    BusRouteStopItem item = new BusRouteStopItem(
                                        routeNumber, adjustedBound, routeServiceType,
                                        stopNameEn, stopNameTc, stopNameSc, stopId, gmbRouteID, String.valueOf(gmbRouteSeq));
                                    freshStops.add(item);
                                    
                                    // Try to get location data for the stop
                                    fetchGMBStopLocation(stopId, freshStops.size() - 1, freshStops, stopPositions);
                                }
                                
                                apiSuccess.set(true);
                            } else {
                                Log.e(TAG, "No route_stops array in GMB API response");
                            }
                        } else {
                            Log.e(TAG, "No data object in GMB API response");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing GMB API response: " + e.getMessage(), e);
                } finally {
                    // Signal that processing is complete
                    latch.countDown();
                }
            };
            
            // Make the API call
            dataFetcher.fetchGMBRouteStops(Integer.parseInt(gmbRouteID), Integer.parseInt(gmbRouteSeq), onSuccess);
            
            // Wait for the API response with a timeout
            try {
                boolean completed = latch.await(10, TimeUnit.SECONDS);
                if (!completed) {
                    Log.w(TAG, "Timeout waiting for GMB API response");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for GMB API response", e);
                Thread.currentThread().interrupt();
            }
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (apiSuccess.get() && !freshStops.isEmpty()) {
                    Log.d(TAG, "Updating UI with " + freshStops.size() + " GMB stops from API");
                    
                    // Clear existing items
                    busRouteStopItems.clear();
                    busRouteStopItems.addAll(freshStops);
                    adapter.notifyDataSetChanged();
                    
                    // Update map with any stop positions we gathered
                    if (!stopPositions.isEmpty() && mMap != null) {
                        mMap.clear();
                        
                        // Add markers for each stop
                        for (int i = 0; i < stopPositions.size(); i++) {
                            LatLng stopPosition = stopPositions.get(i);
                            float markerHue = (i == 0) ? BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_RED;
                            
                            // Get the correct stop from freshStops if indices match
                            String stopTitle = "Stop " + (i + 1);
                            if (i < freshStops.size()) {
                                BusRouteStopItem stop = freshStops.get(i);
                                // Use the English name of the stop
                                stopTitle = stop.getStopEn();
                            }
                            
                            mMap.addMarker(new MarkerOptions()
                                .position(stopPosition)
                                .title(stopTitle)
                                .icon(BitmapDescriptorFactory.defaultMarker(markerHue)));
                        }
                        
                        // Draw route line if we have multiple stops
                        if (stopPositions.size() >= 2) {
                            connectPointsOnMap(stopPositions);
                        }
                        
                        // Center map on first stop
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopPositions.get(0), 15f));
                    } else {
                        Log.d(TAG, "No stop positions available for GMB route");
                        // Use a default Hong Kong location
                        if (mMap != null) {
                            LatLng defaultHongKong = new LatLng(22.302711, 114.177216);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultHongKong, 12f));
                        }
                    }
                    
                    Toast.makeText(BusRouteDetailViewActivity.this,
                        "Loaded " + freshStops.size() + " GMB bus stops from API", 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Failed to fetch GMB stops from API");
                    Toast.makeText(BusRouteDetailViewActivity.this,
                        "Failed to load GMB bus stops", Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid route ID or sequence: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(
                BusRouteDetailViewActivity.this,
                "Error: Invalid GMB route information", 
                Toast.LENGTH_SHORT).show());
        }
    }
    
    // Helper method to fetch location data for a GMB stop
    private void fetchGMBStopLocation(String stopId, int index, List<BusRouteStopItem> stops, List<LatLng> positions) {
        if (stopId == null || stopId.isEmpty() || stopId.equals("unknown")) {
            Log.d(TAG, "Invalid stop ID for location fetch");
            return;
        }
        
        // Check if we already have this stop location in the database
        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            String query = "SELECT * FROM " + GMBDatabase.Tables.GMB_STOP_LOCATIONS.TABLE_NAME + 
                          " WHERE " + GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_STOP_ID + " = ?";
            
            try (Cursor cursor = db.rawQuery(query, new String[]{stopId})) {
                if (cursor.moveToFirst()) {
                    // We have location data in the database
                    @SuppressLint("Range") String latitude = cursor.getString(
                        cursor.getColumnIndex(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_LATITUDE));
                    @SuppressLint("Range") String longitude = cursor.getString(
                        cursor.getColumnIndex(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_LONGITUDE));
                    
                    if (latitude != null && !latitude.isEmpty() && longitude != null && !longitude.isEmpty()) {
                        try {
                            double lat = Double.parseDouble(latitude);
                            double lng = Double.parseDouble(longitude);
                            LatLng stopPosition = new LatLng(lat, lng);
                            
                            // Only add if index is valid
                            if (index >= 0 && index < stops.size()) {
                                synchronized (positions) {
                                    // Ensure positions list has enough entries
                                    while (positions.size() <= index) {
                                        positions.add(null);
                                    }
                                    positions.set(index, stopPosition);
                                }
                            }
                            return; // We found and processed the location, no need for API call
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Invalid coordinates in database: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking database for stop location: " + e.getMessage());
        }
        
        // If we get here, we need to fetch from API - use the DataFetcher method
        Log.d(TAG, "No location data in database for GMB stop " + stopId + ", fetching from API");
        
        // Create a listener for the database updates
        // This will monitor the database for changes to this stop's location data
        Thread dbMonitorThread = new Thread(() -> {
            boolean locationFound = false;
            for (int attempt = 0; attempt < 5; attempt++) { // Try for a reasonable amount of time
                try {
                    Thread.sleep(1000); // Check every second
                    
                    SQLiteDatabase db = databaseHelper.getReadableDatabase();
                    String query = "SELECT * FROM " + GMBDatabase.Tables.GMB_STOP_LOCATIONS.TABLE_NAME + 
                                  " WHERE " + GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_STOP_ID + " = ?";
                    
                    try (Cursor cursor = db.rawQuery(query, new String[]{stopId})) {
                        if (cursor.moveToFirst()) {
                            // Check if we have valid location data
                            @SuppressLint("Range") String latitude = cursor.getString(
                                cursor.getColumnIndex(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_LATITUDE));
                            @SuppressLint("Range") String longitude = cursor.getString(
                                cursor.getColumnIndex(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_LONGITUDE));
                            
                            if (latitude != null && !latitude.isEmpty() && longitude != null && !longitude.isEmpty()) {
                                try {
                                    double lat = Double.parseDouble(latitude);
                                    double lng = Double.parseDouble(longitude);
                                    LatLng stopPosition = new LatLng(lat, lng);
                                    
                                    // Only add if index is valid
                                    if (index >= 0 && index < stops.size()) {
                                        synchronized (positions) {
                                            // Ensure positions list has enough entries
                                            while (positions.size() <= index) {
                                                positions.add(null);
                                            }
                                            positions.set(index, stopPosition);
                                        }
                                        
                                        // If we have enough positions, update the map
                                        if (positions.size() >= 2) {
                                            final List<LatLng> validPositions = new ArrayList<>();
                                            synchronized (positions) {
                                                for (LatLng pos : positions) {
                                                    if (pos != null) {
                                                        validPositions.add(pos);
                                                    }
                                                }
                                            }
                                            
                                            if (validPositions.size() >= 2) {
                                                handler.post(() -> {
                                                    if (mMap != null) {
                                                        connectPointsOnMap(validPositions);
                                                    }
                                                });
                                            }
                                        }
                                        
                                        locationFound = true;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Invalid coordinates in database: " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error monitoring database for GMB stop location: " + e.getMessage());
                }
            }
            
            if (!locationFound) {
                Log.d(TAG, "No location data found for GMB stop " + stopId + " after waiting");
            }
        });
        dbMonitorThread.setDaemon(true);
        dbMonitorThread.start();
        
        // Use DataFetcher to make the API call
        dataFetcher.fetchGMBStopLocation(stopId);
    }

    // This is a simplified version of the fallbackToApiForStops method
    private void fallbackToApiForStops(SQLiteDatabase db, String adjustedBound) {
        String stopIDQuery;
        String[] stopIDSelectionArgs = null;
        Log.e(TAG, "No matched data from database, fetching from API instead");

        // Set up the query based on bus company
        boolean isCTB = busCompany.equalsIgnoreCase("ctb");
        boolean isKMB = busCompany.equalsIgnoreCase("kmb");
        boolean isGMB = busCompany.equalsIgnoreCase("gmb");

        if (isCTB) {
            stopIDQuery = CTBDatabase.Queries.QUERY_GET_STOPID_FROM_ROUTEBOUND;
            stopIDSelectionArgs = new String[]{routeNumber, adjustedBound};
        } else if (isKMB) {
            stopIDQuery = KMBDatabase.Queries.QUERY_GET_STOPID_FROM_ROUTEBOUND;
            stopIDSelectionArgs = new String[]{routeNumber, adjustedBound};
        } else if (isGMB) {
            // For GMB, use the specialized GMB fallback method
            Log.d(TAG, "Using specialized GMB fallback method");
            
            // Convert bound from "I"/"O" to "1"/"2" for GMB API
            String gmbRouteSeq = adjustedBound.equalsIgnoreCase("I") ? "1" : "2";
            
            // Ensure we have the gmbRouteID from intent
            if (gmbRouteID == null || gmbRouteID.isEmpty()) {
                gmbRouteID = getIntent().getStringExtra("gmbRouteID");
                Log.d(TAG, "Re-fetched GMB Route ID from intent: " + gmbRouteID);
            }
            
            if (gmbRouteID != null && !gmbRouteID.isEmpty()) {
                fallbackForGMB(db, gmbRouteID, gmbRouteSeq);
            } else {
                Log.e(TAG, "Missing GMB route ID for API fallback");
                Toast.makeText(this, "Missing route information for GMB", Toast.LENGTH_SHORT).show();
            }
            return;
        } else {
            Log.e(TAG, "Unknown bus company: Are you from other world? :" + busCompany);
            return;
        }

        try (Cursor stopIDCursor = db.rawQuery(stopIDQuery, stopIDSelectionArgs)) {
            if (stopIDCursor.getCount() == 0) {
                Log.e(TAG, "No stop IDs found for route? Did you fetch all the routes? " + routeNumber);
                return;
            }

            final CountDownLatch latch = new CountDownLatch(stopIDCursor.getCount());
            final List<StopWithSequence> apiStopsWithSequence = Collections.synchronizedList(new ArrayList<>());
            final List<LatLng> stopPositions = Collections.synchronizedList(new ArrayList<>());
            final List<MarkerOptions> markers = Collections.synchronizedList(new ArrayList<>());

            while (stopIDCursor.moveToNext()) {
                // Common variables across both branches
                String stopIDQueryResult = null;
                int sequenceNumber = 0;
                final int index = stopIDCursor.getPosition(); // For marker color

                // Extract stop ID and sequence number based on bus company
                if (isCTB) {
                    stopIDQueryResult = stopIDCursor.getString(
                            stopIDCursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_ROUTE_STOPS.COLUMN_STOP_ID));
                    sequenceNumber = stopIDCursor.getInt(
                            stopIDCursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_ROUTE_STOPS.COLUMN_STOP_SEQ));
                } else if (isKMB) { // Must be KMB
                    stopIDQueryResult = stopIDCursor.getString(
                            stopIDCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID));
                    sequenceNumber = stopIDCursor.getInt(
                            stopIDCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ));
                } else {
                    Log.e(TAG, "Unknown bus company: How do you get here? :" + busCompany);
                }

                // Store these as final variables for use in lambdas
                final String finalStopID = stopIDQueryResult;
                final int finalSequence = sequenceNumber;

                // Use the appropriate fetch method based on company
                if (isCTB) {
                    dataFetcher.fetchCTBStop(finalStopID,
                            response -> processStopData(response, finalStopID, finalSequence, index, "1",
                                    apiStopsWithSequence, stopPositions, markers, latch, adjustedBound),
                            error -> handleFetchError(error, latch));
                } else if (isKMB) { // Must be KMB
                    dataFetcher.fetchKMBStop(finalStopID,
                            response -> processStopData(response, finalStopID, finalSequence, index, routeServiceType,
                                    apiStopsWithSequence, stopPositions, markers, latch, adjustedBound),
                            error -> handleFetchError(error, latch));
                }
            }

            // Wait for all API calls to complete (with timeout)
            try {
                if (latch.await(10, TimeUnit.SECONDS)) {
                    // Sort stops by their sequence
                    handler.post(() -> {
                        // Sort by sequence number
                        Collections.sort(apiStopsWithSequence,
                                (a, b) -> Integer.compare(a.sequence, b.sequence));

                        // Extract just the stop items in correct order
                        List<BusRouteStopItem> sortedStops = new ArrayList<>();
                        for (StopWithSequence sws : apiStopsWithSequence) {
                            sortedStops.add(sws.stopItem);
                        }

                        // Clear existing items before adding new ones to prevent duplication
                        busRouteStopItems.clear();
                        busRouteStopItems.addAll(sortedStops);
                        adapter.notifyDataSetChanged();

                        // Clear existing markers from map before adding new ones
                        if (mMap != null) {
                            mMap.clear();
                            for (MarkerOptions marker : markers) {
                                mMap.addMarker(marker);
                            }
                        }

                        // Connect stops on map and draw the route
                        connectPointsOnMap(stopPositions);
                        
                        // Log success message
                        Log.d(TAG, "Successfully fetched and processed " + sortedStops.size() + " stops from API");
                    });
                } else {
                    Log.e(TAG, "Timeout waiting for API responses");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for API responses", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    // Helper method to process API stop data response
    private void processStopData(String response, String stopID, int sequence, int index,
                                 String serviceType, List<StopWithSequence> apiStopsWithSequence,
                                 List<LatLng> stopPositions, List<MarkerOptions> markers,
                                 CountDownLatch latch, String adjustedBound) {
        try {
            if (response == null || response.isEmpty()) {
                Log.e(TAG, "Empty response for stop " + stopID);
                return;
            }
            
            JSONObject jsonResponse = new JSONObject(response);
            if (!jsonResponse.has("data")) {
                Log.e(TAG, "Invalid response format for stop " + stopID + ": " + response);
                return;
            }
            
            JSONObject stopData = jsonResponse.getJSONObject("data");
            Log.d(TAG, "API result for stop " + stopID + " (seq: " + sequence + "): " + response);

            // Verify all required fields are present
            if (!stopData.has("name_en") || !stopData.has("name_tc") || 
                !stopData.has("name_sc") || !stopData.has("lat") || !stopData.has("long")) {
                Log.e(TAG, "Missing required fields in stop data for " + stopID);
                return;
            }

            String stopNameEn = stopData.getString("name_en");

            // Create a stop item with sequence information
            BusRouteStopItem stopItem = new BusRouteStopItem(
                    routeNumber, adjustedBound, serviceType,
                    stopNameEn,
                    stopData.getString("name_tc"),
                    stopData.getString("name_sc"),
                    stopID, busCompany);

            // Store both the stop item and its sequence for later sorting
            synchronized (apiStopsWithSequence) {
                apiStopsWithSequence.add(new StopWithSequence(stopItem, sequence));
                Log.d(TAG, "Added stop " + stopID + " with sequence " + sequence + " (total: " + apiStopsWithSequence.size() + ")");
            }

            try {
                double lat = Double.parseDouble(stopData.getString("lat"));
                double lng = Double.parseDouble(stopData.getString("long"));
                LatLng stopPosition = new LatLng(lat, lng);

                // Store position and marker with proper synchronization
                synchronized (stopPositions) {
                    stopPositions.add(stopPosition);

                    // Create marker
                    float markerHue = (index == 0) ? BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_RED;
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(stopPosition)
                            .title(stopNameEn)
                            .snippet("Stop #" + sequence)
                            .icon(BitmapDescriptorFactory.defaultMarker(markerHue));
                    
                    markers.add(markerOptions);
                    Log.d(TAG, "Added marker for stop " + stopID + " at " + lat + "," + lng);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid coordinates for stop " + stopID + ": " + e.getMessage());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing stop data: " + e.getMessage() + ", Response: " + response);
        } finally {
            latch.countDown();
            Log.d(TAG, "Countdown latch decremented for stop " + stopID + " (remaining: " + latch.getCount() + ")");
        }
    }

    // Helper method to handle API fetch errors
    private void handleFetchError(String error, CountDownLatch latch) {
        Log.e(TAG, "Error fetching stop data: " + error);
        latch.countDown();
    }

    // Debug method to check what's in the GMB_STOP_LOCATIONS table
    private void debugGMBStopLocations(SQLiteDatabase db) {
        try {
            Log.d(TAG, "Debugging GMB_STOP_LOCATIONS table:");
            String query = "SELECT * FROM " + GMBDatabase.Tables.GMB_STOP_LOCATIONS.TABLE_NAME + " LIMIT 10";
            
            try (Cursor cursor = db.rawQuery(query, null)) {
                Log.d(TAG, "Found " + cursor.getCount() + " entries in GMB_STOP_LOCATIONS table");
                
                if (cursor.getCount() > 0) {
                    String[] columnNames = cursor.getColumnNames();
                    Log.d(TAG, "Columns: " + Arrays.toString(columnNames));
                    
                    while (cursor.moveToNext()) {
                        try {
                            String stopId = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_STOP_ID));
                            String lat = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_LATITUDE));
                            String lng = cursor.getString(cursor.getColumnIndexOrThrow(GMBDatabase.Tables.GMB_STOP_LOCATIONS.COLUMN_LONGITUDE));
                            
                            Log.d(TAG, "GMB Location: Stop ID=" + stopId + ", Lat=" + lat + ", Lng=" + lng);
                        } catch (Exception e) {
                            Log.e(TAG, "Error retrieving GMB stop location data: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error debugging GMB_STOP_LOCATIONS: " + e.getMessage());
        }
    }
    
    // Helper class to associate a stop with its sequence number
    private static class StopWithSequence {
        final BusRouteStopItem stopItem;
        final int sequence;
        final LatLng position; // Optional, if you want to track position with sequence
        final MarkerOptions marker; // Optional, if you want to track marker with sequence

        StopWithSequence(BusRouteStopItem stopItem, int sequence) {
            this.stopItem = stopItem;
            this.sequence = sequence;
            this.position = null;
            this.marker = null;
        }

        // Constructor with position and marker for complete tracking
        StopWithSequence(BusRouteStopItem stopItem, int sequence, LatLng position, MarkerOptions marker) {
            this.stopItem = stopItem;
            this.sequence = sequence;
            this.position = position;
            this.marker = marker;
        }
    }

    private void connectPointsOnMap(final List<LatLng> stopPositions) {
        if (stopPositions == null || stopPositions.isEmpty()) {
            Log.e(TAG, "No stop positions to connect");
            return;
        }

        // If the map is not yet ready, store positions for later use
        if (mMap == null) {
            Log.d(TAG, "Map not ready, storing positions for later");
            pendingStopPositions = new ArrayList<>(stopPositions);
            return;
        }

        runOnUiThread(() -> drawRouteOnMap(stopPositions));
    }

    private void drawRouteOnMap(List<LatLng> stopPositions) {
        Log.d(TAG, "Drawing route with " + stopPositions.size() + " points");

        if (stopPositions.size() < 2) {
            Log.e(TAG, "At least two points are needed to draw a route.");
            return;
        }

        try {
            // Center the map on the user-selected initial stop
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopPositions.get(initialStopSeqView), 16f));

            // Add markers for each stop
            for (int i = 0; i < stopPositions.size(); i++) {
                LatLng stop = stopPositions.get(i);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(stop)
                        .title("Stop " + (i + 1))
                        .snippet("Lat: " + stop.latitude + ", Lng: " + stop.longitude);
                mMap.addMarker(markerOptions);
            }

            // Get the origin (first stop) and final destination (last stop)
            String origin = stopPositions.get(0).latitude + "," + stopPositions.get(0).longitude;
            String finalDestination = stopPositions.get(stopPositions.size() - 1).latitude + "," + 
                                    stopPositions.get(stopPositions.size() - 1).longitude;

            // Break intermediate stops into batches of 23 (25 total including origin and destination)
            List<List<LatLng>> batches = new ArrayList<>();
            // Skip first point as it's the origin
            List<LatLng> intermediatePoints = stopPositions.subList(1, stopPositions.size() - 1);
            
            for (int i = 0; i < intermediatePoints.size(); i += 23) {
                int end = Math.min(i + 23, intermediatePoints.size());
                batches.add(intermediatePoints.subList(i, end));
            }

            // Process each batch
            for (List<LatLng> batch : batches) {
                StringBuilder waypoints = new StringBuilder();
                waypoints.append("waypoints=");
                for (int i = 0; i < batch.size(); i++) {
                    waypoints.append(batch.get(i).latitude)
                            .append(",")
                            .append(batch.get(i).longitude);
                    if (i < batch.size() - 1) {
                        waypoints.append("|");
                    }
                }
                Log.d(TAG, "Waypoints URL: " + waypoints);

                String apiKey = BuildConfig.MAPS_API_KEY;
                String requestUrl = "https://maps.googleapis.com/maps/api/directions/json?"
                        + "origin=" + origin
                        + "&destination=" + finalDestination
                        + "&" + waypoints.toString()
                        + "&key=" + apiKey;

                // Execute API request in a background thread
                new FetchRouteTask().execute(requestUrl);
            }
            String destination = stopPositions.get(stopPositions.size() - 1).latitude + "," + stopPositions.get(stopPositions.size() - 1).longitude;

            StringBuilder waypoints = new StringBuilder();
            if (stopPositions.size() > 2) {
                waypoints.append("waypoints=");
                for (int i = 1; i < stopPositions.size() - 1; i++) {
                    waypoints.append(stopPositions.get(i).latitude)
                            .append(",")
                            .append(stopPositions.get(i).longitude);
                    if (i < stopPositions.size() - 2) {
                        waypoints.append("|");
                    }
                }
                Log.d(TAG, "Waypoints URL: " + waypoints);
            }

            String apiKey = BuildConfig.MAPS_API_KEY; // Replace with your API key
            String requestUrl = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "origin=" + origin
                    + "&destination=" + destination
                    + (waypoints.length() > 0 ? "&" + waypoints.toString() : "")
                    + "&key=" + apiKey;

            // Execute API request in a background thread
            new FetchRouteTask().execute(requestUrl);

        } catch (Exception e) {
            Log.e(TAG, "Error fetching route: " + e.getMessage(), e);
        }
    }

    private class FetchRouteTask {
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        public void execute(String url) {
            executorService.execute(() -> {
                try {
                    Log.d(TAG, "Fetching route from " + url);
                    String response = fetchRouteData(url);
                    if (response != null) {
                        mainHandler.post(() -> onPostExecute(response));
                    } else {
                        Log.e(TAG, "Failed to get route data");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching route: " + e.getMessage(), e);
                }
            });
        }

        private String fetchRouteData(String urlString) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route data: " + e.getMessage(), e);
                return null;
            }
        }

        private void onPostExecute(String result) {
            if (result == null) {
                Log.e(TAG, "Failed to get route data");
                return;
            }

            try {
                JSONObject jsonResponse = new JSONObject(result);
                JSONArray routes = jsonResponse.getJSONArray("routes");
                if (routes.length() == 0) {
                    Log.e(TAG, "No routes found");
                    return;
                }

                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");

                List<LatLng> routePoints = decodePolyline(encodedPolyline);

                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(routePoints)
                        .color(Color.RED)
                        .width(15f)
                        .geodesic(true);

                mMap.addPolyline(polylineOptions);
                Log.d(TAG, "Route drawn successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error parsing route JSON: " + e.getMessage(), e);
            }
        }

        private List<LatLng> decodePolyline(String encoded) {
            List<LatLng> polyline = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1F) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1F) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
                polyline.add(p);
            }
            return polyline;
        }
    }


}