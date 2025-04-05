package com.chung.a9rushtobus.fragments;

import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.database.KMBDatabase;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.List;

public class FragmentNearby extends Fragment implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationClient;
    private TextView locationInfo;
    private GoogleMap mMap;
    private BottomSheetBehavior<ConstraintLayout> bottomSheetBehavior;
    private RecyclerView nearbyStationsRecyclerView;
    private LatLng currentLocation;
    private KMBDatabase kmbDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the database
        DatabaseHelper databaseHelper = new DatabaseHelper(requireContext());
        kmbDatabase = databaseHelper.kmbDatabase;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);

        // Initialize UI components
        locationInfo = view.findViewById(R.id.locationInfo);
        nearbyStationsRecyclerView = view.findViewById(R.id.nearbyStationsRecyclerView);

        // Setup RecyclerView
        setupRecyclerView();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Setup bottom sheet
        setupBottomSheet(view);

        // Request location permissions
        requestPermissions();

        return view;
    }

    private void setupRecyclerView() {
        nearbyStationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Add adapter setup here once you have your data model and adapter ready
        // nearbyStationsRecyclerView.setAdapter(new NearbyStationsAdapter(stationsList));
    }

    private void setupBottomSheet(View view) {
        ConstraintLayout bottomSheet = view.findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        // Handle expanded state
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        // Handle collapsed state
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Handle sliding (optional)
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        try {
            mMap.setMyLocationEnabled(true);

            UiSettings uiSettings = mMap.getUiSettings();

            // Hide zoom controls
            uiSettings.setZoomControlsEnabled(false);

            // Hide compass
            uiSettings.setCompassEnabled(false);

            // Hide map toolbar (appears when you click a marker)
            uiSettings.setMapToolbarEnabled(false);

            // Hide my location button (if you're using location layer)
            uiSettings.setMyLocationButtonEnabled(false);

            // Handle dark mode styling
            UiModeManager uiModeManager = (UiModeManager) requireContext().getSystemService(Context.UI_MODE_SERVICE);
            boolean darkTheme = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_DARK, false);
            boolean followSystem = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_FOLLOW_SYSTEM, false);
            if (darkTheme || (followSystem && uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES)) {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.maps_night_theme));
            }

            getLastLocation();

        } catch (SecurityException e) {
            requestPermissions();
        }

        if (currentLocation != null) {
            updateMapLocation(currentLocation);
        }
    }


    private void updateMapLocation(LatLng location) {
        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(location).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));

            // Here you would fetch nearby stations based on the location
            fetchNearbyStations(location);
        }
    }

    private void fetchNearbyStations(LatLng location) {
        // TODO: Implement API call or database query to get nearby stations
        // This is a placeholder - you would replace this with actual data fetching logic

        // After fetching, update the RecyclerView adapter
        // stationsAdapter.updateStations(stationsList);

        // Expand bottom sheet slightly to show results are available
        bottomSheetBehavior.setPeekHeight(200);
    }

    private void requestPermissions() {
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {

                    Boolean fineLocationGranted = result.getOrDefault(
                            android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        getLastLocation();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // Only approximate location access granted
                        getLastLocation(); // Still try to use coarse location
                    } else {
                        // No location access granted
                        locationInfo.setText("Location permission denied");
                    }
                });

        locationPermissionRequest.launch(new String[] {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(),
                    location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            currentLocation = new LatLng(latitude, longitude);

                            locationInfo.setText("Your location: " + latitude + ", " + longitude);

                            // Update map with location
                            updateMapLocation(currentLocation);
                            getNearbyRoutes(latitude, longitude);
                        } else {
                            locationInfo.setText("Location not available");
                        }
                    });
        }
    }

    private void getNearbyRoutes(double latitude, double longitude) {
        // For example, in Hong Kong - increased radius for testing
        int radiusMeters = 1000; // 1000m radius for better chance of finding nearby stops
        
        if (kmbDatabase == null) {
            Log.e("FragmentNearby", "KMBDatabase is null. Initializing now.");
            DatabaseHelper databaseHelper = new DatabaseHelper(requireContext());
            kmbDatabase = databaseHelper.kmbDatabase;
        }
        
        // Check if database tables are actually populated
        Cursor countCheck = kmbDatabase.db.rawQuery("SELECT COUNT(*) FROM " + KMBDatabase.Tables.KMB_STOPS.TABLE_NAME, null);
        int stopCount = 0;
        if (countCheck != null && countCheck.moveToFirst()) {
            stopCount = countCheck.getInt(0);
            countCheck.close();
        }
        
        Log.d("FragmentNearby", "Database has " + stopCount + " stops in total");
        
        if (stopCount == 0) {
            Log.e("FragmentNearby", "No stops in database! Make sure data is imported properly.");
            locationInfo.setText("No bus stops in database. Please check data import.");
            return;
        }
        
        Cursor routesCursor = kmbDatabase.queryNearbyRoutes(latitude, longitude, radiusMeters);

        Log.d("FragmentNearby", "Querying nearby routes with latitude: " + latitude + ", longitude: " + longitude);
        Log.d("FragmentNearby", "Cursor count: " + (routesCursor != null ? routesCursor.getCount() : "null"));

        if (routesCursor != null && routesCursor.moveToFirst()) {
            // Create a list of nearby routes that we can display
            List<NearbyRouteInfo> nearbyRoutes = new ArrayList<>();
            
            do {
                try {
                    String route = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE));
                    String origin = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN));
                    String destination = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_EN));
                    String stopName = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN));
                    double distance = routesCursor.getDouble(routesCursor.getColumnIndexOrThrow("distance"));

                    // Use the data as needed
                    Log.d("NearbyRoutes", "Route: " + route + " from " + origin + " to " + destination +
                            " at stop " + stopName + " (" + distance + "m away)");
                            
                    // Add to our list for display
                    nearbyRoutes.add(new NearbyRouteInfo(route, origin, destination, stopName, distance));
                } catch (Exception e) {
                    Log.e("FragmentNearby", "Error reading cursor: " + e.getMessage(), e);
                }
            } while (routesCursor.moveToNext());

            routesCursor.close();
            
            // Update UI with the found routes
            if (!nearbyRoutes.isEmpty()) {
                // Update the RecyclerView with our results
                
                // Show bottom sheet with results
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                locationInfo.setText("Found " + nearbyRoutes.size() + " routes nearby");
            } else {
                locationInfo.setText("No routes found nearby");
            }
        } else {
            Log.d("FragmentNearby", "No nearby routes found within " + radiusMeters + "m");
            locationInfo.setText("No routes found within " + radiusMeters + "m");
        }
    }
    
    // Data class to hold nearby route information
    public static class NearbyRouteInfo {
        public final String route;
        public final String origin;
        public final String destination;
        public final String stopName;
        public final double distance;
        
        public NearbyRouteInfo(String route, String origin, String destination, String stopName, double distance) {
            this.route = route;
            this.origin = origin;
            this.destination = destination;
            this.stopName = stopName;
            this.distance = distance;
        }
    }
}