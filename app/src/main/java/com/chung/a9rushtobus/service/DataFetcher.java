package com.chung.a9rushtobus.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.chung.a9rushtobus.database.CTBDatabase;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.elements.BusRoute;
import com.chung.a9rushtobus.elements.RTHKTrafficEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DataFetcher {
    
    /**
     * Interface for tracking data fetching progress
     */
    public interface ProgressCallback {
        /**
         * Called when a progress update is available
         * 
         * @param progressText The progress text to display
         */
        void onProgressUpdate(String progressText);
        
        /**
         * Called when the data fetching is complete
         * 
         * @param success Whether the operation was successful
         * @param message A message describing the result
         */
        void onComplete(boolean success, String message);
    }
    private static final String KMB_BASE_URL = "https://data.etabus.gov.hk/v1/transport/kmb/";
    private static final String CTB_BASE_URL = "https://rt.data.gov.hk/v2/transport/citybus/";
    private static final String GMB_BASE_URL = "https://data.etagmb.gov.hk/";
    private static final String TRAFFIC_NEWS_URL = "https://programme.rthk.hk/channel/radio/trafficnews/index.php";
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client;
    private DatabaseHelper databaseHelper;
    private Context context;
    
    // Flag to track if the current operation has been cancelled
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    public DataFetcher(Context context) {
        databaseHelper = DatabaseHelper.getInstance(context);
        this.context = context;

        okhttp3.Dispatcher dispatcher = new okhttp3.Dispatcher();
        dispatcher.setMaxRequests(4);
        dispatcher.setMaxRequestsPerHost(4);

        this.client = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }
    
    public void refreshAllData(ProgressCallback progressCallback) {
        // Reset cancellation state at the start of a new operation
        resetCancellationState();
        
        if (progressCallback != null) {
            progressCallback.onProgressUpdate("Step 1/4: Backing up database...");
        }
        
        // Create a completion tracker
        final AtomicInteger totalTasksCompleted = new AtomicInteger(0);
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);
        final AtomicBoolean completionReported = new AtomicBoolean(false);
        
        // Helper method to check if all tasks have completed
        Runnable checkCompletion = () -> {
            // If cancelled, don't proceed with completion logic
            if (isCancelled()) {
                if (!completionReported.getAndSet(true)) {
                    Log.d("DataFetcher", "Operation was cancelled, skipping completion");
                    // No need to call onComplete as it should have been called when cancellation happened
                }
                return;
            }
            
            int completed = totalTasksCompleted.incrementAndGet();
            Log.d("DataFetcher", "Task completed. Total completed tasks: " + completed);
            
            // We expect 4 main tasks to complete (backup, KMB, CTB, GMB)
            if (completed >= 4 && !completionReported.getAndSet(true)) {
                // All tasks are complete; log and show a toast message on the main thread
                if (errorOccurred.get()) {
                    Log.e("DataFetcher", "Data fetch completed with errors");
                    mainHandler.post(() -> {
                        if (!isCancelled()) {  // Double-check cancellation before showing UI
                            Toast.makeText(context, "Data fetch completed with errors", Toast.LENGTH_LONG).show();
                            if (progressCallback != null) {
                                progressCallback.onComplete(false, "Data fetch completed with errors");
                            }
                        }
                    });
                } else {
                    Log.d("DataFetcher", "All data fetched successfully");
                    mainHandler.post(() -> {
                        if (!isCancelled()) {  // Double-check cancellation before showing UI
                            Toast.makeText(context, "All data fetched successfully", Toast.LENGTH_LONG).show();
                            if (progressCallback != null) {
                                progressCallback.onComplete(true, "All data fetched successfully");
                            }
                        }
                    });
                }
            }
        };
        
        backupDatabase(success -> {
            // Check if operation was cancelled during backup
            if (isCancelled()) {
                Log.d("DataFetcher", "Operation cancelled during database backup");
                return;
            }
            
            if (success) {
                Log.d("DataFetcher", "Database backup successful");
                databaseHelper.removeAllValues();
                
                if (progressCallback != null && !isCancelled()) {
                    progressCallback.onProgressUpdate("Database backup successful. Preparing to fetch data...");
                }
                
                // Check cancellation again before starting KMB fetch
                if (isCancelled()) {
                    Log.d("DataFetcher", "Operation cancelled after database backup");
                    return;
                }
                
                if (progressCallback != null) {
                    progressCallback.onProgressUpdate("Step 2/4: Fetching KMB bus routes...");
                }
                
                fetchAllBusRoutes(
                        routes -> {
                            // Check if operation was cancelled during KMB fetch
                            if (isCancelled()) {
                                Log.d("DataFetcher", "Operation cancelled during KMB routes fetch");
                                return;
                            }
                            
                            Log.d("DataFetcher", "All bus routes fetched successfully");
                            if (progressCallback != null) {
                                progressCallback.onProgressUpdate("Step 2/4 completed: KMB bus routes fetched successfully");
                            }
                            checkCompletion.run();
                        },
                        error -> {
                            // Check if operation was cancelled during KMB fetch
                            if (isCancelled()) {
                                Log.d("DataFetcher", "Operation cancelled during KMB routes fetch (error)");
                                return;
                            }
                            
                            if (!errorOccurred.get()) {
                                errorOccurred.set(true);
                                Log.e("DataFetcher", "Error fetching bus routes: " + error);
                                if (progressCallback != null) {
                                    progressCallback.onProgressUpdate("Error in Step 2/4: " + error);
                                }
                                handleDataFetchFailure();
                            }
                            checkCompletion.run();
                        }
                );

                // Check cancellation again before starting CTB fetch
                if (isCancelled()) {
                    Log.d("DataFetcher", "Operation cancelled before CTB routes fetch");
                    return;
                }
                
                if (progressCallback != null) {
                    progressCallback.onProgressUpdate("Step 3/4: Fetching CTB routes...");
                }
                
                // Fetch all CTB routes (which internally triggers fetching of CTB route stops and stops)
                fetchAllCTBRoutes(
                        message -> {
                            // Check if operation was cancelled during CTB fetch
                            if (isCancelled()) {
                                Log.d("DataFetcher", "Operation cancelled during CTB routes fetch");
                                return;
                            }
                            
                            Log.d("DataFetcher", "All CTB routes fetched successfully");
                            if (progressCallback != null) {
                                progressCallback.onProgressUpdate("Step 3/4 completed: CTB routes fetched successfully");
                            }
                            checkCompletion.run();
                        },
                        error -> {
                            // Check if operation was cancelled during CTB fetch
                            if (isCancelled()) {
                                Log.d("DataFetcher", "Operation cancelled during CTB routes fetch (error)");
                                return;
                            }
                            
                            if (!errorOccurred.get()) {
                                errorOccurred.set(true);
                                Log.e("DataFetcher", "Error fetching CTB routes: " + error);
                                if (progressCallback != null) {
                                    progressCallback.onProgressUpdate("Error in Step 3/4: " + error);
                                }
                            }
                            checkCompletion.run();
                        }
                );

                // Check cancellation again before starting GMB fetch
                if (isCancelled()) {
                    Log.d("DataFetcher", "Operation cancelled before GMB routes fetch");
                    return;
                }
                
                if (progressCallback != null) {
                    progressCallback.onProgressUpdate("Step 4/4: Fetching GMB routes...");
                }
                
                // Create a wrapper for the GMB routes progress callback
                ProgressCallback gmbCallback = new ProgressCallback() {
                    @Override
                    public void onProgressUpdate(String progressText) {
                        if (progressCallback != null && !isCancelled()) {
                            progressCallback.onProgressUpdate(progressText);
                        }
                    }

                    @Override
                    public void onComplete(boolean success, String message) {
                        // Check if operation was cancelled during GMB fetch
                        if (isCancelled()) {
                            Log.d("DataFetcher", "Operation cancelled during GMB routes fetch completion");
                            return;
                        }
                        
                        // This will be called when GMB routes fetching is complete
                        Log.d("DataFetcher", "GMB routes fetching completed: " + message);
                        checkCompletion.run();
                    }
                };
                
                // Pass the completion callback to GMB routes fetching
                fetchAllGMBRoutes(gmbCallback);
                
                // Count the backup as a completed task
                checkCompletion.run();

            } else {
                Log.e("DataFetcher", "Database backup failed");
                if (!isCancelled()) {
                    Toast.makeText(context, "Database backup failed. Please try again later.", Toast.LENGTH_SHORT).show();
                    if (progressCallback != null) {
                        progressCallback.onComplete(false, "Database backup failed. Please try again later.");
                    }
                }
            }
        });
    }



    // Handle failures by restoring the database
    private void handleDataFetchFailure() {
        Log.d("DataFetcher", "Attempting to restore the database...");
        restoreDatabase(success -> {
            if (success) {
                Log.d("DataFetcher", "Database restored successfully.");
            } else {
                Log.e("DataFetcher", "Database restoration failed.");
            }
        });
    }

    public void fetchAllBusRoutes(Consumer<List<BusRoute>> onSuccess, Consumer<String> onError) {
        Request request = new Request.Builder()
                .url("https://data.etabus.gov.hk/v1/transport/kmb/route/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> onError.accept("Failed to fetch data: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("FetchLog", "All KMB Routes Response: " + response);
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> onError.accept("Error: " + response.code()));
                }

                try {
                    String jsonData = response.body().string();
                    List<BusRoute> routes = parseBusRouteJsonData(jsonData);
                    mainHandler.post(() -> onSuccess.accept(routes));
                } catch (JSONException e) {
                    mainHandler.post(() -> onError.accept("Error parsing JSON: " + e.getMessage()));
                }
            }
        });
    }

    public void fetchCTBStop(String stopID, Consumer<String> onSuccess, Consumer<String> onError) {
        String url = CTB_BASE_URL + "stop/" + stopID ;
        Request request = new Request.Builder().url(url).build();
        Log.d("FetchLog", "Making request for stop " + stopID + " to URL: " + url);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchLog", "Error CTB bus stop info: " + e.getMessage());
                Log.d("DataFetcher", "Error Fetching CTB bus stop info: " + e.getMessage());
                onError.accept("Failed to fetch data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.e("FetchLog", "CTB Stop Info Fetch Response: " + response);
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> Log.e("DataFetchCTB", "Error Fetching CTB stop info: " + response.code()));
                    Log.e("FetchLog", "Response is un-successful");
                    response.close();
                    onError.accept("Error: " + response.code());
                    return;
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        processCTBStop(jsonData);
                        onSuccess.accept(jsonData);
                        response.close();
                    } catch (Exception e) {
                        Log.e("DataFetch", "Error Fetching CTB bus stop info: " + e.getMessage());
                        Log.e("DataFetchCTB", "Error Fetching CTB bus stop info: " + e.getMessage());
                        onError.accept("Error processing data: " + e.getMessage());
                        response.close();
                    }
                });
            }
        });
    }

    // Processes the JSON data for a CTB stop and updates the database immediately
    private void processCTBStop(String jsonData) throws JSONException {
        Log.d("FetchLog", "Processing CTB stop data");
        JSONObject jsonObject = new JSONObject(jsonData);
        // "data" is a JSONObject, not a JSONArray
        JSONObject dataObject = jsonObject.getJSONObject("data");

        String stop = dataObject.getString("stop");
        String nameEn = dataObject.getString("name_en");
        String nameTc = dataObject.getString("name_tc");
        String nameSc = dataObject.getString("name_sc");
        String lat = dataObject.getString("lat");
        String lng = dataObject.getString("long");

        Log.d("FetchLog", "Saving stop: " + stop + " - " + nameEn);
        // Update the database. Ensure that updateCTBStops() is thread-safe.
        databaseHelper.ctbDatabase.updateCTBStops(stop, nameEn, nameTc, nameSc, lat, lng);
    }

    public void fetchKMBStop(String stopID, Consumer<String> onSuccess, Consumer<String> onError) {
        String url = KMB_BASE_URL + "stop/" + stopID ;
        Request request = new Request.Builder().url(url).build();
        Log.d("FetchLog", "Making request for stop " + stopID + " to URL: " + url);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchLog", "Error KMB bus stop info: " + e.getMessage());
                Log.d("DataFetcher", "Error Fetching KMB bus stop info: " + e.getMessage());
                onError.accept("Failed to fetch data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.e("FetchLog", "KMB Stop Info Fetch Response: " + response);
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> Log.e("DataFetchKMB", "Error Fetching KMB stop info: " + response.code()));
                    Log.e("FetchLog", "Response is un-successful");
                    response.close();
                    onError.accept("Error: " + response.code());
                    return;
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        processKMBStop(jsonData);
                        onSuccess.accept(jsonData);
                        response.close();
                    } catch (Exception e) {
                        Log.e("DataFetch", "Error Fetching KMB bus stop info: " + e.getMessage());
                        Log.e("DataFetchKMB", "Error Fetching KMB bus stop info: " + e.getMessage());
                        onError.accept("Error processing data: " + e.getMessage());
                        response.close();
                    }
                });
            }
        });
    }

    // Processes the JSON data for a CTB stop and updates the database immediately
    private void processKMBStop(String jsonData) throws JSONException {
        Log.d("FetchLog", "Processing KMB stop data");
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONObject dataObject = jsonObject.getJSONObject("data");

        String stop = dataObject.getString("stop");
        String nameEn = dataObject.getString("name_en");
        String nameTc = dataObject.getString("name_tc");
        String nameSc = dataObject.getString("name_sc");
        String lat = dataObject.getString("lat");
        String lng = dataObject.getString("long");

        Log.d("FetchLog", "Saving stop: " + stop + " - " + nameEn);
        // Update the database. Ensure that updateCTBStops() is thread-safe.
        databaseHelper.kmbDatabase.updateKMBStop(stop, nameEn, nameTc, nameSc, lat, lng);
    }

    public void fetchAllCTBRoutes(Consumer<String> onSuccess, Consumer<String> onError) {
        String url = CTB_BASE_URL + "route/CTB";
        Request request = new Request.Builder().url(url).build();

        Log.d("FetchLog", "Fetching All CTB routes");
        executorService.execute(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e("FetchLog", "Error fetching All CTB routes " + response.code());
                    mainHandler.post(() -> onError.accept("Error: " + response.code()));
                    return;
                }
                String jsonData = response.body().string();
                processAllCTBRoutes(jsonData);
                mainHandler.post(() -> onSuccess.accept("Routes fetched"));
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> onError.accept("Error: " + e.getMessage()));
            }
        });
    }

    public void processAllCTBRoutes(String JSONData) throws JSONException {
        Log.d("DataFetchCTB", "Processing all CTB routes");
        JSONObject jsonObject = new JSONObject(JSONData);
        JSONArray routesArray = jsonObject.getJSONArray("data");

        Log.d("DataFetchCTB", "Number of routes to process: " + routesArray.length());

        for (int i = 0; i < routesArray.length(); i++) {
            JSONObject routeObject = routesArray.getJSONObject(i);
            String route = routeObject.getString("route");
            String origEn = routeObject.getString("orig_en");
            String origTc = routeObject.getString("orig_tc");
            String origSc = routeObject.getString("orig_sc");
            String destEn = routeObject.getString("dest_en");
            String destTc = routeObject.getString("dest_tc");
            String destSc = routeObject.getString("dest_sc");

            Log.d("DataFetchCTB", "Processing route " + (i + 1) + "/" + routesArray.length() + ": " + route);
            databaseHelper.ctbDatabase.updateCTBRoute(route, origEn, origTc, origSc, destEn, destTc, destSc);
        }
        Log.d("DataFetchCTB", "Finished processing all CTB routes");
        fetchAllCTBRouteStops();
    }

    private void fetchAllCTBRouteStops() {
        Log.d("DataFetcher", "Fetching all CTB route stops");
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + CTBDatabase.Tables.CTB_ROUTES.COLUMN_ROUTE + " FROM " + CTBDatabase.Tables.CTB_ROUTES.TABLE_NAME, null);
        List<String> routeIds = new ArrayList<>();

        while (cursor.moveToNext()) {
            @SuppressLint("Range") String routeId = cursor.getString(cursor.getColumnIndex("route"));
            routeIds.add(routeId);
        }
        cursor.close();

        for (String routeId : routeIds) {
            fetchSingleCTBRouteStops(routeId);
        }
    }

    private synchronized void fetchSingleCTBRouteStops(String routeId) {
        Log.d("DataFetcher", "Fetching route stops for " + routeId);

        fetchRouteStops(routeId, "inbound");
        fetchRouteStops(routeId, "outbound");
    }

    private void fetchRouteStops(String routeId, String direction) {
        String url = CTB_BASE_URL + "route-stop/CTB/" + routeId + "/" + direction;
        Log.d("DataFetcher", "Request URL: " + url);
        Request request = new Request.Builder().url(url).build();

        executorService.execute(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e("FetchLog", "Error fetching route stops for " + routeId + " " + response.code());
                    return;
                }
                String jsonData = response.body().string();
                Log.d("DataFetcher", "CTB " + routeId + " " + direction + " passing to processCTBRouteStops");
                processCTBRouteStops(jsonData);
            } catch (IOException | JSONException e) {
                Log.e("DataFetcher", "Error: " + e.getMessage());
            }
        });
    }


    private void processCTBRouteStops(String jsonData) throws JSONException {
        Log.d("DataFetchCTB", "Processing route stops");
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray stopsArray = jsonObject.getJSONArray("data");
        for (int i = 0; i < stopsArray.length(); i++) {
            JSONObject routeStop = stopsArray.getJSONObject(i);
            Log.d("DataFetchCTB", "Adding route stop: " + routeStop.getString("stop") + " " + routeStop.getString("route") + " " + routeStop.getString("dir") + " " + routeStop.getString("seq"));
            databaseHelper.ctbDatabase.updateCTBRouteStops(routeStop.getString("stop"), routeStop.getString("route"), routeStop.getString("dir"), routeStop.getString("seq"));
        }
    }

    public void fetchAllKMBRouteStop(Consumer<String> onSuccess, Consumer<String> onError) {
        Log.d("FetchLog", "Attempt to fetching all kmb route-stop");
        Request request = new Request.Builder()
                .url(KMB_BASE_URL + "route-stop")
                .build();

        Log.d("FetchLog", "Request URL: " + KMB_BASE_URL + "route-stop/");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchLog", "On Failure: Error Fetching all kmb route stops: " + e.getMessage());
                onError.accept("Failed to fetch data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.d("FetchLog", "Successful: " + response.isSuccessful() + " " + response.code());
                Log.e("DataFetcher", "All KMB Route Stop Response: " + response);
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> Log.e("DataFetcheh", "onResponse: Error Fetching all kmb route stops: " + response.code()));
                    onError.accept("Error: " + response.code());
                    response.close();
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        processAllKMBRouteStop(jsonData);
                        onSuccess.accept("Executor: All kmb route-stop fetched successfully, now processing");
                        response.close();
                    } catch (Exception e) {
                        Log.e("DataFetch", "Executor: Error Fetching all kmb route stops: " + e.getMessage());
                        onError.accept("Error processing data: " + e.getMessage());
                        response.close();
                    }
                });
            }
        });

    }

    public void processAllKMBRouteStop(String JSONData) {
        try {
            JSONObject jsonObject = new JSONObject(JSONData);
            JSONArray routeStopArray = jsonObject.getJSONArray("data");

            for (int i = 0; i < routeStopArray.length(); i++) {
                JSONObject routeStop = routeStopArray.getJSONObject(i);
                databaseHelper.kmbDatabase.updateKMBRouteStops(routeStop.getString("stop"), routeStop.getString("route"), routeStop.getString("bound"), routeStop.getString("service_type"), routeStop.getString("seq"));
            }
        } catch (Exception e) {
            Log.e("DataFetchKMB", "Error: Unable to parse and save route-stop info to database:  " + e.getMessage());
        }
    }

    public boolean isGMBStopNumberMatch(Integer stopNumber, String routeID, String routeSeq) {

        String url = GMB_BASE_URL + "route-stop/" + routeID + "/" + routeSeq;

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) { // Synchronous request
            if (!response.isSuccessful()) {
                Log.e("DataFetcher", "Error fetching stops: " + response.code());
                // Return true in case of network errors to avoid unnecessary refetches
                return true;
            }

            String jsonData = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonData);

            if (!jsonObject.has("data")) {
                Log.e("DataFetch", "Invalid API response format");
                return true; // Return true to use existing data
            }

            JSONArray dataArray = jsonObject.getJSONObject("data").getJSONArray("route_stops");

            Log.d("DataFetch", "API reports " + dataArray.length() + " stops, database has " + stopNumber);
            return dataArray.length() == stopNumber;
        } catch (java.net.UnknownHostException | java.net.SocketTimeoutException | java.net.ConnectException e) {
            // These are network connectivity issues
            Log.e("DataFetch", "Network connectivity error: " + e.getMessage());
            // When offline, trust the database and continue
            return true;
        } catch (Exception e) {
            Log.e("DataFetch", "Error fetching stop data: " + e.getMessage());
            // For other errors, better to trust the database than to have no data
            return true;
        }
    }

    public boolean isStopNumberMatch(Integer stopNumber, String route, String bound, String serviceType, String company) {
        String url;

        if (company.equalsIgnoreCase("kmb")) {
            url = KMB_BASE_URL + "route-stop/" + route + "/" + bound + "/" + serviceType;
        } else if (company.equalsIgnoreCase("ctb")) {
            url = CTB_BASE_URL + "route-stop/CTB/" + route + "/" + bound;
        } else {
            Log.e("DataFetch", "ETA for other bus companies is coming soon");
            return false;
        }

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) { // Synchronous request
            if (!response.isSuccessful()) {
                Log.e("DataFetcher", "Error fetching stops: " + response.code());
                // Return true in case of network errors to avoid unnecessary refetches
                return true; 
            }

            String jsonData = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonData);
            
            if (!jsonObject.has("data")) {
                Log.e("DataFetch", "Invalid API response format");
                return true; // Return true to use existing data
            }
            
            JSONArray dataArray = jsonObject.getJSONArray("data");
            
            Log.d("DataFetch", "API reports " + dataArray.length() + " stops, database has " + stopNumber);
            return dataArray.length() == stopNumber;
        } catch (java.net.UnknownHostException | java.net.SocketTimeoutException | java.net.ConnectException e) {
            // These are network connectivity issues
            Log.e("DataFetch", "Network connectivity error: " + e.getMessage());
            // When offline, trust the database and continue
            return true;
        } catch (Exception e) {
            Log.e("DataFetch", "Error fetching stop data: " + e.getMessage());
            // For other errors, better to trust the database than to have no data
            return true;
        }
    }

    public void fetchGMBStopETA(String routeID, String routeSeq, String stopSeq, Consumer<JSONArray> onSuccess, Consumer<String> onError) {
        Log.d("DataFetch", "Fetching GMB stop ETA information");
        Log.d("DataFetch", "Parameters: " + routeID + ", " + routeSeq + ", " + stopSeq);
        String url = GMB_BASE_URL + "eta/route-stop/" + routeID + "/" + routeSeq + "/" + stopSeq;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String errMsg = "Network error: " + e.getMessage();
                Log.e("DataFetch", errMsg);
                mainHandler.post(() -> onError.accept(errMsg));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("DataFetch", "Response: " + response);
                if (!response.isSuccessful()) {
                    String errMsg = "API error: " + response.code();
                    Log.e("DataFetch", errMsg);
                    mainHandler.post(() -> onError.accept(errMsg));
                    return;
                }

                // Ensure the response body is closed after use
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        String errMsg = "Empty response";
                        Log.e("DataFetch", errMsg);
                        mainHandler.post(() -> onError.accept(errMsg));
                        return;
                    }

                    String jsonData = responseBody.string();
                    Log.d("DataFetch", "Data for stopSeq " + stopSeq + ": " + jsonData);

                    try {
                        Log.d("DataFetch", "In try block");
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONObject dataArray = jsonObject.getJSONObject("data");
                        JSONArray etaArray = dataArray.getJSONArray("eta");
                        mainHandler.post(() -> onSuccess.accept(etaArray));
                    } catch (JSONException e) {
                        Log.d("DataFetch", "In JSONEXCEPTION block");
                        String errMsg = "JSON parsing error: " + e.getMessage();
                        Log.e("DataFetch", errMsg);
                        mainHandler.post(() -> onError.accept(errMsg));
                    }
                } catch (Exception e) {
                    Log.d("DataFetch", "In exception e block");
                    String errMsg = "Error processing response: " + e.getMessage();
                    Log.e("DataFetch", errMsg);
                    mainHandler.post(() -> onError.accept(errMsg));
                }
            }
        });


    }


    public void fetchStopETA(String stopID, String route, String serviceType, String company,
                             Consumer<JSONArray> onSuccess, Consumer<String> onError) {
        Log.d("DataFetch", "Fetching stop ETA information for company: " + company);

        // Build URL based on the company
        String url;
        if (company.equals("kmb")) {
            url = KMB_BASE_URL + "eta/" + stopID + "/" + route + "/" + serviceType;
        } else if (company.equals("ctb")) {
            url = CTB_BASE_URL + "eta/CTB/" + stopID + "/" + route;
        } else {
            String msg = "ETA for other bus companies is coming soon";
            Log.e("DataFetch", msg);
            mainHandler.post(() -> onError.accept(msg));
            return;
        }

        Log.d("fetchStopETA", "ETA Request URL for " + company + " " + route + " : " + url);
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String errMsg = "Network error: " + e.getMessage();
                Log.e("DataFetch", errMsg);
                mainHandler.post(() -> onError.accept(errMsg));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("DataFetch", "Response: " + response);
                if (!response.isSuccessful()) {
                    String errMsg = "API error: " + response.code();
                    Log.e("DataFetch", errMsg);
                    mainHandler.post(() -> onError.accept(errMsg));
                    return;
                }

                // Ensure the response body is closed after use
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        String errMsg = "Empty response";
                        Log.e("DataFetch", errMsg);
                        mainHandler.post(() -> onError.accept(errMsg));
                        return;
                    }

                    String jsonData = responseBody.string();
                    Log.d("DataFetch", "Data for stopID " + stopID + ": " + jsonData);

                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        mainHandler.post(() -> onSuccess.accept(dataArray));
                    } catch (JSONException e) {
                        String errMsg = "JSON parsing error: " + e.getMessage();
                        Log.e("DataFetch", errMsg);
                        mainHandler.post(() -> onError.accept(errMsg));
                    }
                } catch (Exception e) {
                    String errMsg = "Error processing response: " + e.getMessage();
                    Log.e("DataFetch", errMsg);
                    mainHandler.post(() -> onError.accept(errMsg));
                }
            }
        });
    }


    private List<BusRoute> parseBusRouteJsonData(String jsonData) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray routesArray = jsonObject.getJSONArray("data");
        List<BusRoute> routes = new ArrayList<>();

        for (int i = 0; i < routesArray.length(); i++) {
            JSONObject routeObject = routesArray.getJSONObject(i);

            String route = routeObject.getString("route");
            String bound = routeObject.getString("bound");
            String serviceType = routeObject.getString("service_type");
            String origEn = routeObject.getString("orig_en");
            String origTc = routeObject.getString("orig_tc");
            String origSc = routeObject.getString("orig_sc");
            String destEn = routeObject.getString("dest_en");
            String destTc = routeObject.getString("dest_tc");
            String destSc = routeObject.getString("dest_sc");

            BusRoute busRoute = new BusRoute(route, "kmb", bound, serviceType,
                    origEn, origTc, origSc,
                    destEn, destTc, destSc);

            databaseHelper.kmbDatabase.updateKMBRoute(route, bound, serviceType, origEn, origTc, origSc, destEn, destTc, destSc);
            routes.add(busRoute);
        }

        return routes;
    }

    public void fetchAllGMBRoutes() {
        fetchAllGMBRoutes(null);
    }
    
    public void fetchAllGMBRoutes(ProgressCallback progressCallback) {
        // Check if operation is already cancelled
        if (isCancelled()) {
            Log.d("DataFetch", "Skipping GMB routes fetch - operation already cancelled");
            if (progressCallback != null) {
                mainHandler.post(() -> progressCallback.onComplete(false, "Operation cancelled"));
            }
            return;
        }
        
        Log.d("DataFetch", "Attempt to fetch all GMB routes");
        String url = GMB_BASE_URL + "route/";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Check if operation was cancelled
                if (isCancelled()) {
                    Log.d("DataFetch", "GMB routes fetch failed, but operation was already cancelled");
                    return;
                }
                
                Log.e("FetchLog", "Error Fetching All GMB Route: " + e.getMessage());
                Log.d("DataFetcher", "Error Fetching All GMB Route: " + e.getMessage());
                
                if (progressCallback != null) {
                    mainHandler.post(() -> {
                        // Check again before updating UI
                        if (!isCancelled()) {
                            progressCallback.onProgressUpdate("Error in Step 4/4: " + e.getMessage());
                            // Signal completion even on failure
                            progressCallback.onComplete(false, "Error fetching GMB routes: " + e.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                // Check if operation was cancelled
                if (isCancelled()) {
                    Log.d("DataFetch", "GMB routes fetch received response, but operation was already cancelled");
                    response.close();
                    return;
                }
                
                Log.e("FetchLog", "All GMB Route Fetch Response: " + response);
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> {
                        // Check again before updating UI
                        if (!isCancelled()) {
                            Log.e("DataFetchGMB", "Error Fetching All GMB Route: " + response.code());
                            if (progressCallback != null) {
                                progressCallback.onProgressUpdate("Error in Step 4/4: HTTP error code " + response.code());
                                // Signal completion even on error response
                                progressCallback.onComplete(false, "Error fetching GMB routes: HTTP error " + response.code());
                            }
                        }
                    });
                    Log.e("FetchLog", "Response is un-successful");
                    response.close();
                    return;
                }

                try {
                    // Get the response body string before closing the response
                    String jsonData = response.body().string();
                    response.close();
                    
                    // Check again before processing data
                    if (isCancelled()) {
                        Log.d("DataFetch", "GMB routes fetch got data, but operation was already cancelled");
                        return;
                    }
                    
                    // Process the data in a separate thread
                    executorService.execute(() -> {
                        // Check again before processing
                        if (isCancelled()) {
                            Log.d("DataFetch", "GMB routes processing skipped - operation was cancelled");
                            return;
                        }
                        
                        try {
                            Log.d("DataFetchGMB", "Data for all GMB routes: " + jsonData);
                            
                            if (progressCallback != null && !isCancelled()) {
                                mainHandler.post(() -> {
                                    if (!isCancelled()) {
                                        progressCallback.onProgressUpdate(
                                                "Step 4/4 in progress: Processing GMB routes data...");
                                    }
                                });
                            }
                            
                            // Process the routes if not cancelled
                            if (!isCancelled()) {
                                processAllGMBRoutes(jsonData);
                            }
                            
                            // Final cancellation check before completion
                            if (progressCallback != null && !isCancelled()) {
                                mainHandler.post(() -> {
                                    if (!isCancelled()) {
                                        progressCallback.onProgressUpdate("Step 4/4 completed: GMB routes fetched successfully");
                                        // Signal successful completion
                                        progressCallback.onComplete(true, "GMB routes fetched successfully");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            // Only report errors if not cancelled
                            if (!isCancelled()) {
                                Log.e("DataFetch", "Error Processing All GMB Route: " + e.getMessage());
                                Log.e("DataFetchGMB", "Error Processing All GMB Route: " + e.getMessage());
                                
                                if (progressCallback != null) {
                                    mainHandler.post(() -> {
                                        if (!isCancelled()) {
                                            progressCallback.onProgressUpdate("Error in Step 4/4: " + e.getMessage());
                                            // Signal completion with error
                                            progressCallback.onComplete(false, "Error processing GMB routes: " + e.getMessage());
                                        }
                                    });
                                }
                            }
                        }
                    });
                } catch (IOException e) {
                    // Only report errors if not cancelled
                    if (!isCancelled()) {
                        Log.e("DataFetch", "Error reading GMB route response: " + e.getMessage());
                        if (progressCallback != null) {
                            mainHandler.post(() -> {
                                if (!isCancelled()) {
                                    progressCallback.onProgressUpdate("Error in Step 4/4: " + e.getMessage());
                                    // Signal completion with error
                                    progressCallback.onComplete(false, "Error reading GMB route response: " + e.getMessage());
                                }
                            });
                        }
                    }
                    response.close();
                }
            }
        });
    }

    public void processAllGMBRoutes(String jsonData) throws JSONException {
        // Check if operation is already cancelled
        if (isCancelled()) {
            Log.d("DataFetchGMB", "Skipping GMB routes processing - operation already cancelled");
            return;
        }
        
        Log.d("DataFetchGMB", "Processing GMB route data");
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONObject data = jsonObject.getJSONObject("data");
        JSONObject routes = data.getJSONObject("routes");

        // Create a counter to track when all route info fetches are complete
        final int routeCount = routes.length();
        final AtomicInteger routeCounter = new AtomicInteger(routeCount);
        
        try {
            // Start a transaction to ensure database consistency
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            db.beginTransaction();
            
            try {
                databaseHelper.gmbDatabase.updateRoutes(routes, (routeNumber, routeRegion) -> {
                    // Check if operation was cancelled before fetching route info
                    if (isCancelled()) {
                        Log.d("DataFetchGMB", "Skipping route info fetch for " + routeNumber + " - operation cancelled");
                        // Still decrement counter to avoid hanging
                        int remaining = routeCounter.decrementAndGet();
                        Log.d("DataFetchGMB", "Counter decremented due to cancellation. Remaining: " + remaining);
                        return;
                    }
                    
                    Log.d("DataFetchGMB", "GMB route data updated successfully, now fetching detail information for the route");
                    fetchGMBRouteInfo(routeNumber, routeRegion, () -> {
                        // Decrement counter when a route info fetch completes
                        int remaining = routeCounter.decrementAndGet();
                        Log.d("DataFetchGMB", "GMB route info fetch completed. Remaining: " + remaining);
                    });
                }, onError -> {
                    if (!isCancelled()) {
                        Log.e("DataFetchGMB", "Error processing GMB route data: " + onError);
                    }
                });
                
                // Mark the transaction as successful
                db.setTransactionSuccessful();
            } finally {
                // End the transaction
                db.endTransaction();
            }
        } catch (Exception e) {
            // Only log errors if not cancelled
            if (!isCancelled()) {
                Log.e("DataFetchGMB", "Database transaction error: " + e.getMessage());
                e.printStackTrace();
                
                // If there's an error with the transaction, still try to process routes individually
                try {
                    for (int i = 0; i < routeCount && !isCancelled(); i++) {
                        String routeKey = routes.names().getString(i);
                        JSONObject routeRegions = routes.getJSONObject(routeKey);
                        
                        for (int j = 0; j < routeRegions.length() && !isCancelled(); j++) {
                            String regionKey = routeRegions.names().getString(j);
                            fetchGMBRouteInfo(routeKey, regionKey, null);
                        }
                    }
                } catch (Exception ex) {
                    Log.e("DataFetchGMB", "Error in fallback route processing: " + ex.getMessage());
                }
            }
        }
    }

    public void fetchGMBRouteInfo(String route, String region) {
        fetchGMBRouteInfo(route, region, null);
    }
    
    public void fetchGMBRouteInfo(String route, String region, Runnable onComplete) {
        // Check if operation is already cancelled
        if (isCancelled()) {
            Log.d("DataFetch", "Skipping GMB route info fetch for " + route + " - operation already cancelled");
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
            return;
        }
        
        Log.d("DataFetch", "Attempt to fetch Route " + route + " in " + region);
        String url = GMB_BASE_URL + "route/" + region + "/" + route;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Check if operation was cancelled
                if (isCancelled()) {
                    Log.d("DataFetch", "GMB route info fetch failed, but operation was already cancelled");
                    if (onComplete != null) {
                        mainHandler.post(onComplete);
                    }
                    return;
                }
                
                Log.e("FetchLog", "Error Fetching GMB Route Info: " + e.getMessage());
                Log.d("DataFetcher", "Error Fetching GMB Route Info: " + e.getMessage());
                
                // Call onComplete even on failure to ensure we don't block progress
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                // Check if operation was cancelled
                if (isCancelled()) {
                    Log.d("DataFetch", "GMB route info fetch received response, but operation was already cancelled");
                    response.close();
                    if (onComplete != null) {
                        mainHandler.post(onComplete);
                    }
                    return;
                }
                
                Log.e("FetchLog", "GMB Route Info Fetch Response: " + response);
                if (!response.isSuccessful()) {
                    if (!isCancelled()) {
                        mainHandler.post(() -> Log.e("DataFetchGMB", "Error Fetching GMB Route Info: " + response.code()));
                    }
                    Log.e("FetchLog", "Response is un-successful");
                    response.close();
                    
                    // Call onComplete even on error to ensure we don't block progress
                    if (onComplete != null) {
                        mainHandler.post(onComplete);
                    }
                    return;
                }

                executorService.execute(() -> {
                    // Check again before processing
                    if (isCancelled()) {
                        Log.d("DataFetch", "GMB route info processing skipped - operation was cancelled");
                        response.close();
                        if (onComplete != null) {
                            mainHandler.post(onComplete);
                        }
                        return;
                    }
                    
                    try {
                        String jsonData = response.body().string();
                        
                        // Check again after reading response body
                        if (isCancelled()) {
                            Log.d("DataFetch", "GMB route info data read, but operation was cancelled");
                            response.close();
                            if (onComplete != null) {
                                mainHandler.post(onComplete);
                            }
                            return;
                        }
                        
                        Log.d("DataFetchGMB", "Data for GMB Route Info: " + jsonData);
                        databaseHelper.gmbDatabase.updateRouteInfo(jsonData, (routeID, routeSeq) -> {
                            response.close();
                            
                            // Call onComplete when processing is done
                            if (onComplete != null) {
                                mainHandler.post(onComplete);
                            }
                        });

                    } catch (Exception e) {
                        if (!isCancelled()) {
                            Log.e("DataFetch", "Error Fetching GMB Route Info: " + e.getMessage());
                            Log.e("DataFetchGMB", "Error Fetching GMB Route Info: " + e.getMessage());
                        }
                        response.close();
                        
                        // Call onComplete even on exception to ensure we don't block progress
                        if (onComplete != null) {
                            mainHandler.post(onComplete);
                        }
                    }
                });
            }
        });
    }

    /**
     * Fetches GMB route stops from the API
     * @param routeID The GMB route ID
     * @param routeSeq The sequence number (1 for inbound, 2 for outbound)
     * @param onSuccess Callback for successful fetch with JSON response
     */
    public void fetchGMBRouteStops(Integer routeID, Integer routeSeq, Consumer<String> onSuccess) {
        String url = GMB_BASE_URL + "route-stop/" + routeID + "/" + routeSeq;
        Request request = new Request.Builder().url(url).build();
        
        Log.d("DataFetchGMB", "Fetching GMB route stops for route ID: " + routeID + ", seq: " + routeSeq);
        Log.d("DataFetchGMB", "Request URL: " + url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchLog", "Error Fetching GMB Route Stop Info: " + e.getMessage());
                Log.d("DataFetcher", "Error Fetching GMB Route Stop Info: " + e.getMessage());
                mainHandler.post(() -> onSuccess.accept(null)); // Callback with null to indicate failure
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.d("FetchLog", "GMB Route Info Fetch Response Code: " + response.code());
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> {
                        Log.e("DataFetchGMB", "Error Fetching GMB Route Stop Info: " + response.code());
                        onSuccess.accept(null); // Callback with null to indicate failure
                    });
                    response.close();
                    return;
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        Log.d("DataFetchGMB", "Data for GMB Route Stop Info received, length: " + jsonData.length());
                        
                        // Update the database with the fetched data
                        databaseHelper.gmbDatabase.updateRouteStops(jsonData, routeID, routeSeq);
                        
                        // Pass the data to the callback
                        mainHandler.post(() -> onSuccess.accept(jsonData));
                        
                    } catch (Exception e) {
                        Log.e("DataFetch", "Error Processing GMB Route Stop Info: " + e.getMessage());
                        Log.e("DataFetchGMB", "Error Processing GMB Route Stop Info: " + e.getMessage());
                        mainHandler.post(() -> onSuccess.accept(null)); // Callback with null to indicate failure
                    } finally {
                        response.close();
                    }
                });
            }
        });
    }

    /**
     * Fetches location data for a GMB stop from the API and updates the database
     * @param stopID The GMB stop ID
     */
    public void fetchGMBStopLocation(String stopID) {
        String url = GMB_BASE_URL + "stop/" + stopID;
        Request request = new Request.Builder().url(url).build();
        
        Log.d("DataFetchGMB", "Fetching GMB stop location for stop ID: " + stopID);
        Log.d("DataFetchGMB", "Request URL: " + url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchLog", "Error Fetching GMB Stop Location: " + e.getMessage());
                Log.d("DataFetcher", "Error Fetching GMB Stop Location: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.d("FetchLog", "GMB Stop Location Fetch Response: " + response.code());
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> Log.e("DataFetchGMB", "Error Fetching GMB Stop Location: " + response.code()));
                    Log.e("FetchLog", "Response is un-successful");
                    response.close();
                    return;
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        Log.d("DataFetchGMB", "Data for GMB Stop Location received, length: " + jsonData.length());
                        
                        // Parse to verify it's a valid response
                        JSONObject jsonObject = new JSONObject(jsonData);
                        if (jsonObject.has("data") && !jsonObject.isNull("data")) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            String lat = data.optString("lat");
                            String lng = data.optString("long");
                            
                            if (lat != null && !lat.isEmpty() && lng != null && !lng.isEmpty()) {
                                Log.d("DataFetchGMB", "Valid location data found for GMB stop " + stopID + 
                                      ": lat=" + lat + ", lng=" + lng);
                            } else {
                                Log.w("DataFetchGMB", "No coordinates in GMB stop data for " + stopID);
                            }
                        }
                        
                        // Update the database even if coordinates are missing
                        databaseHelper.gmbDatabase.updateStopLocation(jsonData, stopID);
                        response.close();
                    } catch (Exception e) {
                        Log.e("DataFetch", "Error Processing GMB Stop Location: " + e.getMessage());
                        Log.e("DataFetchGMB", "Error Processing GMB Stop Location: " + e.getMessage());
                        response.close();
                    }
                });
            }
        });
    }
    
    /**
     * Fetches location data for a GMB stop from the API with callbacks
     * @param stopID The GMB stop ID
     * @param onSuccess Callback for successful fetch with JSON response
     * @param onError Callback for error with error message
     */
    public void fetchGMBStopLocation(String stopID, Consumer<String> onSuccess, Consumer<String> onError) {
        String url = GMB_BASE_URL + "stop/" + stopID;
        Request request = new Request.Builder().url(url).build();
        
        Log.d("DataFetchGMB", "Fetching GMB stop location with callback for stop ID: " + stopID);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchLog", "Error Fetching GMB Stop Location: " + e.getMessage());
                mainHandler.post(() -> onError.accept("Failed to fetch data: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> {
                        Log.e("DataFetchGMB", "Error Fetching GMB Stop Location: " + response.code());
                        onError.accept("Error: " + response.code());
                    });
                    response.close();
                    return;
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        
                        // Update the database
                        databaseHelper.gmbDatabase.updateStopLocation(jsonData, stopID);
                        
                        // Pass the data to the callback
                        mainHandler.post(() -> onSuccess.accept(jsonData));
                        
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            Log.e("DataFetchGMB", "Error Processing GMB Stop Location: " + e.getMessage());
                            onError.accept("Error processing data: " + e.getMessage());
                        });
                    } finally {
                        response.close();
                    }
                });
            }
        });
    }


    public Future<List<RTHKTrafficEntry>> fetchTrafficNews() {
        return executorService.submit(() -> {
            try {
                Document doc = Jsoup.connect(TRAFFIC_NEWS_URL).get();
                Log.d("DataFetchRTHK", "Document fetched successfully");
                return parseDocument(doc);
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }
        });
    }

    private List<RTHKTrafficEntry> parseDocument(Document doc) {
        List<RTHKTrafficEntry> entries = new ArrayList<>();

        // Find all traffic news entries
        Elements newsElements = doc.select("div#content div.articles ul.dec li.inner");

        for (Element newsElement : newsElements) {
            String newsText = newsElement.ownText();
            // Remove the "::marker" text if present
            if (newsText.startsWith("::marker")) {
                newsText = newsText.substring("::marker".length()).trim();
            }

            String date = newsElement.select("div.date").text();

            if (!newsText.isEmpty() && !date.isEmpty()) {
                databaseHelper.updateRTHKNews(newsText, date);
                entries.add(new RTHKTrafficEntry(newsText, date));
            }
        }

        return entries;
    }

    /**
     * Cancels all ongoing operations without releasing resources.
     * This is used when the user cancels the data fetching operation.
     * 
     * @return true if operations were cancelled, false if already cancelled
     */
    public boolean cancelOperations() {
        if (isCancelled.getAndSet(true)) {
            // Already cancelled
            return false;
        }
        
        Log.d("DataFetcher", "Cancelling all operations");
        
        // Cancel all active network requests
        if (client != null && client.dispatcher() != null) {
            client.dispatcher().cancelAll();
            Log.d("DataFetcher", "Canceled all OkHttp requests");
        }
        
        return true;
    }
    
    /**
     * Resets the cancellation state so the DataFetcher can be reused
     */
    public void resetCancellationState() {
        isCancelled.set(false);
        Log.d("DataFetcher", "Cancellation state reset");
    }
    
    /**
     * Checks if the current operation has been cancelled
     * 
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled() {
        return isCancelled.get();
    }

    /**
     * Cancels all active network requests
     * This method is called by DataFetcherCancellation to cancel ongoing operations
     */
    public void cancelAllRequests() {
        Log.d("DataFetcher", "Cancelling all active network requests");
        
        // Cancel all active network requests
        if (client != null && client.dispatcher() != null) {
            client.dispatcher().cancelAll();
            Log.d("DataFetcher", "Canceled all OkHttp requests");
        }
    }
    
    /**
     * Shuts down all active operations and releases resources.
     * Call this method when the activity is being destroyed to prevent memory leaks and
     * background operations continuing after the activity is gone.
     */
    public void shutdown() {
        Log.d("DataFetcher", "Shutting down DataFetcher");
        
        // Set cancelled flag to prevent new operations
        isCancelled.set(true);
        
        // Cancel all active network requests
        if (client != null && client.dispatcher() != null) {
            client.dispatcher().cancelAll();
            Log.d("DataFetcher", "Canceled all OkHttp requests");
        }
        
        // Remove any pending main thread callbacks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            Log.d("DataFetcher", "Removed all pending handler callbacks");
        }
        
        // Shut down the executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Force shutdown including active tasks
            try {
                // Wait a moment for tasks to respond to shutdown
                if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    Log.w("DataFetcher", "Executor did not terminate in the allowed time");
                }
            } catch (InterruptedException e) {
                Log.w("DataFetcher", "Executor shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
            Log.d("DataFetcher", "Executor service shut down");
        }
        
        // Clear context reference to prevent memory leaks
        context = null;
        
        // Close database connections if needed
        // Note: We don't close databaseHelper here because it's managed by the Android system
        // and might be used by other components, but we null the reference
        databaseHelper = null;
        
        Log.d("DataFetcher", "DataFetcher resources fully released");
    }

    /**
     * Backs up the database to external storage
     *
     * @param callback Consumer<Boolean> that will be called with true if backup was successful, false otherwise
     */
    public void backupDatabase(Consumer<Boolean> callback) {
        executorService.execute(() -> {
            boolean success = false;
            try {
                if (context == null) {
                    Log.e("DataFetcher", "Context is null! Cannot backup database.");
                    mainHandler.post(() -> callback.accept(false));
                    return;
                }

                Log.d("DataFetcher", "Backing up database...");

                // Use the correct database name
                File currentDB = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
                Log.d("DataFetcher", "Current database path: " + currentDB.getAbsolutePath());

                if (!currentDB.exists()) {
                    Log.e("DataFetcher", "Database file not found: " + currentDB.getAbsolutePath());
                    mainHandler.post(() -> callback.accept(false));
                    return;
                }

                // Create backup directory
                File backupDir = new File(context.getExternalFilesDir(null), "database_backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                // Create backup file with timestamp
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File backupDB = new File(backupDir, "backup_" + timestamp + "_" + DatabaseHelper.DATABASE_NAME);

                // Perform the backup
                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                Log.d("DataFetcher", "Database backed up successfully to: " + backupDB.getAbsolutePath());
                success = true;
            } catch (Exception e) {
                Log.e("DataFetcher", "Error backing up database: " + e.getMessage(), e);
            }

            final boolean finalSuccess = success;
            mainHandler.post(() -> callback.accept(finalSuccess));
        });
    }


    /**
     * Restores the database from the most recent backup
     *
     * @param callback Consumer<Boolean> that will be called with true if restore was successful, false otherwise
     */
    public void restoreDatabase(Consumer<Boolean> callback) {
        executorService.execute(() -> {
            boolean success = false;
            try {
                Log.d("DataFetcher", "Restoring database...");
                File backupDir = new File(context.getExternalFilesDir(null), "database_backups");
                if (!backupDir.exists() || backupDir.listFiles() == null || backupDir.listFiles().length == 0) {
                    Log.e("DataFetcher", "No backup files found");
                    mainHandler.post(() -> callback.accept(false));
                    return;
                }

                // Find the most recent backup
                File[] backups = backupDir.listFiles((dir, name) -> name.endsWith(DatabaseHelper.DATABASE_NAME));
                if (backups == null || backups.length == 0) {
                    Log.e("DataFetcher", "No valid backup files found");
                    mainHandler.post(() -> callback.accept(false));
                    return;
                }

                File mostRecentBackup = backups[0];
                for (File backup : backups) {
                    if (backup.lastModified() > mostRecentBackup.lastModified()) {
                        mostRecentBackup = backup;
                    }
                }

                File currentDB = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);

                // Close the database connection before restoring
                databaseHelper.close();

                FileChannel src = new FileInputStream(mostRecentBackup).getChannel();
                FileChannel dst = new FileOutputStream(currentDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                // Reopen the database
                databaseHelper = DatabaseHelper.getInstance(context);

                Log.d("DataFetcher", "Database restored successfully from " + mostRecentBackup.getAbsolutePath());
                success = true;
            } catch (Exception e) {
                Log.e("DataFetcher", "Error restoring database: " + e.getMessage());
                e.printStackTrace();
            }

            final boolean finalSuccess = success;
            mainHandler.post(() -> callback.accept(finalSuccess));
        });
    }

    public void deleteOldBackups() {
        File backupDir = new File(context.getExternalFilesDir(null), "database_backups");
        if (backupDir.exists()) {
            File[] files = backupDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        Log.d("DataFetcher", "Deleted backup: " + file.getAbsolutePath());
                    } else {
                        Log.e("DataFetcher", "Failed to delete: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

}