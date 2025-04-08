package com.chung.a9rushtobus.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;
import com.chung.a9rushtobus.Utils;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.elements.BusRouteStopItem;
import com.chung.a9rushtobus.elements.SavedBusStopAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragmentSaved extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyView;
    private SavedBusStopAdapter adapter;
    private DatabaseHelper databaseHelper;
    private Utils utils;
    private final List<BusRouteStopItem> savedBusStops = new ArrayList<>();
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::loadSavedBusStops;
    private static final int REFRESH_INTERVAL_MS = 60000; // 60 seconds

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = DatabaseHelper.getInstance(requireContext());
        utils = new Utils(getActivity(), getView(), getContext());
        
        // Initialize UserPreferences if not already initialized
        if (UserPreferences.sharedPref == null) {
            new UserPreferences(requireActivity());
        }
        
        // Force onboarding complete for testing if needed
        // UserPreferences.editor.putBoolean(UserPreferences.ONBOARDING_COMPLETE, true);
        // UserPreferences.editor.apply();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_saved, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView_saved_stops);
        emptyView = view.findViewById(R.id.text_no_saved_stops);
        
        setupRecyclerView();
        loadSavedBusStops();
        
        return view;
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SavedBusStopAdapter(requireContext(), savedBusStops, utils);
        recyclerView.setAdapter(adapter);
    }
    
    private void loadSavedBusStops() {
        // Clear current list
        savedBusStops.clear();
        
        // Check if onboarding is completed
        boolean onboardingComplete = UserPreferences.sharedPref.getBoolean(UserPreferences.ONBOARDING_COMPLETE, false);
        Log.d("FragmentSaved", "Onboarding complete: " + onboardingComplete);
        
        // Get saved bus stops from database
        List<BusRouteStopItem> stops = databaseHelper.savedRoutesManager.getSavedRouteStops();
        Log.d("FragmentSaved", "Loaded " + stops.size() + " saved stops");
        
        for (BusRouteStopItem stop : stops) {
            Log.d("FragmentSaved", "Stop: " + stop.getRoute() + " - " + stop.getCompany());
        }
        
        savedBusStops.addAll(stops);
        
        // Sort the saved bus stops
        sortSavedBusStops();
        
        // Update UI
        updateUI();
        
        // Schedule next refresh
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }
    
    /**
     * Sorts the saved bus stops by route number, company, and bound
     */
    private void sortSavedBusStops() {
        Log.d("FragmentSaved", "Sorting saved bus stops...");
        try {
            // Create a comparator for BusRouteStopItem similar to the one for BusRoute
            Collections.sort(savedBusStops, (stop1, stop2) -> {
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
                    // If comparison fails, fall back to string comparison of routes
                    Log.e("FragmentSaved", "Error comparing routes", e);
                    String r1 = stop1.getRoute() != null ? stop1.getRoute() : "";
                    String r2 = stop2.getRoute() != null ? stop2.getRoute() : "";
                    return r1.compareTo(r2);
                }
            });
            
            Log.d("FragmentSaved", "Successfully sorted " + savedBusStops.size() + " saved bus stops");
        } catch (Exception e) {
            Log.e("FragmentSaved", "Error sorting saved bus stops", e);
            // If sorting fails, try a simpler approach
            Collections.sort(savedBusStops, (stop1, stop2) -> {
                String r1 = stop1 != null && stop1.getRoute() != null ? stop1.getRoute() : "";
                String r2 = stop2 != null && stop2.getRoute() != null ? stop2.getRoute() : "";
                return r1.compareTo(r2);
            });
        }
    }
    
    private void updateUI() {
        Log.d("FragmentSaved", "Updating UI with " + savedBusStops.size() + " saved stops");
        
        if (savedBusStops.isEmpty()) {
            Log.d("FragmentSaved", "No saved stops, showing empty view");
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
        } else {
            Log.d("FragmentSaved", "Showing " + savedBusStops.size() + " saved stops");
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
            
            // Make sure adapter is initialized
            if (adapter == null) {
                setupRecyclerView();
            } else {
                adapter.notifyDataSetChanged();
            }
            
            // Start ETA updates in adapter
            if (adapter != null) {
                adapter.startPeriodicUpdates();
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadSavedBusStops();
        if (adapter != null) {
            adapter.resumeETAUpdates();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
        if (adapter != null) {
            adapter.pauseETAUpdates();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(refreshRunnable);
    }
}