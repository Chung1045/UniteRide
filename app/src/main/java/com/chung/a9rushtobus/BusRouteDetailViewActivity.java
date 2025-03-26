package com.chung.a9rushtobus;

import android.app.UiModeManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BusRouteDetailViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BusRouteStopItemAdapter adapter;
    private RecyclerView busRouteStopRecyclerView;
    private List<BusRouteStopItem> busRouteStopItems;
    private String routeNumber, routeDestination, routeBound, routeServiceType;
    private Utils utils;
    private DataFetcher dataFetcher;
    private DatabaseHelper databaseHelper;
    private Handler handler = new Handler();
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

        View v = findViewById(R.id.activity_bus_detail_constraint_layout);

        utils = new Utils(this, v, this);

        routeNumber = getIntent().getStringExtra("route");
        routeDestination = getIntent().getStringExtra("destination");
        if (Objects.equals(getIntent().getStringExtra("bound"), "O")) {
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
        if (UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_DARK, false)
                || (UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_FOLLOW_SYSTEM, false)
                && (uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES))) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_night_theme));
        }

        // Default points until we get real data
        LatLng defaultPoint = new LatLng(22.345415, 114.192640);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPoint, 16f));

        // We'll update the map with real stop data when it's loaded
    }

    public void initView() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        busRouteStopRecyclerView = findViewById(R.id.recyclerView2);
        TextView busRouteNumber = findViewById(R.id.bus_detail_activity_route_number);
        TextView busRouteDestination = findViewById(R.id.bus_detail_activity_route_dest);
        busRouteDestination.setSingleLine(true);
        busRouteDestination.setMarqueeRepeatLimit(-1);
        busRouteDestination.setScroller(new Scroller(this));
        busRouteDestination.setMovementMethod(LinkMovementMethod.getInstance());
        busRouteDestination.setSelected(true);

        busRouteNumber.setText(routeNumber);
        busRouteDestination.setText(routeDestination);
        Log.d("LogBusRouteDetailView", "Route: " + routeNumber + " Destination: " + routeDestination + " Bound: " + routeBound + " Service Type: " + routeServiceType);

        // Initialize the RecyclerView immediately with an empty list
        busRouteStopItems = new ArrayList<>();
        adapter = new BusRouteStopItemAdapter(this, busRouteStopItems, utils);
        getLifecycle().addObserver(adapter);
        busRouteStopRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        busRouteStopRecyclerView.setAdapter(adapter);

        // Then load the data
        loadBusRouteStopData();
    }

    public void initListener() {
        ImageView backBtn = findViewById(R.id.bus_detail_activity_back_button);
        backBtn.setOnClickListener(v -> finish());
    }


    public void loadBusRouteStopData() {

        Log.d("BusRouteDetailView", "Loading bus route stops");
        busRouteStopItems.clear();
        List<LatLng> stopPositions = new ArrayList<>();

        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();

            // Query to get all stops for this route in sequence order with DISTINCT to prevent duplicates
            String query = "SELECT DISTINCT rs." + DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + ", " +
                    "rs." + DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + ", " +
                    "s." + DatabaseHelper.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN + ", " +
                    "s." + DatabaseHelper.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC + ", " +
                    "s." + DatabaseHelper.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC + ", " +
                    "s." + DatabaseHelper.Tables.KMB_STOPS.COLUMN_LATITUDE + ", " +
                    "s." + DatabaseHelper.Tables.KMB_STOPS.COLUMN_LONGITUDE +
                    " FROM " + DatabaseHelper.Tables.KMB_ROUTE_STOPS.TABLE_NAME + " rs" +
                    " JOIN " + DatabaseHelper.Tables.KMB_STOPS.TABLE_NAME + " s" +
                    " ON rs." + DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + " = s." + DatabaseHelper.Tables.KMB_STOPS.COLUMN_STOP_ID +
                    " WHERE rs." + DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE + " = ?" +
                    " AND rs." + DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_BOUND + " = ?" +
                    " AND rs." + DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE + " = ?" +
                    " GROUP BY rs." + DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + " " +
                    " ORDER BY CAST(rs." + DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + " AS INTEGER)";

            if (routeBound.equals("outbound")) {
                routeBound = "O";
            } else {
                routeBound = "I";
            }

            String[] selectionArgs = {routeNumber, routeBound, routeServiceType};

            Cursor cursor = db.rawQuery(query, selectionArgs);

            int index = 0;
            Log.d("BusRouteDetailView", "Found " + cursor.getCount() + " stops for route " + routeNumber);
            if (cursor.moveToFirst()) {
                do {
                    // Extract data from cursor
                    String stopId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID));
                    String stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN));
                    String stopNameTc = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC));
                    String stopNameSc = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC));
                    String latitude = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.KMB_STOPS.COLUMN_LATITUDE));
                    String longitude = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.KMB_STOPS.COLUMN_LONGITUDE));
                    
                    Log.d("BusRouteDetailView", "Processing stop " + index + ": ID=" + stopId + 
                        ", Name=" + stopNameEn + ", Lat=" + latitude + ", Long=" + longitude);

                    // Add to bus route stop items list
                    busRouteStopItems.add(new BusRouteStopItem(
                            routeNumber,
                            routeBound,
                            routeServiceType,
                            stopNameEn,
                            stopNameTc,
                            stopNameSc,
                            stopId));

                    // Add stop position to map if latitude and longitude are available
                    if (latitude != null && !latitude.isEmpty() && longitude != null && !longitude.isEmpty()) {
                        try {
                            double lat = Double.parseDouble(latitude);
                            double lng = Double.parseDouble(longitude);
                            LatLng stopPosition = new LatLng(lat, lng);
                            stopPositions.add(stopPosition);

                            // Add marker to map
                            if (mMap != null) {
                                mMap.addMarker(new MarkerOptions()
                                        .position(stopPosition)
                                        .title(stopNameEn)
                                        .icon(BitmapDescriptorFactory.defaultMarker(index == 0 ?
                                                BitmapDescriptorFactory.HUE_BLUE :
                                                BitmapDescriptorFactory.HUE_RED)));
                            }
                        } catch (NumberFormatException e) {
                            Log.e("BusRouteDetailView", "Invalid coordinates: " + e.getMessage());
                        }
                    }

                    index++;
                } while (cursor.moveToNext());
            }

            cursor.close();

            // Update UI
            adapter.notifyDataSetChanged();
            connectPointsOnMap(stopPositions);

            // Center map on first stop if available
            if (!stopPositions.isEmpty() && mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopPositions.get(0), 15f));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error fetching route stops from database", e);
            Log.e(TAG, "Stack trace: ", e);
            // Show error message to user
            Toast.makeText(this, "Failed to load bus stops from database: " + e.getMessage(), Toast.LENGTH_LONG).show();

            Log.d(TAG, "Falling back to API data fetch");
            // Fallback to API if database fetch fails
            fetchRouteStopsFromAPI();
        }
    }

    private void fetchRouteStopsFromAPI() {
        dataFetcher.fetchRouteStopInfo(
                routeNumber,
                "kmb",
                routeBound,
                routeServiceType,
                routeStopInfo -> {
                    // Process the route stop info
                    busRouteStopItems.clear();
                    List<LatLng> stopPositions = new ArrayList<>();

                    for (int i = 0; i < routeStopInfo.length(); i++) {
                        try {
                            JSONObject stopInfo = routeStopInfo.getJSONObject(i);
                            busRouteStopItems.add(new BusRouteStopItem(
                                    routeNumber,
                                    routeBound,
                                    routeServiceType,
                                    stopInfo.getString("name_en"),
                                    stopInfo.getString("name_tc"),
                                    stopInfo.getString("name_sc"),
                                    stopInfo.getString("stopID")));

                            // Add stop position to map if latitude and longitude are available
                            if (stopInfo.has("lat") && stopInfo.has("long")) {
                                try {
                                    double lat = Double.parseDouble(stopInfo.getString("lat"));
                                    double lng = Double.parseDouble(stopInfo.getString("long"));
                                    LatLng stopPosition = new LatLng(lat, lng);
                                    stopPositions.add(stopPosition);

                                    // Add marker to map
                                    if (mMap != null) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(stopPosition)
                                                .title(stopInfo.getString("name_en"))
                                                .icon(BitmapDescriptorFactory.defaultMarker(i == 0 ?
                                                        BitmapDescriptorFactory.HUE_BLUE :
                                                        BitmapDescriptorFactory.HUE_RED)));
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e("BusRouteDetailView", "Invalid coordinates: " + e.getMessage());
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("BusRouteDetailView", "Error parsing stop info: " + e.getMessage());
                        }
                    }

                    // Update UI
                    adapter.notifyDataSetChanged();

                    // Center map on first stop if available
                    if (!stopPositions.isEmpty() && mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopPositions.get(0), 15f));
                    }
                },
                error -> {
                    // Handle error
                    Log.e("BusRouteDetailView", "Error fetching route stops: " + error);
                    // Show error message to user
                    Toast.makeText(this, "Failed to load bus stops: " + error, Toast.LENGTH_LONG).show();
                }
        );
    }

    private void connectPointsOnMap(List<LatLng> stopPositions) {
        if (mMap != null && !stopPositions.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(stopPositions)
                    .color(Color.RED)
                    .width(10f);

            mMap.addPolyline(polylineOptions);
        }
    }
}