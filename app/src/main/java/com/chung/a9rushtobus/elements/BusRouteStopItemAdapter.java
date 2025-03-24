package com.chung.a9rushtobus.elements;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.DataFetcher;
import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BusRouteStopItemAdapter extends RecyclerView.Adapter<BusRouteStopItemAdapter.ViewHolder> implements DefaultLifecycleObserver {

    private static final String TAG = "BusRouteStopItemAdapter";
    private static final int UPDATE_INTERVAL_MS = 60000; // 60 seconds
    private final Context context;
    private final List<BusRouteStopItem> items;
    private final DataFetcher dataFetcher;
    private final Utils utils;
    private final Handler updateHandler;
    private final Runnable updateRunnable;
    private boolean isUpdating = false;

    public BusRouteStopItemAdapter(Context context, List<BusRouteStopItem> items, Utils utils) {
        this.context = context;
        this.items = items;
        this.utils = utils;
        this.dataFetcher = new DataFetcher(context);
        this.updateHandler = new Handler(Looper.getMainLooper());
        this.updateRunnable = this::refreshAllEtaData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bus_detail_stop_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusRouteStopItem item = items.get(position);
        holder.stopName.setText(item.getStopTc());
        holder.stopSeq.setText(String.valueOf(position + 1));

        // Set the expanded state based on the data model
        holder.detailLayout.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);

        // Clear previous ETA views
        holder.etaLayout.removeAllViews();
        
        // Get ETA data
        List<String> etaDataFull = item.getEtaDataFull();
        String etaData = item.getClosestETA();

        // Display ETA data
        displayEtaData(holder, etaDataFull, etaData);

        // Set click listener for expanding/collapsing details
        holder.mainLayout.setOnClickListener(v -> {
            item.setExpanded(!item.isExpanded());
            TransitionManager.beginDelayedTransition((ViewGroup) holder.detailLayout.getParent());
            holder.detailLayout.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
        });
    }

    private void displayEtaData(ViewHolder holder, List<String> etaDataFull, String etaData) {
        if (etaDataFull.isEmpty()) {
            // Show placeholders if no data available yet
            addPlaceholderEtas(holder.etaLayout);
        } else {
            // Bind fetched ETA data
            for (String eta : etaDataFull) {
                addEtaTextView(holder.etaLayout, eta);
            }
        }

        // Update the firstETA TextView if available
        if (!etaDataFull.isEmpty() && holder.firstETA != null) {
            holder.firstETA.setText(etaData);
        }
    }

    private void addPlaceholderEtas(LinearLayout etaLayout) {
        for (int i = 0; i < 3; i++) {
            addEtaTextView(etaLayout, "Loading ETA...");
        }
    }

    private void addEtaTextView(LinearLayout etaLayout, String text) {
        TextView etaTextView = new TextView(context);
        etaTextView.setText(text);
        etaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        etaTextView.setPadding(8, 0, 8, 0);
        etaLayout.addView(etaTextView);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        Log.d("ETARefresh", "Starting periodic ETA updates at onResume");
        startPeriodicUpdates();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        Log.d("ETARefresh", "Stopping periodic ETA updates at onPause");
        stopPeriodicUpdates();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        Log.d("ETARefresh", "Stopping periodic ETA updates at onDestroy");
        stopPeriodicUpdates();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Method to start periodic updates
    public void startPeriodicUpdates() {
        if (isUpdating) {
            return;
        }

        if (items.isEmpty()) {
            Log.d("ETARefresh", "No stops to update. Retrying in 3 seconds...");
            updateHandler.postDelayed(this::startPeriodicUpdates, 3000);
            return;
        }
        
        isUpdating = true;
        updateHandler.post(updateRunnable);
        Log.d("ETARefresh", "Started periodic ETA updates");
    }

    // Method to stop periodic updates
    public void stopPeriodicUpdates() {
        if (isUpdating) {
            updateHandler.removeCallbacks(updateRunnable);
            isUpdating = false;
            Log.d("ETARefresh", "Stopped periodic ETA updates");
        }
    }

    // Method to refresh all visible ETA data
    private void refreshAllEtaData() {
        Log.d("ETARefresh", "Refreshing ETA data for " + items.size() + " stops");

        for (int position = 0; position < items.size(); position++) {
            BusRouteStopItem item = items.get(position);
            int finalPosition = position;
            fetchEtaForStop(item, finalPosition, System.currentTimeMillis());
        }

        if (isUpdating) {
            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
        }
    }

    private void fetchEtaForStop(BusRouteStopItem item, int position, long startTime) {
        dataFetcher.fetchStopETA(
            item.getStopID(), 
            item.getRoute(), 
            item.getServiceType(), 
            "kmb",
            etaDataArray -> processEtaData(item, position, etaDataArray, startTime),
            error -> handleEtaError(item, position, error)
        );
    }

    private void processEtaData(BusRouteStopItem item, int position, JSONArray etaDataArray, long startTime) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        List<String> newEtaData = new ArrayList<>();

        try {
            if (etaDataArray.length() == 0) {
                newEtaData.add("No available bus at the moment");
                item.setClosestETA(null);
            } else {
                boolean needsRefetch = false;

                for (int i = 0; i < Math.min(etaDataArray.length(), 3); i++) {
                    JSONObject etaData = etaDataArray.getJSONObject(i);
                    if (!etaData.getString("service_type").equals(item.getServiceType())) {
                        continue;
                    }

                    String etaTime = utils.parseTime(etaData.optString("eta", "N/A"));
                    String etaMinutes = utils.getTimeDifference(etaData.optString("eta", "N/A"));
                    String displayText = etaMinutes.equals("N/A") ?
                            "No available bus" : etaTime + " " + etaMinutes + " mins";

                    if (i == 0 && !etaMinutes.equals("N/A")) {
                        updateClosestEta(item, etaMinutes, position);
                        if (Integer.parseInt(etaMinutes) < 0) {
                            needsRefetch = true;
                            displayText = etaTime + " " + "---";
                        } else if (Integer.parseInt(etaMinutes) == 0) {
                            displayText = etaTime + " " + "Arriving";
                        }
                    }

                    newEtaData.add(displayText);
                }

                // Set the current data and update UI
                item.setEtaData(newEtaData);
                updateUi(position);

                // If negative ETA detected, refetch after a short delay
                if (needsRefetch) {
                    scheduleRefetch(item, position);
                }

                Log.d("ETARefresh", "ETA fetched for position " + position + " in " + elapsedTime + "ms");
                return;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ETA data: " + e.getMessage());
            newEtaData.add("Error: JSON parsing failed");
        }

        item.setEtaData(newEtaData);
        updateUi(position);
    }

    private void updateClosestEta(BusRouteStopItem item, String etaMinutes, int position) {
        if (etaMinutes.equals("N/A")) {
            item.setClosestETA("!");
        } else if (etaMinutes.equals("0")) {
            item.setClosestETA("Arriving");
        } else if (Integer.parseInt(etaMinutes) < 0) {
            item.setClosestETA("---");
            Log.d("ETARefresh", "Position " + (position + 1) + " Negative ETA detected: " + etaMinutes + " mins. Will refetch.");
        } else {
            item.setClosestETA(etaMinutes + " mins");
        }
    }

    private void scheduleRefetch(BusRouteStopItem item, int position) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d("ETARefresh", "Refetching ETA for position " + position + " due to negative value");
            fetchEtaForStop(item, position, System.currentTimeMillis());
        }, 5000); // Wait 5 seconds before re-fetching
    }

    private void handleEtaError(BusRouteStopItem item, int position, String error) {
        Log.e(TAG, "Error fetching ETA for position " + position + ": " + error);

        List<String> errorData = new ArrayList<>();
        errorData.add("Error: " + error);
        item.setEtaData(errorData);

        updateUi(position);
    }

    private void updateUi(int position) {
        new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stopName;
        TextView stopSeq;
        TextView firstETA;
        ConstraintLayout mainLayout, detailLayout;
        LinearLayout etaLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mainLayout = itemView.findViewById(R.id.stopRouteItem_main_layout);
            detailLayout = itemView.findViewById(R.id.stopRouteItem_details_layout);
            stopName = itemView.findViewById(R.id.item_stop_summary_stop_name);
            stopSeq = itemView.findViewById(R.id.item_stop_summary_stop_seq);
            firstETA = itemView.findViewById(R.id.item_stop_summary_stop_first_eta);
            etaLayout = itemView.findViewById(R.id.eta_container);
        }
    }
}