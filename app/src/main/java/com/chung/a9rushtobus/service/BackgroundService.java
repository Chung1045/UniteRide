package com.chung.a9rushtobus.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.chung.a9rushtobus.BusRouteDetailViewActivity;
import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;
import com.chung.a9rushtobus.elements.BusRouteStopItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
    private static final String CHANNEL_ID = "BusTrackingChannel";
    private static final int UPDATE_INTERVAL_MS = 60000; // 60 seconds
    private static final String ACTION_STOP_TRACKING = "com.chung.a9rushtobus.action.STOP_TRACKING";
    private Handler handler;
    private DataFetcher dataFetcher;
    private NotificationManager notificationManager;
    private Map<String, TrackingInfo> trackingStops = new HashMap<>();
    
    // Generate unique notification ID for each route
    private int generateNotificationId(BusRouteStopItem stop) {
        return (stop.getCompany() + stop.getRoute() + stop.getStopID()).hashCode();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        dataFetcher = new DataFetcher(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_TRACKING.equals(action)) {
                Log.d(TAG, "Received stop tracking action");
                String stopKey = intent.getStringExtra("stop_key");
                if (stopKey != null) {
                    stopTracking(stopKey);
                }
                return START_NOT_STICKY;
            } else if (intent.hasExtra("stop_item")) {
                BusRouteStopItem stopItem = intent.getParcelableExtra("stop_item");
                if (stopItem != null) {
                    String stopKey = stopItem.getCompany() + stopItem.getRoute() + stopItem.getStopID();
                    startTracking(stopKey, stopItem);
                }
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW  // Use LOW importance for sticky behavior
            );
            channel.setDescription(getString(R.string.notif_channel_description));
            channel.setShowBadge(false);  // Don't show badge on app icon
            channel.enableLights(false);   // No notification light
            channel.enableVibration(false); // No vibration
            channel.setSound(null, null);  // No sound
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startTracking(String stopKey, BusRouteStopItem stopItem) {
        if (trackingStops.containsKey(stopKey)) {
            return;
        }

        int notificationId = generateNotificationId(stopItem);
        Handler stopHandler = new Handler(Looper.getMainLooper());
        TrackingInfo trackingInfo = new TrackingInfo(stopItem, stopHandler, notificationId);
        trackingStops.put(stopKey, trackingInfo);
        
        fetchEtaAndUpdateNotification(stopKey);
    }

    private void stopTracking(String stopKey) {
        Log.d(TAG, "Stopping bus tracking for " + stopKey);
        TrackingInfo info = trackingStops.get(stopKey);
        if (info != null) {
            info.setTracking(false);
            info.getHandler().removeCallbacksAndMessages(null);
            notificationManager.cancel(info.getNotificationId());
            trackingStops.remove(stopKey);
        }

        if (trackingStops.isEmpty()) {
            stopForeground(true);
            stopSelf();
        }
    }

    private void fetchEtaAndUpdateNotification(String stopKey) {
        TrackingInfo info = trackingStops.get(stopKey);
        if (info == null || !info.isTracking()) {
            return;
        }

        BusRouteStopItem stop = info.getStopItem();
        if (stop.getCompany().equals("kmb") || stop.getCompany().equals("ctb")) {
            dataFetcher.fetchStopETA(
                    stop.getStopID(),
                    stop.getRoute(),
                    stop.getServiceType(),
                    stop.getCompany(),
                    etaData -> processEtaData(stopKey, etaData),
                    error -> handleEtaError(stopKey, error)
            );
        } else if (stop.getCompany().equals("gmb")) {
            dataFetcher.fetchGMBStopETA(
                    stop.getGmbRouteID(),
                    stop.getGmbRouteSeq(),
                    stop.getStopSeq(),
                    etaData -> processEtaData(stopKey, etaData),
                    error -> handleEtaError(stopKey, error)
            );
        }
    }

    private void processEtaData(String stopKey, JSONArray etaDataArray) {
        try {
            String notificationText;
            TrackingInfo info = trackingStops.get(stopKey);
            if (info == null) return;
            BusRouteStopItem stop = info.getStopItem();
            
            StringBuilder etaBuilder = new StringBuilder();
            // Add stop name at the top
            etaBuilder.append(stop.getStopName()).append("\n\n");

            if (etaDataArray.length() == 0) {
                etaBuilder.append(getString(R.string.bus_eta_msg_noBus_name));
                notificationText = etaBuilder.toString();
            } else {

                // Process first valid ETA
                JSONObject firstEta = null;
                for (int i = 0; i < etaDataArray.length(); i++) {
                    JSONObject eta = etaDataArray.getJSONObject(i);
                    
                    if (shouldSkipEta(eta, stop)) continue;

                    String remarks = getRemarks(eta, stop);
                    if (!remarks.isEmpty()) {
                        etaBuilder.append(remarks).append("\n");
                    }

                    firstEta = eta;
                    appendEtaInfo(eta, etaBuilder, true);
                    break;
                }

                if (firstEta == null) {
                    notificationText = getString(R.string.bus_eta_msg_noBus_name);
                    updateNotification(stopKey, notificationText);
                    return;
                }

                // Process remaining valid ETAs
                for (int i = 0; i < Math.min(etaDataArray.length(), 3); i++) {
                    JSONObject eta = etaDataArray.getJSONObject(i);

                    TrackingInfo currentInfo = trackingStops.get(stopKey);
                    if (currentInfo == null) continue;
                    BusRouteStopItem currentStop = currentInfo.getStopItem();
                    
                    if (shouldSkipEta(eta, currentStop)) continue;
                    if (eta == firstEta) continue;

                    String remarks = getRemarks(eta, currentStop);
                    if (!remarks.isEmpty()) {
                        etaBuilder.append("\n").append(remarks);
                    }

                    etaBuilder.append("\n");
                    appendEtaInfo(eta, etaBuilder, false);

                }

                notificationText = etaBuilder.toString();
            }

            updateNotification(stopKey, notificationText);
            
            if (info != null && info.isTracking()) {
                info.getHandler().postDelayed(() -> fetchEtaAndUpdateNotification(stopKey), UPDATE_INTERVAL_MS);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error processing ETA data: " + e.getMessage());
            updateNotification(stopKey, "Error processing ETA data");
        }
    }

    private boolean shouldSkipEta(JSONObject eta, BusRouteStopItem stop) {
        try {
            return stop.getCompany().equals("kmb") &&
                    eta.has("service_type") &&
                    !eta.getString("service_type").equals(stop.getServiceType());
        } catch (JSONException e) {
            return true; // skip if service_type is missing or invalid
        }
    }


    private void appendEtaInfo(JSONObject etaObj, StringBuilder builder, boolean isFirst) throws JSONException {
        String etaTime, etaMinutes;
        Context localizedContext = getLocalizedContext();

        if (etaObj.has("eta")) {
            etaTime = parseTime(etaObj.optString("eta", "N/A"));
            etaMinutes = getTimeDifference(etaObj.optString("eta", "N/A"));
        } else {
            etaTime = parseTime(etaObj.optString("timestamp", "N/A"));
            etaMinutes = etaObj.optString("diff", "N/A");
        }

        if (etaMinutes.equals("N/A")) {
            builder.append("N/A (")
                   .append(localizedContext.getString(R.string.bus_eta_msg_noBus_name))
                   .append(")");
        } else if (etaMinutes.equals("0")) {
            if (isFirst) {
                builder.append(etaTime).append(" (")
                        .append(localizedContext.getString(R.string.detail_view_eta_arriving_name))
                        .append(")");
            } else {
                builder.append(localizedContext.getString(R.string.detail_view_eta_arriving_name));
            }
        } else if (Integer.parseInt(etaMinutes) < 0 && isFirst) {
            builder.append(etaTime).append(" (")
                    .append("---)");
        } else {
            builder.append(etaTime).append(" (")
                    .append(etaMinutes).append(" ")
                    .append(localizedContext.getString(R.string.bus_eta_minute_text_name))
                    .append(")");
        }
    }

    private String getRemarks(JSONObject eta, BusRouteStopItem stop) throws JSONException {
        String company = stop.getCompany();
        if (company.equals("kmb") || company.equals("ctb")) {
            return getLocalizedRemarks(eta, "rmk_en", "rmk_tc", "rmk_sc");
        } else if (company.equals("gmb")) {
            return getLocalizedRemarks(eta, "remarks_en", "remarks_tc", "remarks_sc");
        }
        return "";
    }


    private String parseTime(String timestamp) {
        if (timestamp.equals("N/A")) {
            return "N/A";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.US);
            Date date = inputFormat.parse(timestamp);
            return date != null ? outputFormat.format(date) : "N/A";
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time: " + e.getMessage());
            return "N/A";
        }
    }

    private String getTimeDifference(String timestamp) {
        if (timestamp.equals("N/A")) {
            return "N/A";
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            format.setTimeZone(TimeZone.getDefault());

            Date etaTime = format.parse(timestamp);
            if (etaTime == null) {
                return "N/A";
            }

            long diffInMillis = etaTime.getTime() - System.currentTimeMillis();
            long diffInMinutes = diffInMillis / (60 * 1000);

            return String.valueOf(diffInMinutes);
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating time difference: " + e.getMessage());
            return "N/A";
        }
    }

    private String getLocalizedRemarks(JSONObject etaData, String enKey, String tcKey, String scKey) throws JSONException {
        String appLang = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
        String remarks;

        switch (appLang) {
            case "zh-rCN":
                remarks = etaData.optString(scKey, "");
                break;
            case "zh-rHK":
                remarks = etaData.optString(tcKey, "");
                break;
            default:
                remarks = etaData.optString(enKey, "");
        }

        return remarks;
    }

    private void handleEtaError(String stopKey, String error) {
        Log.e(TAG, "Error fetching ETA: " + error);
        updateNotification(stopKey, "Error: " + error);

        // Retry after delay
        TrackingInfo info = trackingStops.get(stopKey);
        if (info != null && info.isTracking()) {
            info.getHandler().postDelayed(() -> fetchEtaAndUpdateNotification(stopKey), UPDATE_INTERVAL_MS);
        }
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
        
        Configuration configuration = new Configuration(getResources().getConfiguration());
        configuration.setLocale(locale);
        return createConfigurationContext(configuration);
    }

private void updateNotification(String stopKey, String content) {
        TrackingInfo info = trackingStops.get(stopKey);
        if (info == null) return;
        
        BusRouteStopItem trackedStop = info.getStopItem();
        // Debug logging
        Log.d(TAG, "Current Locale: " + getResources().getConfiguration().locale);
        Log.d(TAG, "App Language Setting: " + UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en"));
        Log.d(TAG, "Company: " + trackedStop.getCompany());
        Log.d(TAG, "Tracking Message: " + getString(R.string.notif_trackBus_name));
        Intent notificationIntent = new Intent(this, BusRouteDetailViewActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Create stop tracking intent
        Intent stopIntent = new Intent(this, BackgroundService.class);
        stopIntent.setAction(ACTION_STOP_TRACKING);
        stopIntent.putExtra("stop_key", stopKey);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String companyDisplayName = "";

        Context localizedContext = getLocalizedContext();
        switch (trackedStop.getCompany()) {
            case "kmb":
                companyDisplayName = localizedContext.getString(R.string.bus_company_kmb_name);
                break;
            case "ctb":
                companyDisplayName = localizedContext.getString(R.string.bus_company_ctb_name);
                break;
            case "gmb":
                companyDisplayName = localizedContext.getString(R.string.bus_company_gmb_name);
                break;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(localizedContext.getString(R.string.notif_trackBus_name) + " " + companyDisplayName + " " + trackedStop.getRoute())
                .setContentText(content)
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setPriority(NotificationCompat.PRIORITY_LOW)  // Lower priority for sticky behavior
                .setOngoing(true)  // Makes it persistent
                .setAutoCancel(false)  // Prevent auto-cancellation
                .setOnlyAlertOnce(true)  // Only alert the first time
                .setSilent(true)  // No sound
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.baseline_notifications_none_24, getString(R.string.notif_stop_tracking), stopPendingIntent);

        if (content.contains("\n")) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        }

        if (trackingStops.size() == 1) {
            startForeground(info.getNotificationId(), builder.build());
        } else {
            notificationManager.notify(info.getNotificationId(), builder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop tracking for all stops
        for (String stopKey : new ArrayList<>(trackingStops.keySet())) {
            stopTracking(stopKey);
        }
    }
}
