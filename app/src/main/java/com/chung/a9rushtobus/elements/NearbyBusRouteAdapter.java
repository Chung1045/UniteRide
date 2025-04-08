package com.chung.a9rushtobus.elements;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.BusRouteDetailViewActivity;
import com.chung.a9rushtobus.BuildConfig;
import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;
import com.chung.a9rushtobus.Utils;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.service.DataFetcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for displaying nearby bus routes in a RecyclerView.
 */
public class NearbyBusRouteAdapter extends RecyclerView.Adapter<NearbyBusRouteAdapter.ViewHolder> {
    private static final String TAG = "NearbyBusRouteAdapter";
    private static final int UPDATE_INTERVAL_MS = 30000; // 30 seconds

    private final Context context;
    private List<BusRouteStopItem> busRouteStopItems;
    private OnItemClickListener onItemClickListener;
    private final DataFetcher dataFetcher;
    private final Utils utils;
    private final DatabaseHelper databaseHelper;
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = this::refreshAllEtaData;
    private boolean isUpdating = false;

    /**
     * Interface for handling item click events
     */
    public interface OnItemClickListener {
        void onItemClick(BusRouteStopItem item);
    }

    /**
     * Constructor for the adapter
     * @param context The context
     */
    public NearbyBusRouteAdapter(Context context) {
        this.context = context;
        this.busRouteStopItems = new ArrayList<>();
        this.dataFetcher = new DataFetcher(context);
        this.utils = null; // We'll implement our own time parsing methods
        this.databaseHelper = DatabaseHelper.getInstance(context);
    }
    
    /**
     * Parse ISO datetime string to get time in HH:mm format
     * @param isoDateTime ISO datetime string
     * @return Formatted time string or "N/A" if parsing fails
     */
    private String parseTime(String isoDateTime) {
        try {
            // Parse the ISO date-time string
            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(isoDateTime);
            // Convert to LocalTime in system default timezone
            java.time.LocalTime time = zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalTime();
            // Format time without seconds
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            return time.format(formatter);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing time: " + e.getMessage());
            return "N/A";
        }
    }
    
    /**
     * Calculate time difference in minutes between now and given ISO datetime
     * @param isoDateTime ISO datetime string
     * @return Time difference in minutes as string, or "N/A" if parsing fails
     */
    private String getTimeDifference(String isoDateTime) {
        try {
            // Use ZonedDateTime to parse ISO date with timezone information
            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(isoDateTime);
            // Convert to LocalDateTime in system default timezone for comparison
            java.time.LocalDateTime dateTime = zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
            java.time.Duration duration = java.time.Duration.between(java.time.LocalDateTime.now(), dateTime);
            return String.valueOf(duration.toMinutes());
        } catch (Exception e) {
            Log.e(TAG, "Error calculating time difference: " + e.getMessage());
            return "N/A";
        }
    }

    /**
     * Updates the list of bus routes
     * @param newRoutes The new list of bus routes
     */
    public void updateRoutes(List<BusRouteStopItem> newRoutes) {
        if (newRoutes == null) {
            this.busRouteStopItems = new ArrayList<>();
        } else {
            this.busRouteStopItems = newRoutes;
        }
        notifyDataSetChanged();
        
        // Start ETA updates if we have routes
        if (!busRouteStopItems.isEmpty()) {
            startPeriodicUpdates();
        }
    }

