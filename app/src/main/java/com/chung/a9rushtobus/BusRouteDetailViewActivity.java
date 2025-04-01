package com.chung.a9rushtobus;

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

public class BusRouteDetailViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BusRouteStopItemAdapter adapter;
    private RecyclerView busRouteStopRecyclerView;
    private List<BusRouteStopItem> busRouteStopItems;
    private String routeNumber, routeDestination, gmbRouteID, routeBound, routeServiceType, busCompany, remarks;
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

        assert busCompany != null;
        if (busCompany.equals("gmb")) {
            gmbRouteID = getIntent().getStringExtra("gmbRouteID");
        }


        String remarks = getIntent().getStringExtra("remarks");
        if (remarks == null || remarks.isEmpty()) {
            remarks = "Normal Route";
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

        busRouteRemarks.setText(remarks);

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

                // Set up query based on bus company
                if (Objects.equals(busCompany, "kmb")) {
                    query = KMBDatabase.Queries.QUERY_GET_STOP_FROM_ROUTE;
                    selectionArgs = new String[]{routeNumber, adjustedBound, routeServiceType};
                    Log.d(TAG, "Using KMB query with route: " + routeNumber + ", bound: " + adjustedBound + ", service type: " + routeServiceType);
                } else if (Objects.equals(busCompany, "ctb")) {
                    query = CTBDatabase.Queries.QUERY_GET_STOP_FROM_ROUTE;
                    selectionArgs = new String[]{routeNumber, adjustedBound};
                    Log.d(TAG, "Using CTB query with route: " + routeNumber + ", bound: " + adjustedBound);
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

                        boolean isMatchOrOffline;
                        try {
                            isMatchOrOffline = dataFetcher.isStopNumberMatch(cursor.getCount(), routeNumber, routeBound, routeServiceType, busCompany);
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
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for stop data", e);
                    Thread.currentThread().interrupt();
                }

                // Log the results for debugging
                Log.d(TAG, "Loaded " + stops.size() + " stops, " + markers.size() + " markers, " 
                      + stopPositions.size() + " positions. Used fallback: " + usesFallback.get());

                // Update UI on main thread after all data is ready
                handler.post(() -> {
                    // Hide loading indicator if you added one
                    // progressBar.setVisibility(View.GONE);
                    
                    if (stops.isEmpty()) {
                        Toast.makeText(BusRouteDetailViewActivity.this, 
                                "No bus stops found for this route", Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    busRouteStopItems.addAll(stops);
                    adapter.notifyDataSetChanged();

                    // Add all markers to map at once
                    if (mMap != null) {
                        for (MarkerOptions marker : markers) {
                            mMap.addMarker(marker);
                        }
                    }

                    // Connect stops on map and draw the route
                    connectPointsOnMap(stopPositions);

                    // Center map on the first stop if available
                    if (!stopPositions.isEmpty() && mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopPositions.get(0), 15f));
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
            String stopId, stopNameEn, stopNameTc, stopNameSc, latitude, longitude;

            // Extract data based on bus company
            if (Objects.equals(busCompany, "kmb")) {
                stopId = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID));
                stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN));
                stopNameTc = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC));
                stopNameSc = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC));
                latitude = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE));
                longitude = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE));
            } else { // "ctb" branch
                stopId = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_ROUTE_STOPS.COLUMN_STOP_ID));
                stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_NAME_EN));
                stopNameTc = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_NAME_TC));
                stopNameSc = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_NAME_SC));
                latitude = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_LATITUDE));
                longitude = cursor.getString(cursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_STOPS.COLUMN_LONGITUDE));
            }

            Log.d(TAG, "Processing stop " + index + ": ID=" + stopId + ", Name=" + stopNameEn);

            // Create the stop item
            String adjustedBound = (routeBound.equalsIgnoreCase("outbound") || routeBound.equalsIgnoreCase("O")) ? "O" : "I";
            Log.d(TAG, "Stop " + index + " using bound: " + routeBound + " (adjusted to: " + adjustedBound + ")");

            stops.add(new BusRouteStopItem(
                    routeNumber, adjustedBound, routeServiceType,
                    stopNameEn, stopNameTc, stopNameSc, stopId, busCompany));

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
            stopIDQuery = GMBDatabase.Queries.QUERY_STOPS_BY_ROUTE_ID;

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
                String stopIDQueryResult;
                int sequenceNumber;
                final int index = stopIDCursor.getPosition(); // For marker color

                // Extract stop ID and sequence number based on bus company
                if (isCTB) {
                    stopIDQueryResult = stopIDCursor.getString(
                            stopIDCursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_ROUTE_STOPS.COLUMN_STOP_ID));
                    sequenceNumber = stopIDCursor.getInt(
                            stopIDCursor.getColumnIndexOrThrow(CTBDatabase.Tables.CTB_ROUTE_STOPS.COLUMN_STOP_SEQ));
                } else { // Must be KMB
                    stopIDQueryResult = stopIDCursor.getString(
                            stopIDCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID));
                    sequenceNumber = stopIDCursor.getInt(
                            stopIDCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ));
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
                } else { // Must be KMB
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