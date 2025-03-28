package com.chung.a9rushtobus;

import android.app.UiModeManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
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
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BusRouteDetailViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BusRouteStopItemAdapter adapter;
    private RecyclerView busRouteStopRecyclerView;
    private List<BusRouteStopItem> busRouteStopItems;
    private String routeNumber, routeDestination, routeBound, routeServiceType, busCompany;
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
        // Determine the correct bound value for later queries ("O" for outbound, "I" for inbound)
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
        busRouteDestination.setSingleLine(true);
        busRouteDestination.setMarqueeRepeatLimit(-1);
        busRouteDestination.setScroller(new Scroller(this));
        busRouteDestination.setMovementMethod(LinkMovementMethod.getInstance());
        busRouteDestination.setSelected(true);

        busRouteNumber.setText(routeNumber);
        busRouteDestination.setText(routeDestination);
        Log.d("LogBusRouteDetailView", "Route: " + routeNumber + " Destination: " + routeDestination +
                " Bound: " + routeBound + " Service Type: " + routeServiceType);

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
        List<LatLng> stopPositions = new ArrayList<>();

        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            String query;
            String[] selectionArgs;
            // Use a temporary variable to adjust the bound value for the query
            String adjustedBound = routeBound.equalsIgnoreCase("outbound") ? "O" : "I";

            if (Objects.equals(busCompany, "kmb")) {
                query = KMBDatabase.Queries.QUERY_GET_STOP_FROM_ROUTE;
                selectionArgs = new String[]{routeNumber, adjustedBound, routeServiceType};
            } else if (Objects.equals(busCompany, "ctb")) {
                query = CTBDatabase.Queries.QUERY_GET_STOP_FROM_ROUTE;
                selectionArgs = new String[]{routeNumber, adjustedBound};
            } else {
                Log.e(TAG, "Unknown bus company: " + busCompany);
                return;
            }

            Log.d(TAG, "Query Args: " + Arrays.toString(selectionArgs));
            Log.d(TAG, "Query: " + query);

            try (Cursor cursor = db.rawQuery(query, selectionArgs)) {
                int index = 0;
                Log.d(TAG, "Found " + cursor.getCount() + " stops for route " + routeNumber);
                while (cursor.moveToNext()) {
                    String stopId;
                    String stopNameEn;
                    String stopNameTc;
                    String stopNameSc;
                    String latitude;
                    String longitude;

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

                    Log.d(TAG, "Processing stop " + index + ": ID=" + stopId + ", Name=" + stopNameEn +
                            ", Lat=" + latitude + ", Long=" + longitude);

                    busRouteStopItems.add(new BusRouteStopItem(
                            routeNumber,
                            adjustedBound,
                            routeServiceType,
                            stopNameEn,
                            stopNameTc,
                            stopNameSc,
                            stopId,
                            busCompany));

                    if (latitude != null && !latitude.isEmpty() && longitude != null && !longitude.isEmpty()) {
                        try {
                            double lat = Double.parseDouble(latitude);
                            double lng = Double.parseDouble(longitude);
                            LatLng stopPosition = new LatLng(lat, lng);
                            stopPositions.add(stopPosition);

                            if (mMap != null) {
                                // Use a different color for the first stop marker
                                float markerHue = (index == 0) ? BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_RED;
                                mMap.addMarker(new MarkerOptions()
                                        .position(stopPosition)
                                        .title(stopNameEn)
                                        .icon(BitmapDescriptorFactory.defaultMarker(markerHue)));
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Invalid coordinates for stop " + stopId + ": " + e.getMessage());
                        }
                    }
                    index++;
                }
            }

            // Notify the adapter of updated data
            adapter.notifyDataSetChanged();

            // Connect stops on map and draw the route
            connectPointsOnMap(stopPositions);

            // Center map on the first stop if available
            if (!stopPositions.isEmpty() && mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stopPositions.get(0), 15f));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error fetching route stops from database", e);
            Toast.makeText(this, "Failed to load bus stops from database: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

            // Optionally, add a marker for each stop (if not already added while loading stops)
            for (int i = 0; i < stopPositions.size(); i++) {
                LatLng stop = stopPositions.get(i);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(stop)
                        .title("Stop " + (i + 1))
                        .snippet("Lat: " + stop.latitude + ", Lng: " + stop.longitude);
                mMap.addMarker(markerOptions);
            }

            // Build the Directions API request URL
            String origin = stopPositions.get(0).latitude + "," + stopPositions.get(0).longitude;
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
                    + "&mode=driving"
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