    /**
     * Sets the item click listener
     * @param listener The listener to set
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby_bus_route, parent, false);
        // Set layout_height to wrap_content for the root layout to fix display issues
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        view.setLayoutParams(params);
        return new ViewHolder(view);
    }

    private Context getLocalizedContext() {
        String appLang = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
        Locale locale;
        switch (appLang) {
            case "zh-rCN":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "zh-rHK":
                locale = Locale.TRADITIONAL_CHINESE;
                break;
            default:
                locale = Locale.ENGLISH;
        }

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusRouteStopItem item = busRouteStopItems.get(position);
        String destination = null;
        
        // Set route number
        String routeNumber = item.getRoute();
        holder.tvRouteName.setText(routeNumber);
        
        // Set company name based on the company code
        String company = item.getCompany();
        switch (company) {
            case "kmb":
                holder.tvRouteBusCompany.setText(R.string.bus_company_kmb_name);
                holder.tvRouteRemarks.setVisibility(View.GONE);
                
                // Set special indicator for non-standard KMB routes
                if (!Objects.equals(item.getServiceType(), "1")) {
                    holder.routeSpecialIndicator.setVisibility(View.VISIBLE);
                    holder.routeSpecialIndicator.setText(R.string.frag_search_specialRoute_name);
                } else {
                    holder.routeSpecialIndicator.setVisibility(View.GONE);
                }
                break;
                
            case "ctb":
                holder.tvRouteBusCompany.setText(R.string.bus_company_ctb_name);
                holder.tvRouteRemarks.setVisibility(View.GONE);
                holder.routeSpecialIndicator.setVisibility(View.GONE);
                break;
                
            case "gmb":
                holder.tvRouteBusCompany.setText(R.string.bus_company_gmb_name);
                holder.tvRouteRemarks.setVisibility(View.GONE);
                holder.routeSpecialIndicator.setVisibility(View.GONE);
                break;
                
            default:
                holder.tvRouteBusCompany.setText(company.toUpperCase());
                holder.tvRouteRemarks.setVisibility(View.GONE);
                holder.routeSpecialIndicator.setVisibility(View.GONE);
                break;
        }
        
        // Set stop name
        holder.tvStopName.setText(item.getStopName());
        holder.tvStopName.setSelected(true); // Enable marquee effect
        
        // Get and set destination based on company
        try {
            switch (item.getCompany()) {
                case "kmb":
                    if (item.getRoute() != null && item.getBound() != null && item.getServiceType() != null) {
                        destination = databaseHelper.kmbDatabase.getRouteDestination(
                            item.getRoute(), 
                            item.getBound(), 
                            item.getServiceType()
                        );
                    }
                    break;
                case "ctb":
                    if (item.getRoute() != null) {
                        destination = databaseHelper.ctbDatabase.getRouteDestination(
                                item.getRoute(),
                                item.getBound()
                        );
                    }
                    break;
                case "gmb":
                    if (item.getGmbRouteID() != null && item.getGmbRouteSeq() != null) {
                        destination = databaseHelper.gmbDatabase.getRouteDestination(
                            item.getGmbRouteID(),
                            item.getGmbRouteSeq()
                        );
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting destination for route " + item.getRoute() + ": " + e.getMessage());
        }
        
        if (destination != null && !destination.trim().isEmpty()) {
            String appLang = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
            switch (appLang){
                case "zh-rCN":
                case "zh-rHK":
                    holder.tvDestination.setText("往 " + destination.trim().toUpperCase());
                    break;
                default:
                    holder.tvDestination.setText("TO " + destination.trim().toUpperCase());
            }
        } else {
            holder.tvDestination.setText("");
        }
        holder.tvDestination.setSelected(true); // Enable marquee effect
        
        // Handle ETAs
        if (item.getClosestETA() != null) {
            holder.tvEta1.setText(item.getClosestETA());
            holder.tvEta1.setVisibility(View.VISIBLE);
        } else {
            holder.tvEta1.setText("---");
            holder.tvEta1.setVisibility(View.VISIBLE);
        }

        // Handle additional ETAs if available
        List<String> etaData = item.getEtaDataFull();
        if (etaData != null && etaData.size() > 0) {
            // First ETA already handled above with getClosestETA()

            // Handle second ETA
            if (etaData.size() > 1) {
                holder.tvEta2.setText(etaData.get(1));
                holder.tvEta2.setVisibility(View.VISIBLE);
            } else {
                holder.tvEta2.setVisibility(View.GONE);
            }

            // Handle third ETA
            if (etaData.size() > 2) {
                holder.tvEta3.setText(etaData.get(2));
                holder.tvEta3.setVisibility(View.VISIBLE);
            } else {
                holder.tvEta3.setVisibility(View.GONE);
            }
        } else {
            // Hide additional ETAs if there's no data
            holder.tvEta2.setVisibility(View.GONE);
            holder.tvEta3.setVisibility(View.GONE);
        }
        
        // Set route number styling based on route type
        setTextColorAndBackground(holder.tvRouteName, routeNumber);
        
        // Set click listener for the entire item
        String finalDestination = destination;
        holder.busRouteItemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
            
            // Open detail view
            Intent intent = new Intent(context, BusRouteDetailViewActivity.class);
            intent.putExtra("route", item.getRoute());
            intent.putExtra("destination", finalDestination);
            intent.putExtra("company", item.getCompany());
            intent.putExtra("bound", item.getBound());
            intent.putExtra("serviceType", item.getServiceType());
            if (item.getCompany().equals("gmb")) {
                intent.putExtra("gmbRouteID", item.getGmbRouteID());
                intent.putExtra("gmbRouteSeq", item.getGmbRouteSeq());
            }
            context.startActivity(intent);
        });
        
        // Set route ID for reference
        holder.tvRouteId.setText(item.getRoute() + "_" + item.getStopID());
        
        // Hide remarks by default
        holder.tvRouteRemarks.setVisibility(View.GONE);
        
        // Log for debugging in debug builds only
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Binding route: " + routeNumber + " at stop: " + item.getStopName());
        }
    }

    /**
     * Sets the text color and background for route number based on route type
     * @param textView The TextView to style
     * @param routeNumber The route number
     */
    private void setTextColorAndBackground(TextView textView, String routeNumber) {
        if (routeNumber.startsWith("A") || routeNumber.startsWith("E")) {
            // Airport/External routes
            textView.setBackgroundColor(context.getColor(R.color.externalBusBackground));
            textView.setTextColor(context.getColor(R.color.externalBusForeground));
        } else if (routeNumber.startsWith("N")) {
            // Night routes
            textView.setBackgroundColor(Color.GRAY);
            textView.setTextColor(Color.WHITE);
        } else if (routeNumber.matches("^[169]\\d{2}[A-Za-z]?$")) {
            // Cross-harbour routes
            textView.setBackgroundColor(context.getColor(R.color.crossHarbourBusBackground));
            textView.setTextColor(context.getColor(R.color.crossHarbourBusForeground));
        } else {
            // Regular routes
            textView.setBackgroundColor(Color.TRANSPARENT);
            textView.setTextColor(context.getColor(R.color.foreground));
        }
    }

