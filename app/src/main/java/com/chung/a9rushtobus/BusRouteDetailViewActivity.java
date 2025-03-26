package com.chung.a9rushtobus;

import android.app.UiModeManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
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

import com.chung.a9rushtobus.database.DatabaseHelper;
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
import java.util.List;
import java.util.Objects;

public class BusRouteDetailViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BusRouteStopItemAdapter adapter;
    private RecyclerView busRouteStopRecyclerView;
    private List<BusRouteStopItem> busRouteStopItems;
    private String routeNumber, routeDestination, routeBound, routeServiceType;
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

        View v = findViewById(R.id.activity_bus_detail_constraint_layout);

        utils = new Utils(this, v, this);

        routeNumber = getIntent().getStringExtra("route");
        routeDestination = getIntent().getStringExtra("destination");
        initialStopSeqView = getIntent().getIntExtra("initialStopSeqView", 0);
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

        if (pendingStopPositions != null) {
            drawRouteOnMap(pendingStopPositions);
            pendingStopPositions = null;
        }

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
            String query = "SELECT DISTINCT rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + ", " +
                    "rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + ", " +
                    "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN + ", " +
                    "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC + ", " +
                    "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC + ", " +
                    "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE + ", " +
                    "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE +
                    " FROM " + KMBDatabase.Tables.KMB_ROUTE_STOPS.TABLE_NAME + " rs" +
                    " JOIN " + KMBDatabase.Tables.KMB_STOPS.TABLE_NAME + " s" +
                    " ON rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + " = s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_ID +
                    " WHERE rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE + " = ?" +
                    " AND rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_BOUND + " = ?" +
                    " AND rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE + " = ?" +
                    " GROUP BY rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + " " +
                    " ORDER BY CAST(rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_SEQ + " AS INTEGER)";

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
                    String stopId = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID));
                    String stopNameEn = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN));
                    String stopNameTc = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC));
                    String stopNameSc = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC));
                    String latitude = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE));
                    String longitude = cursor.getString(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE));
                    
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

                                    double lat = Double.parseDouble(stopInfo.getString("lat"));
                                    double lng = Double.parseDouble(stopInfo.getString("long"));
                                    Log.d("BusRouteDetailView", "Location coordinate for Stop Seq " + i + " : " + lat + " " + lng);
                                    Log.d("BusRouteDetailView", "Attempt to add stop position to map");
                                    LatLng stopPosition = new LatLng(lat, lng);
                                    connectPointsOnMap(stopPositions);
                                    stopPositions.add(stopPosition);

                        } catch (JSONException e) {
                            Log.e("BusRouteDetailView", "Error parsing stop info: " + e.getMessage());
                        }
                    }

                    // Update UI
                    adapter.notifyDataSetChanged();
                    
                    // Draw the route on the map
                    Log.d("BusRouteDetailView", "Drawing route with " + stopPositions.size() + " stops from API data");
                    connectPointsOnMap(stopPositions);

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

    private void connectPointsOnMap(final List<LatLng> stopPositions) {
        final String TAG = "BusRouteDetailView";
        
        if (stopPositions == null || stopPositions.isEmpty()) {
            Log.e(TAG, "No stop positions to connect");
            return;
        }

        // Store positions if map isn't ready yet
        if (mMap == null) {
            Log.d(TAG, "Map not ready, storing positions for later");
            pendingStopPositions = new ArrayList<>(stopPositions);
        }

        runOnUiThread(() -> drawRouteOnMap(stopPositions));
    }

    private void drawRouteOnMap(List<LatLng> stopPositions) {
        final String TAG = "BusRouteDetailView";

        Log.d(TAG, "Drawing route with " + stopPositions.size() + " points");

        if (stopPositions.size() < 2) {
            Log.e(TAG, "At least two points are needed to draw a route.");
            return;
        }

        try {
            Log.d(TAG, "Fetching route for " + stopPositions.size() + " points");

            // Construct the Directions API request URL
            String origin = stopPositions.get(0).latitude + "," + stopPositions.get(0).longitude;
            String destination = stopPositions.get(stopPositions.size() - 1).latitude + "," + stopPositions.get(stopPositions.size() - 1).longitude;

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopPositions.get(initialStopSeqView), 16f));

            // Add a marker for each stop
            for (int i = 0; i < stopPositions.size(); i++) {
                LatLng stop = stopPositions.get(i);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(stop)
                        .title("Stop " + (i + 1)) // You can customize this with actual names
                        .snippet("Lat: " + stop.latitude + ", Lng: " + stop.longitude);
                mMap.addMarker(markerOptions);
            }

            // Waypoints for intermediate stops (optional)
            StringBuilder waypoints = new StringBuilder();
            if (stopPositions.size() > 2) {
                waypoints.append("waypoints=");
                for (int i = 1; i < stopPositions.size() - 1; i++) {
                    waypoints.append(stopPositions.get(i).latitude)
                            .append(",")
                            .append(stopPositions.get(i).longitude);
                    if (i < stopPositions.size() - 2) waypoints.append("|");
                }
                Log.d(TAG, "Waypoints URL: " + waypoints);
            }

            String apiKey = BuildConfig.MAPS_API_KEY; // Replace with your API key
            String requestUrl = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "origin=" + origin
                    + "&destination=" + destination
                    + "&mode=driving"
                    + (waypoints.length() > 0 ? "&" + waypoints.toString() : "")
                    + "&key=" + apiKey;

            // Execute API request in a background thread
            new FetchRouteTask().execute(requestUrl);

        } catch (Exception e) {
            Log.e(TAG, "Error fetching route: " + e.getMessage(), e);
        }
    }


    private class FetchRouteTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                Log.d(TAG, "Fetching route from " + urls[0]);

                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                    Log.d(TAG, "Response: " + line);
                }

                return response.toString();

            } catch (Exception e) {
                Log.e(TAG, "Error fetching route: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
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

                // Draw the route on the map
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