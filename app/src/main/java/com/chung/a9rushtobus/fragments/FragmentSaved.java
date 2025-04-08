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
import java.util.List;

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
        
        // Update UI
        updateUI();
        
        // Schedule next refresh
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
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