    @Override
    public int getItemCount() {
        return busRouteStopItems.size();
    }
    
    // Periodic ETA updates methods
    public void startPeriodicUpdates() {
        if (isUpdating) {
            return;
        }

        if (busRouteStopItems.isEmpty()) {
            Log.d("ETARefresh", "No nearby stops to update.");
            return;
        }

        isUpdating = true;
        updateHandler.post(updateRunnable);
        Log.d("ETARefresh", "Started periodic ETA updates for nearby stops");
    }

    public void pauseETAUpdates() {
        if (isUpdating) {
            updateHandler.removeCallbacks(updateRunnable);
            isUpdating = false;
            Log.d("ETARefresh", "Stopped periodic ETA updates for nearby stops");
        }
    }

    public void resumeETAUpdates() {
        startPeriodicUpdates();
    }

    private void refreshAllEtaData() {
        Log.d("ETARefresh", "Refreshing ETA data for " + busRouteStopItems.size() + " nearby stops");

        for (int position = 0; position < busRouteStopItems.size(); position++) {
            BusRouteStopItem item = busRouteStopItems.get(position);
            int finalPosition = position;
            fetchEtaForStop(item, finalPosition);
        }

        if (isUpdating) {
            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
        }
    }

    private void fetchEtaForStop(BusRouteStopItem item, int position) {
        if (item.getCompany().equals("kmb") || item.getCompany().equals("ctb")) {
            dataFetcher.fetchStopETA(
                    item.getStopID(),
                    item.getRoute(),
                    item.getServiceType(),
                    item.getCompany(),
                    etaDataArray -> processEtaData(item, position, etaDataArray),
                    error -> Log.e(TAG, "Error fetching ETA: " + error)
            );
        } else if (item.getCompany().equals("gmb")) {
            dataFetcher.fetchGMBStopETA(
                    item.getGmbRouteID(),
                    item.getGmbRouteSeq(),
                    String.valueOf(position + 1),
                    etaDataArray -> processEtaData(item, position, etaDataArray),
                    error -> Log.e(TAG, "Error fetching GMB ETA: " + error)
            );
        }
    }

