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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.chung.a9rushtobus.elements.BusRoute;
import com.google.android.material.button.MaterialButton;
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
import com.chung.a9rushtobus.elements.BusRouteStopItem;
import com.chung.a9rushtobus.elements.NearbyBusRouteAdapter;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragmentNearby extends Fragment implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationClient;
    private TextView locationInfo;
    private GoogleMap mMap;
    private BottomSheetBehavior<ConstraintLayout> bottomSheetBehavior;
    private RecyclerView nearbyStationsRecyclerView;
    private LatLng currentLocation;
    private KMBDatabase kmbDatabase;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private MaterialButton btnLocationPermission;
    private NearbyBusRouteAdapter nearbyBusRouteAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the database
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(requireContext());
        kmbDatabase = databaseHelper.kmbDatabase;
        
        // Initialize location permission launcher
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
                .RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(
                    android.Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean coarseLocationGranted = result.getOrDefault(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION, false);

            if (fineLocationGranted != null && fineLocationGranted) {
                btnLocationPermission.setVisibility(View.GONE);
                getLastLocation();
            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                // Only approximate location access granted
                btnLocationPermission.setVisibility(View.GONE);
                getLastLocation(); // Still try to use coarse location
            } else {
                // No location access granted
                btnLocationPermission.setVisibility(View.VISIBLE);
                locationInfo.setText("Location permission denied");
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);

        // Initialize UI components
        locationInfo = view.findViewById(R.id.locationInfo);
        nearbyStationsRecyclerView = view.findViewById(R.id.nearbyStationsRecyclerView);
        btnLocationPermission = view.findViewById(R.id.btnLocationPermission);

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

        // Setup location permission button
        btnLocationPermission.setOnClickListener(v -> requestPermissions());

        // Check if we already have permission
        if (ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If we already have permission, hide the button
            btnLocationPermission.setVisibility(View.GONE);
            getLastLocation();
        } else {
            // Show the button if we don't have permission
            btnLocationPermission.setVisibility(View.VISIBLE);
            locationInfo.setText("Location access required");
        }

        return view;
    }

    private void sortSavedBusStops(List<BusRouteStopItem> nearbyBusStops) {
        try {
            // Create a comparator for BusRouteStopItem similar to the one for BusRoute
            Collections.sort(nearbyBusStops, (stop1, stop2) -> {
                if (stop1 == null && stop2 == null) {
                    return 0;
                } else if (stop1 == null) {
                    return -1;
                } else if (stop2 == null) {
                    return 1;
                }

                try {
                    // First compare by route number using a similar approach to BusRoute.compareRouteNumber
                    String routeStr1 = stop1.getRoute() != null ? stop1.getRoute().trim().toUpperCase() : "";
                    String routeStr2 = stop2.getRoute() != null ? stop2.getRoute().trim().toUpperCase() : "";

                    // Pattern to match numeric prefix and alphabetic suffix
                    Pattern pattern = Pattern.compile("^(\\d+)([A-Z]*)");
                    Matcher matcher1 = pattern.matcher(routeStr1);
                    Matcher matcher2 = pattern.matcher(routeStr2);

                    boolean hasNumericPrefix1 = matcher1.find();
                    boolean hasNumericPrefix2 = matcher2.find();

                    // Case 1: Both have numeric prefixes (most common case)
                    if (hasNumericPrefix1 && hasNumericPrefix2) {
                        // Compare the numeric parts first
                        int num1 = Integer.parseInt(matcher1.group(1));
                        int num2 = Integer.parseInt(matcher2.group(1));

                        if (num1 != num2) {
                            return Integer.compare(num1, num2);
                        }

                        // If numeric parts are equal, compare the alphabetic suffixes
                        String suffix1 = matcher1.group(2);
                        String suffix2 = matcher2.group(2);

                        // If one has no suffix and the other does, the one without comes first
                        if (suffix1.isEmpty() && !suffix2.isEmpty()) {
                            return -1;
                        } else if (!suffix1.isEmpty() && suffix2.isEmpty()) {
                            return 1;
                        }

                        // Both have suffixes, compare them alphabetically
                        int suffixCompare = suffix1.compareTo(suffix2);
                        if (suffixCompare != 0) {
                            return suffixCompare;
                        }
                    }
                    // Case 2: Only the first has a numeric prefix
                    else if (hasNumericPrefix1) {
                        return -1; // Numeric prefixes come before non-numeric
                    }
                    // Case 3: Only the second has a numeric prefix
                    else if (hasNumericPrefix2) {
                        return 1; // Numeric prefixes come before non-numeric
                    }
                    // Case 4: Neither has a numeric prefix, compare as strings
                    else {
                        int routeCompare = routeStr1.compareTo(routeStr2);
                        if (routeCompare != 0) {
                            return routeCompare;
                        }
                    }

                    // If route numbers are the same, compare by company as a secondary sort
                    String company1 = stop1.getCompany() != null ? stop1.getCompany() : "";
                    String company2 = stop2.getCompany() != null ? stop2.getCompany() : "";

                    int companyCompare = company1.compareTo(company2);
                    if (companyCompare != 0) {
                        return companyCompare;
                    }

                    // If companies are the same, compare by bound/direction
                    String bound1 = stop1.getBound() != null ? stop1.getBound() : "";
                    String bound2 = stop2.getBound() != null ? stop2.getBound() : "";
                    return bound1.compareTo(bound2);

                } catch (Exception e) {
                    String r1 = stop1.getRoute() != null ? stop1.getRoute() : "";
                    String r2 = stop2.getRoute() != null ? stop2.getRoute() : "";
                    return r1.compareTo(r2);
                }
            });

        } catch (Exception e) {
            Collections.sort(nearbyBusStops, (stop1, stop2) -> {
                String r1 = stop1 != null && stop1.getRoute() != null ? stop1.getRoute() : "";
                String r2 = stop2 != null && stop2.getRoute() != null ? stop2.getRoute() : "";
                return r1.compareTo(r2);
            });
        }
    }
    
    private void setupRecyclerView() {
        nearbyStationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        nearbyBusRouteAdapter = new NearbyBusRouteAdapter(requireContext());
        nearbyStationsRecyclerView.setAdapter(nearbyBusRouteAdapter);
        
        // Set click listener for items
        nearbyBusRouteAdapter.setOnItemClickListener(item -> {
            // Handle click on bus route item
            Toast.makeText(requireContext(), "Selected route: " + item.getRoute() + " at " + item.getStopName(), Toast.LENGTH_SHORT).show();
        });
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

            if (ActivityCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        } catch (SecurityException e) {
            Log.e("FragmentNearby", "Security exception: " + e.getMessage());
        }

        if (currentLocation != null) {
            updateMapLocation(currentLocation);
        }
    }


    private void updateMapLocation(LatLng location) {
        if (mMap != null) {
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f));

            // Here you would fetch nearby stations based on the location
            fetchNearbyStations(location);
        }
    }

    private void fetchNearbyStations(LatLng location) {
        // Call getNearbyRoutes to fetch and display nearby bus routes
        getNearbyRoutes(location.latitude, location.longitude);
        
        // Expand bottom sheet slightly to show results are available
        bottomSheetBehavior.setPeekHeight(200);
    }

    private void requestPermissions() {
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

                            // Update map with location
                            updateMapLocation(currentLocation);
                            getNearbyRoutes(latitude, longitude);
                        }
                    });
        }
    }

    private void getNearbyRoutes(double latitude, double longitude) {
        // Set radius to 600 meters as per requirement
        int radiusMeters = 1000;
        
        if (kmbDatabase == null) {
            Log.e("FragmentNearby", "KMBDatabase is null. Initializing now.");
            DatabaseHelper databaseHelper = DatabaseHelper.getInstance(requireContext());
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
            // Create a list of BusRouteStopItem objects for the adapter
            List<BusRouteStopItem> nearbyBusRoutes = new ArrayList<>();
            
            do {
                try {
                    String route = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE));
                    String bound = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND));
                    String serviceType = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE));
                    String origin = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN));
                    String destination = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_EN));
                    String stopNameEn = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN));
                    String stopId = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_ID));
                    double distance = routesCursor.getDouble(routesCursor.getColumnIndexOrThrow("distance"));
                    
                    // Get TC and SC stop names if available
                    String stopNameTc = "";
                    String stopNameSc = "";
                    
                    try {
                        stopNameTc = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC));
                        stopNameSc = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC));
                    } catch (Exception e) {
                        // If TC and SC names are not available, use EN name
                        stopNameTc = stopNameEn;
                        stopNameSc = stopNameEn;
                        Log.w("FragmentNearby", "TC/SC stop names not available, using EN name");
                    }

                    // Calculate actual distance in meters (approximate)
                    double distanceInMeters = Math.sqrt(distance) * 111000; // Rough conversion from degrees to meters
                    
                    // Only include stops that are actually within our target radius
                    if (distanceInMeters <= radiusMeters) {
                        // Log the found route
                        Log.d("NearbyRoutes", "Route: " + route + " from " + origin + " to " + destination +
                                " at stop " + stopNameEn + " (" + distanceInMeters + "m away)");
                        
                        // Create a BusRouteStopItem for this route
                        BusRouteStopItem busRouteStopItem = new BusRouteStopItem(
                                route, bound, serviceType, 
                                stopNameEn, stopNameTc, stopNameSc,
                                stopId, "kmb"); // Using "kmb" as the company
                        
                        // Add to our list for display
                        nearbyBusRoutes.add(busRouteStopItem);
                    }
                    
                } catch (Exception e) {
                    Log.e("FragmentNearby", "Error reading cursor: " + e.getMessage(), e);
                }
            } while (routesCursor.moveToNext());

            routesCursor.close();
            
            // Update UI with the found routes
            if (!nearbyBusRoutes.isEmpty()) {
                // Update the RecyclerView with our results
                sortSavedBusStops(nearbyBusRoutes);

                nearbyBusRouteAdapter.updateRoutes(nearbyBusRoutes);
                
                // Show bottom sheet with results
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                
                // Make sure the RecyclerView is visible
                nearbyStationsRecyclerView.setVisibility(View.VISIBLE);
                btnLocationPermission.setVisibility(View.GONE);
                
                // Start ETA updates
                nearbyBusRouteAdapter.startPeriodicUpdates();
            } else {
                locationInfo.setText(R.string.frag_nearby_noStops_name);
                nearbyBusRouteAdapter.updateRoutes(null); // Clear the adapter
            }
        } else {
            Log.d("FragmentNearby", "No nearby routes found within " + radiusMeters + "m");
            locationInfo.setText(R.string.frag_nearby_noStops_name + " (" + radiusMeters + "m)");
            nearbyBusRouteAdapter.updateRoutes(null); // Clear the adapter
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Resume ETA updates when fragment becomes visible
        if (nearbyBusRouteAdapter != null) {
            nearbyBusRouteAdapter.resumeETAUpdates();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Pause ETA updates when fragment is not visible
        if (nearbyBusRouteAdapter != null) {
            nearbyBusRouteAdapter.pauseETAUpdates();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Ensure ETA updates are stopped when fragment is destroyed
        if (nearbyBusRouteAdapter != null) {
            nearbyBusRouteAdapter.pauseETAUpdates();
        }
    }
    // We're now using BusRouteStopItem instead of a custom NearbyRouteInfo class
}