package com.chung.a9rushtobus.elements;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class BusRouteStopItemAdapter extends RecyclerView.Adapter<BusRouteStopItemAdapter.ViewHolder> implements DefaultLifecycleObserver {

    private static final String TAG = "BusRouteStopItemAdapter";
    private static final int UPDATE_INTERVAL_MS = 60000; // 60 seconds
    private Context context;
    private List<BusRouteStopItem> items;
    private DataFetcher dataFetcher;
    private Utils utils;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isUpdating = false;

    public BusRouteStopItemAdapter(Context context, List<BusRouteStopItem> items, Utils utils) {
        this.context = context;
        this.items = items;
        this.utils = utils;
        this.dataFetcher = new DataFetcher();
        this.updateHandler = new Handler(Looper.getMainLooper());
        this.updateRunnable = this::refreshAllEtaData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bus_detail_stop_summary, parent, false);
        return new BusRouteStopItemAdapter.ViewHolder(view);
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
        List<String> etaDataFull = item.getEtaDataFull();
        String etaData = item.getClosestETA();

        if (etaData == null || etaData.isEmpty()) {
            // Show placeholders if no data available yet
            for (int i = 0; i < 3; i++) {
                TextView etaTextView = new TextView(context);
                etaTextView.setText("Loading ETA...");
                etaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                etaTextView.setTextColor(R.color.foreground);
                etaTextView.setPadding(8, 0, 8, 0);
                holder.etaLayout.addView(etaTextView);
            }
        } else {
            // Bind fetched ETA data
            for (String eta : etaDataFull) {
                TextView etaTextView = new TextView(context);
                etaTextView.setText(eta);
                etaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                // You can adjust styling based on your requirements
                etaTextView.setPadding(8, 0, 8, 0);
                holder.etaLayout.addView(etaTextView);
            }
        }

        // Update the firstETA TextView if available
        if (!etaDataFull.isEmpty() && holder.firstETA != null) {
            holder.firstETA.setText(etaData);
        }

        // Modify the click listener to update the data model
        holder.mainLayout.setOnClickListener(v -> {
            // Toggle the state in the data model
            item.setExpanded(!item.isExpanded());
            TransitionManager.beginDelayedTransition((ViewGroup) holder.detailLayout.getParent());
            holder.detailLayout.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        Log.d("ETARefresh", "Activity is in onResume state");
        Log.d("ETARefresh", "Starting periodic ETA updates at onResume");
        startPeriodicUpdates();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        Log.d("ETARefresh", "Activity is in onPause state");
        Log.d("ETARefresh", "Stopping periodic ETA updates at onPause");
        stopPeriodicUpdates();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        Log.d("ETARefresh", "Activity is Destroyed");
        Log.d("ETARefresh", "Stopping periodic ETA updates at onDestroy");
        stopPeriodicUpdates();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Method to start periodic updates
    public void startPeriodicUpdates() {
        Log.d("ETARefresh", "Starting method to start periodic updates");
        Log.d("ETARefresh", "Checking if it is updating: " + isUpdating);

        if (isUpdating) {
            Log.d("ETARefresh", "Already updating, so not starting periodic updates");
            return;
        }

        Log.d("ETARefresh", "Not updating, so starting periodic updates");
        Log.d("ETARefresh", "Number of stops to update: " + items.size());

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
        Log.d("ETARefresh", "In refreshAllEtaData method");
        Log.d("ETARefresh", "Attempting to fetch ETA data for " + items.size() + " stops");

        for (int position = 0; position < items.size(); position++) {
            BusRouteStopItem item = items.get(position);
            int finalPosition = position;
            long startTime = System.currentTimeMillis();

            Log.d("ETARefresh", "Fetching ETA for position " + (finalPosition + 1) + " of " + items.size());

            fetchEtaForStop(item, finalPosition, startTime);
        }

        if (isUpdating) {
            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
        }
    }

    private void fetchEtaForStop(BusRouteStopItem item, int position, long startTime) {
        dataFetcher.fetchStopETA(item.getStopID(), item.getRoute(), item.getServiceType(), "kmb",
                etaDataArray -> {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    List<String> newEtaData = new ArrayList<>();

                    try {
                        if (etaDataArray.length() == 0) {
                            newEtaData.add("No ETA data available");
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

                                if (i == 0) {
                                    if (etaMinutes.equals("N/A")) {
                                        item.setClosestETA("!");
                                    } else if (etaMinutes.equals("0")) {
                                        item.setClosestETA("Arriving");
                                    } else if (Integer.parseInt(etaMinutes) < 0) {
                                        item.setClosestETA("---");
                                        Log.d("ETARefresh", "Position " + (position + 1) + " Negative ETA detected: " + etaMinutes + " mins. Will refetch.");
                                        needsRefetch = true;
                                    } else {
                                        item.setClosestETA(etaMinutes + " mins");
                                    }
                                }

                                newEtaData.add(displayText);
                            }

                            // Set the current data regardless
                            item.setEtaData(newEtaData);

                            // Update the UI with current data
                            new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(position, null));

                            // If negative ETA detected, refetch after a short delay
                            if (needsRefetch) {
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    Log.d("ETARefresh", "Refetching ETA for position " + position + " due to negative value");
                                    fetchEtaForStop(item, position, System.currentTimeMillis());
                                }, 5000); // Wait 5 seconds before refetching
                            }

                            Log.d("ETARefresh", "ETA fetched for position " + position + " in " + elapsedTime + "ms");
                            return;
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing ETA data: " + e.getMessage());
                        newEtaData.add("Error: JSON parsing failed");
                    }

                    item.setEtaData(newEtaData);
                    new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(position, null));
                },
                error -> {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    Log.e(TAG, "Error fetching ETA for position " + position + " in " + elapsedTime + "ms: " + error);

                    List<String> errorData = new ArrayList<>();
                    errorData.add("Error: " + error);
                    item.setEtaData(errorData);

                    new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(position, null));
                }
        );
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

            listenerInit();
        }

        private void listenerInit() {
            mainLayout.setOnClickListener(v -> {
                // Create a transition
                TransitionManager.beginDelayedTransition((ViewGroup) detailLayout.getParent());

                // Toggle visibility with animation
                if (detailLayout.getVisibility() == View.GONE) {
                    detailLayout.setVisibility(View.VISIBLE);
                } else {
                    detailLayout.setVisibility(View.GONE);
                }
            });
        }
    }
}