    private void processEtaData(BusRouteStopItem item, int position, JSONArray etaDataArray) {
        List<String> newEtaData = new ArrayList<>();
        Context localizedContext = getLocalizedContext();

        try {
            if (etaDataArray.length() == 0) {
                Log.d("ETARefresh", "No ETA data received for nearby stop " + position);
                item.setClosestETA("---");
            } else {
                for (int i = 0; i < Math.min(etaDataArray.length(), 3); i++) {
                    JSONObject etaData = etaDataArray.getJSONObject(i);
                    String etaTime = "N/A", etaMinutes = "N/A";

                    try {
                        if (item.getCompany().equals("ctb") || item.getCompany().equals("kmb")) {
                            // Safely use the TimeUtils methods
                            etaTime = utils.parseTime(etaData.optString("eta", "N/A"));
                            etaMinutes = utils.getTimeDifference(etaData.optString("eta", "N/A"));
                        } else if (item.getCompany().equals("gmb")) {
                            etaTime = utils.parseTime(etaData.optString("timestamp", "N/A"));
                            etaMinutes = etaData.optString("diff", "N/A");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing ETA time: " + e.getMessage());
                        etaTime = "N/A";
                        etaMinutes = "N/A";
                    }

                    String displayText = etaMinutes.equals("N/A") ?
                            "---" :
                            etaTime + " " + etaMinutes + " " + localizedContext.getString(R.string.bus_eta_minute_text_name);

                    newEtaData.add(displayText);

                    if (i == 0 && !etaMinutes.equals("N/A")) {
                        try {
                            if (etaMinutes.equals("0")) {
                                item.setClosestETA(localizedContext.getString(R.string.detail_view_eta_arriving_name));
                            } else if (Integer.parseInt(etaMinutes) < 0) {
                                item.setClosestETA("---");
                            } else {
                                item.setClosestETA(etaMinutes + " " + context.getString(R.string.bus_eta_minute_text_name));
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing ETA minutes: " + e.getMessage());
                            item.setClosestETA("---");
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ETA data: " + e.getMessage());
            newEtaData.add("Error: " + e.getMessage());
            item.setClosestETA("Error");
        }

        item.setEtaData(newEtaData);
        updateUI(position);
    }

    private void updateUI(int position) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (position < busRouteStopItems.size()) {
                notifyItemChanged(position);
            }
        });
    }

    /**
     * ViewHolder class for the adapter
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRouteName, tvDestination, tvStopName, tvRouteBusCompany;
        TextView routeSpecialIndicator, tvRouteRemarks;
        TextView tvEta1, tvEta2, tvEta3, tvRouteId;
        LinearLayout busRouteItemView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            busRouteItemView = itemView.findViewById(R.id.item_nearby_bus_route_info_holder);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvDestination = itemView.findViewById(R.id.tvNearbyRouteDestination);
            tvStopName = itemView.findViewById(R.id.tvNearbyRouteStopName);
            tvRouteBusCompany = itemView.findViewById(R.id.tvNearbyRouteBusCompany);
            routeSpecialIndicator = itemView.findViewById(R.id.tvNearbyRouteBusSpecialIndicator);
            tvRouteRemarks = itemView.findViewById(R.id.tvNearbyRouteRemarks);
            tvEta1 = itemView.findViewById(R.id.tvNearbyRouteETA);
            tvEta2 = itemView.findViewById(R.id.tvNearbyRouteETA2);
            tvEta3 = itemView.findViewById(R.id.tvNearbyRouteETA3);
            tvRouteId = itemView.findViewById(R.id.tvNearbyRouteId);
            
            // Enable marquee effect for text views that might need it
            tvDestination.setSelected(true);
            tvStopName.setSelected(true);
        }
    }
}
