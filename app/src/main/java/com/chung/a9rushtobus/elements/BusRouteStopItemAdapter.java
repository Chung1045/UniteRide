package com.chung.a9rushtobus.elements;

import android.content.Context;
import android.graphics.Color;
import android.transition.TransitionManager;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.DataFetcher;
import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import android.util.Log;

//TODO: Implement feature to update the eta info when onResume() / every 60 seconds

public class BusRouteStopItemAdapter extends RecyclerView.Adapter<BusRouteStopItemAdapter.ViewHolder> {

    private Context context;
    private List<BusRouteStopItem> items;
    private DataFetcher dataFetcher;
    private Utils utils;

    public BusRouteStopItemAdapter(Context context, List<BusRouteStopItem> items, Utils utils) {
        this.context = context;
        this.items = items;
        this.utils = utils;
        this.dataFetcher = new DataFetcher();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bus_detail_stop_summary, parent, false);
        return new BusRouteStopItemAdapter.ViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusRouteStopItem item = items.get(position);
        holder.stopName.setText(item.getStopTc());
        holder.stopSeq.setText(String.valueOf(position + 1));

        // Clear previous views
        holder.etaLayout.removeAllViews();
        
        // Add placeholder ETA views (will be updated when data arrives)
        for (int i = 0; i < 3; i++) {
            TextView etaTextView = new TextView(context);
            etaTextView.setText("Loading ETA...");
            etaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            etaTextView.setTextColor(Color.GRAY);
            etaTextView.setPadding(8, 0, 8, 0);
            holder.etaLayout.addView(etaTextView);
        }

        // Fetch ETA data using the callback-based method
        fetchAndDisplayETA(item, holder);
    }
    
    private void fetchAndDisplayETA(BusRouteStopItem item, ViewHolder holder) {
        String stopID = item.getStopID();
        String route = item.getRoute();
        String serviceType = item.getServiceType();
        
        Log.d("BusAdapter", "Fetching ETA for stop: " + stopID + ", route: " + route + ", serviceType: " + serviceType);
        
        dataFetcher.fetchStopETA(stopID, route, serviceType, "kmb",
            // Success callback
            etaDataArray -> {
                try {
                    // Clear existing views if we're going to replace them
                    if (etaDataArray.length() > 0) {
                        holder.etaLayout.removeAllViews();
                    }
                    
                    // If no ETAs available, show a message
                    if (etaDataArray.length() == 0) {
                        TextView noEtaTextView = new TextView(context);
                        noEtaTextView.setText("No ETA data available");
                        noEtaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        noEtaTextView.setTextColor(Color.GRAY);
                        noEtaTextView.setPadding(8, 0, 8, 0);
                        holder.etaLayout.addView(noEtaTextView);
                        return;
                    }
                    
                    // Add ETA data to the layout
                    for (int i = 0; i < Math.min(etaDataArray.length(), 3); i++) {
                        JSONObject etaData = etaDataArray.getJSONObject(i);
                        if (etaData.getString("service_type").equals(serviceType)) {
                            Log.d("BusAdapter", "Found ETA data for service type: " + serviceType);
                        } else {
                            Log.d("BusAdapter", "Skipping ETA data for service type: " + etaData.getJSONObject("service_type").getString("name"));
                            continue;
                        }
                        TextView etaTextView = new TextView(context);
                        String etaTime = utils.parseTime(etaData.optString("eta", "N/A"));
                        String etaMinutes = utils.getTimeDifference(etaData.optString("eta", "N/A"));
                        
                        String displayText = etaMinutes.equals("N/A") ?
                              "No available bus at the moment" : etaTime + " " + etaMinutes + " mins";
                            
                        etaTextView.setText(displayText);
                        etaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        etaTextView.setTextColor(Color.BLACK);
                        etaTextView.setPadding(8, 4, 8, 4);
                        holder.etaLayout.addView(etaTextView);
                        
                        Log.d("BusAdapter", "Added ETA: " + displayText);
                        Log.d("BusAdapter", "Parsed ETA data");
                        Log.d("BusAdapter", "ETA: " + etaMinutes);
                    }
                } catch (JSONException e) {
                    Log.e("BusAdapter", "Error parsing ETA data: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Show error message
                    TextView errorTextView = new TextView(context);
                    errorTextView.setText("Error loading ETA data");
                    errorTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    errorTextView.setTextColor(Color.RED);
                    errorTextView.setPadding(8, 0, 8, 0);
                    holder.etaLayout.removeAllViews();
                    holder.etaLayout.addView(errorTextView);
                }
            },
            // Error callback
            error -> {
                Log.e("BusAdapter", "Error fetching ETA: " + error);
                
                // Show error message
                TextView errorTextView = new TextView(context);
                errorTextView.setText("Error: " + error);
                errorTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                errorTextView.setTextColor(Color.RED);
                errorTextView.setPadding(8, 0, 8, 0);
                holder.etaLayout.removeAllViews();
                holder.etaLayout.addView(errorTextView);
            }
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stopName;
        TextView stopSeq;
        TextView stopEta;
        ConstraintLayout mainLayout, detailLayout;
        LinearLayout etaLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mainLayout = itemView.findViewById(R.id.stopRouteItem_main_layout);
            detailLayout = itemView.findViewById(R.id.stopRouteItem_details_layout);
            stopName = itemView.findViewById(R.id.item_stop_summary_stop_name);
            stopSeq = itemView.findViewById(R.id.item_stop_summary_stop_seq);
            stopEta = itemView.findViewById(R.id.item_stop_summary_stop_eta);
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
