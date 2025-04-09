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

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.database.KMBDatabase;
import com.chung.a9rushtobus.elements.BusRouteStopItem;
import com.chung.a9rushtobus.elements.NearbyBusRouteAdapter;
import com.chung.a9rushtobus.UserPreferences;
import com.chung.a9rushtobus.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            locationInfo.setVisibility(View.GONE);
            getLastLocation();
        } else {
            // Show the button if we don't have permission
            btnLocationPermission.setVisibility(View.VISIBLE);
            locationInfo.setVisibility(View.VISIBLE);
            locationInfo.setText(R.string.settings_location_required_req_name);
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

                    // If routes are the same, compare by service type (normal routes first)
                    if (routeStr1.equals(routeStr2)) {
                        // For KMB, service type "1" is normal, others are special
                        if (stop1.getCompany().equals("kmb") && stop2.getCompany().equals("kmb")) {
                            String serviceType1 = stop1.getServiceType() != null ? stop1.getServiceType() : "";
                            String serviceType2 = stop2.getServiceType() != null ? stop2.getServiceType() : "";
                            
                            // If one is normal (1) and the other is special, normal comes first
                            if (serviceType1.equals("1") && !serviceType2.equals("1")) {
                                return -1;
                            } else if (!serviceType1.equals("1") && serviceType2.equals("1")) {
                                return 1;
                            }
                            
                            // If both are special or both are normal, compare by service type number
                            int serviceTypeCompare = serviceType1.compareTo(serviceType2);
                            if (serviceTypeCompare != 0) {
                                return serviceTypeCompare;
                            }
                        }
                        
                        // If service types are the same or not KMB, compare by bound/direction
                        String bound1 = stop1.getBound() != null ? stop1.getBound() : "";
                        String bound2 = stop2.getBound() != null ? stop2.getBound() : "";
                        int boundCompare = bound1.compareTo(bound2);
                        if (boundCompare != 0) {
                            return boundCompare;
                        }
                        
                        // If bounds are the same, compare by stop ID to group by stops
                        String stopId1 = stop1.getStopID() != null ? stop1.getStopID() : "";
                        String stopId2 = stop2.getStopID() != null ? stop2.getStopID() : "";
                        return stopId1.compareTo(stopId2);
                    }

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
            Log.e("FragmentNearby", "Error sorting bus stops: " + e.getMessage(), e);
            Collections.sort(nearbyBusStops, (stop1, stop2) -> {
                String r1 = stop1 != null && stop1.getRoute() != null ? stop1.getRoute() : "";
                String r2 = stop2 != null && stop2.getRoute() != null ? stop2.getRoute() : "";
                return r1.compareTo(r2);
            });
        }
    }
    
    private void setupRecyclerView() {
        nearbyStationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        nearbyBusRouteAdapter = new NearbyBusRouteAdapter(requireContext(), new Utils(getActivity(), getView(), getContext()));
        nearbyStationsRecyclerView.setAdapter(nearbyBusRouteAdapter);
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
            // Enable the "My Location" layer if permission is granted
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }

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
            } else {
                LatLng defaultPoint = new LatLng(22.345415, 114.192640);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPoint, 16f));
                // Even for default location, we can show the search radius
                addRadiusCircle(defaultPoint, SEARCH_RADIUS_METERS);
            }
        } catch (SecurityException e) {
            Log.e("FragmentNearby", "Security exception: " + e.getMessage());
        }

        // If we already have a location (from a previous state), update the map
        if (currentLocation != null) {
            updateMapLocation(currentLocation);
        }
    }


    // Define the search radius as a class constant
    private static final int SEARCH_RADIUS_METERS = 1000;
    
    private void updateMapLocation(LatLng location) {
        if (mMap != null) {
            // Clear the map to remove any existing markers and shapes
            mMap.clear();
            
            // Add a circle to show the search radius
            addRadiusCircle(location, SEARCH_RADIUS_METERS);
            
            // Move the camera to the new location with appropriate zoom level
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f));

            // Fetch nearby stations based on the location
            fetchNearbyStations(location);
            
            // Log that we've updated the map location
            Log.d("FragmentNearby", "Map updated to location: " + location.latitude + ", " + location.longitude);
        } else {
            Log.e("FragmentNearby", "Map is null, couldn't update location");
        }
    }
    
    private void addRadiusCircle(LatLng center, int radiusMeters) {
        if (mMap == null) return;
        
        // Create a circle with a border
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(radiusMeters) // in meters
                .strokeWidth(4) // border width - thicker for better visibility
                .strokeColor(getResources().getColor(R.color.colorPrimary, null)) // border color
                .fillColor(getResources().getColor(R.color.colorPrimaryTransparent, null)) // fill color with transparency
                .clickable(false) // make it non-clickable
                .visible(true);   // ensure it's visible
        
        mMap.addCircle(circleOptions);
        
        // Log that we've added the circle
        Log.d("FragmentNearby", "Added search radius circle: " + radiusMeters + "m at " + center.latitude + ", " + center.longitude);
    }

    private void fetchNearbyStations(LatLng location) {
        // Call getNearbyRoutes to fetch and display nearby bus routes
        getNearbyRoutes(location.latitude, location.longitude);
        
        // Expand bottom sheet slightly to show results are available
        bottomSheetBehavior.setPeekHeight(250);
    }
    
    // Add markers to the map for the bus stops
    private void addStopMarkersToMap(List<BusRouteStopItem> busStops) {
        if (mMap == null || busStops == null || busStops.isEmpty()) {
            return;
        }
        
        // Clear existing markers
        mMap.clear();
        
        // Re-add the radius circle after clearing the map
        if (currentLocation != null) {
            addRadiusCircle(currentLocation, SEARCH_RADIUS_METERS);
        }
        
        // Keep track of stops we've already added to avoid duplicates
        Set<String> addedStopIds = new HashSet<>();
        
        for (BusRouteStopItem stop : busStops) {
            // Only add each physical stop once (even if it serves multiple routes)
            if (!addedStopIds.contains(stop.getStopID())) {
                // Get stop coordinates from the database
                String query = "SELECT " +
                        KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE + ", " +
                        KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE + " " +
                        "FROM " + KMBDatabase.Tables.KMB_STOPS.TABLE_NAME + " " +
                        "WHERE " + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_ID + " = ?";
                
                Cursor cursor = kmbDatabase.db.rawQuery(query, new String[]{stop.getStopID()});
                
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        double stopLat = cursor.getDouble(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE));
                        double stopLng = cursor.getDouble(cursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE));
                        
                        // Create a marker for this stop
                        LatLng stopLocation = new LatLng(stopLat, stopLng);
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(stopLocation)
                                .title(stop.getStopName())
                                .snippet("Stop ID: " + stop.getStopID());
                        
                        mMap.addMarker(markerOptions);
                        
                        // Add to our set of added stops
                        addedStopIds.add(stop.getStopID());
                    } catch (Exception e) {
                        Log.e("FragmentNearby", "Error adding marker: " + e.getMessage(), e);
                    }
                }
                
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
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

                            // Update map with location - this will also add the radius circle
                            updateMapLocation(currentLocation);
                            
                            // Get nearby routes
                            getNearbyRoutes(latitude, longitude);
                            
                            // Log that we've updated the location
                            Log.d("FragmentNearby", "Updated location to: " + latitude + ", " + longitude);
                        } else {
                            Log.d("FragmentNearby", "Location is null, couldn't update map");
                        }
                    });
        } else {
            Log.d("FragmentNearby", "No location permission granted");
        }
    }

    private void getNearbyRoutes(double latitude, double longitude) {
        // Use the class constant for radius
        int radiusMeters = SEARCH_RADIUS_METERS;
        
        // Make sure the radius circle is visible on the map
        if (mMap != null) {
            LatLng location = new LatLng(latitude, longitude);
            // We don't need to clear the map here as it will be cleared in addStopMarkersToMap
            // Just ensure the current location is updated
            currentLocation = location;
        }
        
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
        
        // Calculate approximate bounding box for faster filtering
        double latDelta = radiusMeters * 0.000009;
        double lonDelta = radiusMeters * 0.000009;
        
        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;
        
        // Create a list of BusRouteStopItem objects for the adapter
        List<BusRouteStopItem> nearbyBusRoutes = new ArrayList<>();
        
        // Use a more efficient approach with a single query to get all nearby stops with their routes
        String nearbyRoutesQuery = "SELECT " +
                "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_ID + ", " +
                "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN + ", " +
                "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC + ", " +
                "s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC + ", " +
                "r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE + ", " +
                "r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND + ", " +
                "r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + ", " +
                "r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN + ", " +
                "r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_EN + ", " +
                // Simple squared distance for sorting
                "((CAST(s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE + " AS REAL) - ?) * " +
                "(CAST(s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE + " AS REAL) - ?) + " +
                "(CAST(s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE + " AS REAL) - ?) * " +
                "(CAST(s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE + " AS REAL) - ?)) AS distance " +
                "FROM " + KMBDatabase.Tables.KMB_STOPS.TABLE_NAME + " s " +
                "JOIN " + KMBDatabase.Tables.KMB_ROUTE_STOPS.TABLE_NAME + " rs " +
                "ON s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_ID + " = rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_STOP_ID + " " +
                "JOIN " + KMBDatabase.Tables.KMB_ROUTES.TABLE_NAME + " r " +
                "ON rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_ROUTE + " = r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE + " " +
                "AND rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_BOUND + " = r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND + " " +
                "AND rs." + KMBDatabase.Tables.KMB_ROUTE_STOPS.COLUMN_SERVICE_TYPE + " = r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + " " +
                "WHERE CAST(s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_LATITUDE + " AS REAL) BETWEEN ? AND ? " +
                "AND CAST(s." + KMBDatabase.Tables.KMB_STOPS.COLUMN_LONGITUDE + " AS REAL) BETWEEN ? AND ? " +
                "ORDER BY r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE + ", " +
                "r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE + ", " +
                "r." + KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND + ", " +
                "distance";
        
        Cursor routesCursor = kmbDatabase.db.rawQuery(nearbyRoutesQuery, new String[]{
            String.valueOf(latitude),    // For distance calculation 
            String.valueOf(latitude),    // For distance calculation
            String.valueOf(longitude),   // For distance calculation
            String.valueOf(longitude),   // For distance calculation
            String.valueOf(minLat),      // Bounding box min latitude
            String.valueOf(maxLat),      // Bounding box max latitude
            String.valueOf(minLon),      // Bounding box min longitude
            String.valueOf(maxLon)       // Bounding box max longitude
        });
        
        // Use a map to keep track of the closest stop for each unique route
        Map<String, BusRouteStopItem> closestStopForRoute = new HashMap<>();
        
        if (routesCursor != null && routesCursor.moveToFirst()) {
            do {
                try {
                    String route = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE));
                    String bound = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND));
                    String serviceType = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE));
                    String stopId = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_ID));
                    String stopNameEn = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_EN));
                    String stopNameTc = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_TC));
                    String stopNameSc = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_STOPS.COLUMN_STOP_NAME_SC));
                    String origin = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN));
                    String destination = routesCursor.getString(routesCursor.getColumnIndexOrThrow(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_EN));
                    double distance = routesCursor.getDouble(routesCursor.getColumnIndexOrThrow("distance"));
                    
                    // Calculate actual distance in meters (approximate)
                    double distanceInMeters = Math.sqrt(distance) * 111000; // Rough conversion from degrees to meters
                    
                    // Only include stops that are actually within our target radius
                    if (distanceInMeters <= radiusMeters) {
                        // Create a unique key for this route (route number + bound + service type)
                        String routeKey = route + "_" + bound + "_" + serviceType;
                        
                        // Check if we already have this route in our map
                        if (!closestStopForRoute.containsKey(routeKey)) {
                            // This is the first (and therefore closest) stop we've found for this route
                            BusRouteStopItem busRouteStopItem = new BusRouteStopItem(
                                    route, bound, serviceType, 
                                    stopNameEn, stopNameTc, stopNameSc,
                                    stopId, "kmb"); // Using "kmb" as the company
                            
                            // Add to our map
                            closestStopForRoute.put(routeKey, busRouteStopItem);
                            
                            // Log the found route
                            Log.d("NearbyRoutes", "Route: " + route + " (Service Type: " + serviceType + ") from " + origin + 
                                    " to " + destination + " at stop " + stopNameEn + " (" + distanceInMeters + "m away)");
                        }
                        // We don't need an else case because the query is ordered by distance,
                        // so the first occurrence of each route is already the closest
                    }
                } catch (Exception e) {
                    Log.e("FragmentNearby", "Error processing route: " + e.getMessage(), e);
                }
            } while (routesCursor.moveToNext());
            
            routesCursor.close();
        }
        
        // Convert the map values to a list
        nearbyBusRoutes.addAll(closestStopForRoute.values());
        
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
            locationInfo.setVisibility(View.GONE);
            
            // Start ETA updates
            nearbyBusRouteAdapter.startPeriodicUpdates();
            
            // Add markers to the map for the stops
            addStopMarkersToMap(nearbyBusRoutes);
            
            // Log the total number of routes found
            Log.d("FragmentNearby", "Found " + nearbyBusRoutes.size() + " routes at nearby stops within " + radiusMeters + "m");
        } else {
            Log.d("FragmentNearby", "No routes found at nearby stops within " + radiusMeters + "m");
            locationInfo.setVisibility(View.VISIBLE);
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
