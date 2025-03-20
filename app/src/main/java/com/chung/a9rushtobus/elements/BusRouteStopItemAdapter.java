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
        List<String> etaData = item.getEtaData();

        if (etaData == null || etaData.isEmpty()) {
            // Show placeholders if no data available yet
            for (int i = 0; i < 3; i++) {
                TextView etaTextView = new TextView(context);
                etaTextView.setText("Loading ETA...");
                etaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                etaTextView.setTextColor(Color.GRAY);
                etaTextView.setPadding(8, 0, 8, 0);
                holder.etaLayout.addView(etaTextView);
            }
        } else {
            // Bind fetched ETA data
            for (String eta : etaData) {
                TextView etaTextView = new TextView(context);
                etaTextView.setText(eta);
                etaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                // You can adjust styling based on your requirements
                etaTextView.setPadding(8, 0, 8, 0);
                holder.etaLayout.addView(etaTextView);
            }
        }

        // Update the firstETA TextView if available
        if (!etaData.isEmpty() && holder.firstETA != null) {
            holder.firstETA.setText(etaData.get(0));
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
        startPeriodicUpdates();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        Log.d("ETARefresh", "Activity is in onPause state");
        stopPeriodicUpdates();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        Log.d("ETARefresh", "Activity is Destroyed");
        stopPeriodicUpdates();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Method to start periodic updates
    public void startPeriodicUpdates() {
        if (!isUpdating) {
            isUpdating = true;
            updateHandler.post(updateRunnable);
            Log.d("ETARefresh", "Started periodic ETA updates");
        }
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
        Log.d("ETARefresh", "Refreshing all ETA data");

        for (int position = 0; position < items.size(); position++) {
            BusRouteStopItem item = items.get(position);
            int finalPosition = position;
            long startTime = System.currentTimeMillis();

            Log.d("ETARefresh", "Fetching ETA for position " + finalPosition);

            dataFetcher.fetchStopETA(item.getStopID(), item.getRoute(), item.getServiceType(), "kmb",
                    etaDataArray -> {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        List<String> newEtaData = new ArrayList<>();

                        try {
                            if (etaDataArray.length() == 0) {
                                newEtaData.add("No ETA data available");
                            } else {
                                for (int i = 0; i < Math.min(etaDataArray.length(), 3); i++) {
                                    JSONObject etaData = etaDataArray.getJSONObject(i);
                                    if (!etaData.getString("service_type").equals(item.getServiceType())) {
                                        continue;
                                    }
                                    String etaTime = utils.parseTime(etaData.optString("eta", "N/A"));
                                    String etaMinutes = utils.getTimeDifference(etaData.optString("eta", "N/A"));
                                    String displayText = etaMinutes.equals("N/A") ?
                                            "---" : etaTime + " " + etaMinutes + " mins";
                                    newEtaData.add(displayText);
                                }
                            }

                            item.setEtaData(newEtaData);

                            Log.d("ETARefresh", "ETA fetched for position " + finalPosition + " in " + elapsedTime + "ms");

                            // Force RecyclerView to update immediately
                            new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(finalPosition, null));

                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing ETA data: " + e.getMessage());
                        }
                    },
                    error -> {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        Log.e(TAG, "Error fetching ETA for position " + finalPosition + " in " + elapsedTime + "ms: " + error);

                        List<String> errorData = new ArrayList<>();
                        errorData.add("Error: " + error);
                        item.setEtaData(errorData);

                        new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(finalPosition, null));
                    }
            );
        }

        if (isUpdating) {
            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
        }
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