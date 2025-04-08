package com.chung.a9rushtobus.elements;

import static android.provider.Settings.System.getConfiguration;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.BusRouteDetailViewActivity;
import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;
import com.chung.a9rushtobus.Utils;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.service.BackgroundService;
import com.chung.a9rushtobus.service.DataFetcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavedBusStopAdapter extends RecyclerView.Adapter<SavedBusStopAdapter.ViewHolder> {
    private static final String TAG = "SavedBusStopAdapter";
    private static final int UPDATE_INTERVAL_MS = 30000; // 60 seconds

    private final Context context;
    private final List<BusRouteStopItem> items;
    private final DataFetcher dataFetcher;
    private final Utils utils;
    private final DatabaseHelper databaseHelper;
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = this::refreshAllEtaData;
    private boolean isUpdating = false;

    public SavedBusStopAdapter(Context context, List<BusRouteStopItem> items, Utils utils) {
        this.context = context;
        this.items = items;
        this.utils = utils;
        this.dataFetcher = new DataFetcher(context);
        this.databaseHelper = DatabaseHelper.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_bus_route, parent, false);
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
        BusRouteStopItem item = items.get(position);
        String destination = null;

        // Set basic bus route information
        holder.routeName.setText(item.getRoute());
        // Apply color styling to route number
        setTextColorAndBackground(holder.routeName, item.getRoute());
        
        switch (item.getCompany()) {
            case "kmb":
                holder.busCompany.setText(R.string.bus_company_kmb_name);
                break;
            case "ctb":
                holder.busCompany.setText(R.string.bus_company_ctb_name);
                break;
            case "gmb":
                holder.busCompany.setText(R.string.bus_company_gmb_name + " - ");
                break;
            default:
                holder.busCompany.setText(item.getCompany());
        }

        // Show both destination and stop name
        // For the stop name
        holder.stopName.setText(item.getStopName());
        holder.stopName.setVisibility(View.VISIBLE);

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
                        holder.destination.setText("往 " + destination.trim().toUpperCase());
                        break;
                default:
                    holder.destination.setText("TO " + destination.trim().toUpperCase());

            }
        } else {
            holder.destination.setText("");
        }
        holder.destination.setSelected(true);

        // Handle ETAs
        if (item.getClosestETA() != null) {
            holder.eta1.setText(item.getClosestETA());
            holder.eta1.setVisibility(View.VISIBLE);
        } else {
            holder.eta1.setText("---");
            holder.eta1.setVisibility(View.VISIBLE);
        }

        // Handle additional ETAs if available
        List<String> etaData = item.getEtaDataFull();
        Log.d(TAG, "ETA data: " + etaData);
        if (etaData != null && etaData.size() > 0) {
            // First ETA already handled above with getClosestETA()

            // Handle second ETA
            if (etaData.size() > 1) {
                holder.eta2.setText(etaData.get(1));
                holder.eta2.setVisibility(View.VISIBLE);
            } else {
                holder.eta2.setVisibility(View.GONE);
            }

            // Handle third ETA
            if (etaData.size() > 2) {
                holder.eta3.setText(etaData.get(2));
                holder.eta3.setVisibility(View.VISIBLE);
            } else {
                holder.eta3.setVisibility(View.GONE);
            }
        } else {
            // Hide additional ETAs if there's no data
            holder.eta2.setVisibility(View.GONE);
            holder.eta3.setVisibility(View.GONE);
        }

        // Make route and stop clickable to see details
        String finalDestination = destination;
        holder.infoHolder.setOnClickListener(v -> {
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

        // Set click listener for remove button
        holder.routeId.setText(item.getRoute() + "_" + item.getStopID());

        // Track and remove buttons (in detailed expanded view)
        holder.infoHolder.setOnLongClickListener(v -> {
            // Show options to track or remove
            showOptionsDialog(item, position);
            return true;
        });
    }

    private void setTextColorAndBackground(TextView textView, String routeNumber) {
        if (routeNumber.startsWith("A") || routeNumber.startsWith("E")) {
            textView.setBackgroundColor(context.getColor(R.color.externalBusBackground));
            textView.setTextColor(context.getColor(R.color.externalBusForeground));
        } else if (routeNumber.startsWith("N")) {
            textView.setBackgroundColor(Color.GRAY);
            textView.setTextColor(Color.WHITE);
        } else if (routeNumber.matches("^[169]\\d{2}[A-Za-z]?$")) {
            textView.setBackgroundColor(context.getColor(R.color.crossHarbourBusBackground));
            textView.setTextColor(context.getColor(R.color.crossHarbourBusForeground));
        } else {
            textView.setBackgroundColor(Color.TRANSPARENT);
            textView.setTextColor(context.getColor(R.color.foreground));
        }
    }

    private void showOptionsDialog(BusRouteStopItem item, int position) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(item.getRoute() + " - " + item.getStopName());

        String[] options = {"Track This Bus", "Remove from Saved"};
        Context localizedContext = getLocalizedContext();

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Track this bus
                Intent serviceIntent = new Intent(context, BackgroundService.class);
                serviceIntent.putExtra("stop_item", item);
                context.startService(serviceIntent);
                Toast.makeText(context,
                    localizedContext.getString(R.string.notif_tracking_started, item.getStopName()),
                    Toast.LENGTH_SHORT).show();
            } else {
                // Remove from saved
                boolean removed = databaseHelper.savedRoutesManager.removeRouteStop(item);
                if (removed) {
                    Toast.makeText(context,
                        localizedContext.getString(R.string.saved_stop_removed, item.getStopName()),
                        Toast.LENGTH_SHORT).show();
                    items.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, items.size());

                    // If list is now empty, notify fragment to update UI
                    if (items.isEmpty()) {
                        // This is a bit of a hack, but works to trigger the fragment's UI update
                        notifyDataSetChanged();
                    }
                }
            }
        });

        builder.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Periodic ETA updates methods
    public void startPeriodicUpdates() {
        if (isUpdating) {
            return;
        }

        if (items.isEmpty()) {
            Log.d("ETARefresh", "No saved stops to update.");
            return;
        }

        isUpdating = true;
        updateHandler.post(updateRunnable);
        Log.d("ETARefresh", "Started periodic ETA updates for saved stops");
    }

    public void pauseETAUpdates() {
        if (isUpdating) {
            updateHandler.removeCallbacks(updateRunnable);
            isUpdating = false;
            Log.d("ETARefresh", "Stopped periodic ETA updates for saved stops");
        }
    }

    public void resumeETAUpdates() {
        startPeriodicUpdates();
    }

    private void refreshAllEtaData() {
        Log.d("ETARefresh", "Refreshing ETA data for " + items.size() + " saved stops");

        for (int position = 0; position < items.size(); position++) {
            BusRouteStopItem item = items.get(position);
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
                Log.d("ETARefresh", "No ETA data received for saved stop " + position);
                item.setClosestETA("---");
            } else {
                for (int i = 0; i < Math.min(etaDataArray.length(), 3); i++) {
                    JSONObject etaData = etaDataArray.getJSONObject(i);
                    String etaTime = "N/A", etaMinutes = "N/A";

                    if (item.getCompany().equals("ctb") || item.getCompany().equals("kmb")) {
                        etaTime = utils.parseTime(etaData.optString("eta", "N/A"));
                        etaMinutes = utils.getTimeDifference(etaData.optString("eta", "N/A"));
                    } else if (item.getCompany().equals("gmb")) {
                        etaTime = utils.parseTime(etaData.optString("timestamp", "N/A"));
                        etaMinutes = etaData.optString("diff", "N/A");
                    }

                    String displayText = etaMinutes.equals("N/A") ?
                            "---" :
                            etaTime + " " + etaMinutes + " " + localizedContext.getString(R.string.bus_eta_minute_text_name);

                    newEtaData.add(displayText);

                    if (i == 0 && !etaMinutes.equals("N/A")) {
                        if (etaMinutes.equals("0")) {
                            item.setClosestETA(localizedContext.getString(R.string.detail_view_eta_arriving_name));
                        } else if (Integer.parseInt(etaMinutes) < 0) {
                            item.setClosestETA("---");
                        } else {
                            item.setClosestETA(etaMinutes + " " + context.getString(R.string.bus_eta_minute_text_name));
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
            if (position < items.size()) {
                notifyItemChanged(position);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView routeName, specialIndicator, busCompany, destination, stopName, remarks;
        TextView eta1, eta2, eta3, routeId;
        LinearLayout infoHolder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            routeName = itemView.findViewById(R.id.tvRouteName);
            specialIndicator = itemView.findViewById(R.id.tvNearbyRouteBusSpecialIndicator);
            busCompany = itemView.findViewById(R.id.tvNearbyRouteBusCompany);
            destination = itemView.findViewById(R.id.tvNearbyRouteDestination);
            stopName = itemView.findViewById(R.id.tvNearbyRouteStopName);
            remarks = itemView.findViewById(R.id.tvNearbyRouteRemarks);
            eta1 = itemView.findViewById(R.id.tvNearbyRouteETA);
            eta2 = itemView.findViewById(R.id.tvNearbyRouteETA2);
            eta3 = itemView.findViewById(R.id.tvNearbyRouteETA3);
            routeId = itemView.findViewById(R.id.tvNearbyRouteId);
            infoHolder = itemView.findViewById(R.id.item_nearby_bus_route_info_holder);
            
            remarks.setVisibility(View.GONE);
            specialIndicator.setVisibility(View.GONE);
        }
    }


}
