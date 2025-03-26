package com.chung.a9rushtobus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.chung.a9rushtobus.elements.BusRoute;
import com.chung.a9rushtobus.elements.BusRouteStopItem;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataFetcher {
    private static final String KMB_BASE_URL = "https://data.etabus.gov.hk/v1/transport/kmb/";
    private static final String BUS_ROUTES_URL = "https://data.etabus.gov.hk/v1/transport/kmb/route";
    private static final String TRAFFIC_NEWS_URL = "https://programme.rthk.hk/channel/radio/trafficnews/index.php";
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    private DatabaseHelper databaseHelper;
    private Context context;

    public DataFetcher(Context context) {
        this.executorService = Executors.newCachedThreadPool();
        databaseHelper = new DatabaseHelper(context);
        this.context = context;
    }

    public void refreshAllData() {
        backupDatabase(success -> {
            if (success) {
                Log.d("DataFetcher", "Database backup successful");
                databaseHelper.removeAllValues();

                // Flag to track whether any operation fails
                AtomicBoolean failed = new AtomicBoolean(false);

                // Fetch KM Bus Stops
                Log.d("DataFetcher", "1. Fetching all kmb bus stops");
                fetchAllKMBBusStop(
                        onSuccess -> Log.d("DataFetcher", "All kmb bus stops fetched successfully, now processing"),
                        error -> {
                            if (!failed.get()) {
                                failed.set(true);
                                Log.e("DataFetcher", "Error fetching all kmb bus stops: " + error);
                                handleDataFetchFailure();
                            }
                        }
                );

                // Fetch Bus Routes
                Log.d("DataFetcher", "2. Fetching all bus routes");
                fetchAllBusRoutes(
                        onSuccess -> {
                            if (!failed.get()) {
                                Log.d("DataFetcher", "All bus routes fetched successfully");
                            }
                        },
                        error -> {
                            if (!failed.get()) {
                                failed.set(true);
                                Log.e("DataFetcher", "2. Error fetching all bus routes: " + error);
                                handleDataFetchFailure();
                            }
                        }
                );

                // Fetch KMB Route Stops
                Log.d("DataFetcher", "3. Fetching all kmb route-stop");
                fetchAllKMBRouteStop(
                        onSuccess -> {
                            if (!failed.get()) {
                                Log.d("DataFetcher", "All kmb route-stop fetched successfully, now processing");
                            }
                        },
                        error -> {
                            if (!failed.get()) {
                                failed.set(true);
                                Log.e("DataFetcher", "Error fetching all kmb route-stop: " + error);
                                handleDataFetchFailure();
                            }
                        }
                );
            } else {
                Log.e("DataFetcher", "Database backup failed");
                Toast.makeText(context, "Database backup failed. Please try again later.", Toast.LENGTH_SHORT).show();
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

    public void fetchAllKMBBusStop(Consumer<String> onSuccess, Consumer<String> onError) {
        Request request = new Request.Builder()
                .url(KMB_BASE_URL + "stop")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DataFetchKMB", "Error Fetching all kmb bus stops: " + e.getMessage());
                onError.accept("Failed to fetch data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> Log.e("DataFetchehKMB", "Error Fetching all kmb bus stops: " + response.code()));
                    onError.accept("Error: " + response.code());
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        processAllKMBBusStop(jsonData);
                        onSuccess.accept("All kmb bus stops fetched successfully, now processing");
                    } catch (Exception e) {
                        Log.e("DataFetchKMB", "Error Fetching all kmb bus stops: " + e.getMessage());
                        onError.accept("Error processing data: " + e.getMessage());
                    }
                });
            }
        });
    }

    public void processAllKMBBusStop(String JSONData) {
        try {
            JSONObject jsonObject = new JSONObject(JSONData);
            JSONArray stopsArray = jsonObject.getJSONArray("data");

            for (int i = 0; i < stopsArray.length(); i++) {
                JSONObject stop = stopsArray.getJSONObject(i);
                Log.d("DataFetchKMBSTOP", "Adding stop: " + stop.getString("stop"));
                databaseHelper.updateKMBStop(stop.getString("stop"), stop.getString("name_en"), stop.getString("name_tc"), stop.getString("name_sc"), stop.getString("lat"), stop.getString("long"));
            }

        } catch (Exception e) {
            Log.e("DataFetchKMBSTOP", "Error: Unable to parse and save stop info to database:  " + e.getMessage());
        }
    }

    public void fetchAllKMBRouteStop(Consumer<String> onSuccess, Consumer<String> onError) {
        Log.d("DataFetch", "Attempt to fetching all kmb route-stop");
        Request request = new Request.Builder()
                .url(KMB_BASE_URL + "/route-stop")
                .build();

        Log.d("DataFetch", "Request URL: " + KMB_BASE_URL + "route-stop/");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DataFetch", "On Failure: Error Fetching all kmb route stops: " + e.getMessage());
                onError.accept("Failed to fetch data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.d("DataFetch", "Successful: " + response.isSuccessful());
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> Log.e("DataFetcheh", "onResponse: Error Fetching all kmb route stops: " + response.code()));
                    Log.e("DataFetch", "Response: " + response);
                    onError.accept("Error: " + response.code());
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        processAllKMBRouteStop(jsonData);
                        onSuccess.accept("Executor: All kmb route-stop fetched successfully, now processing");
                    } catch (Exception e) {
                        Log.e("DataFetch", "Executor: Error Fetching all kmb route stops: " + e.getMessage());
                        onError.accept("Error processing data: " + e.getMessage());
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
                databaseHelper.updateKMBRouteStops(routeStop.getString("stop"), routeStop.getString("route"), routeStop.getString("bound"), routeStop.getString("service_type"), routeStop.getString("seq"));
            }
        } catch (Exception e) {
            Log.e("DataFetchKMB", "Error: Unable to parse and save route-stop info to database:  " + e.getMessage());
        }
    }

    public void fetchRouteStopInfo(String route, String company, String bound, String serviceType,
                                   Consumer<JSONArray> onSuccess, Consumer<String> onError) {
        Log.d("DataFetchKMB", "Fetching route stop information");
        Request request = new Request.Builder()
                .url(KMB_BASE_URL + "route-stop/" + route + "/" + bound + "/" + serviceType)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> onError.accept("Failed to fetch data: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> onError.accept("Error: " + response.code()));
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        JSONArray stopInfoArray = processRouteStopInfo(jsonData);
                        mainHandler.post(() -> onSuccess.accept(stopInfoArray));
                    } catch (Exception e) {
                        mainHandler.post(() -> onError.accept("Error processing data: " + e.getMessage()));
                    }
                });
            }
        });
    }

    private JSONArray processRouteStopInfo(String jsonData) {
        JSONArray returnArray = new JSONArray();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray stopIDsArray = jsonObject.getJSONArray("data");

            for (int i = 0; i < stopIDsArray.length(); i++) {
                JSONObject stopObject = stopIDsArray.getJSONObject(i);
                String stopID = stopObject.getString("stop");
                JSONObject stopName = fetchStopName(stopID);

                // Create a new object with all information
                JSONObject stopInfo = new JSONObject();
                stopInfo.put("seq", i + 1);
                stopInfo.put("stop_id", stopID);

                // Add data from original stop object
                for (Iterator<String> it = stopObject.keys(); it.hasNext(); ) {
                    String key = it.next();
                    if (!key.equals("stop")) { // Already added as stop_id
                        stopInfo.put(key, stopObject.get(key));
                    }
                }

                // Add name and location data
                if (stopName != null) {
                    stopInfo.put("name_en", stopName.getString("name_en"));
                    stopInfo.put("name_tc", stopName.getString("name_tc"));
                    stopInfo.put("name_sc", stopName.getString("name_sc"));
                    stopInfo.put("stopID", stopName.getString("stop"));
                    stopInfo.put("lat", stopName.getString("lat"));
                    stopInfo.put("long", stopName.getString("long"));
                }

                // Add to return array
                returnArray.put(stopInfo);

                // Log for debugging
                Log.d("DataFetchKMB", "Added stop: " + stopID);
            }

        } catch (Exception e) {
            Log.e("DataFetchKMB", "Error: " + e.getMessage());
        }

        return returnArray;
    }

    private JSONObject fetchStopName(String stopID) {
        Request request = new Request.Builder()
                .url(KMB_BASE_URL + "stop/" + stopID)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e("DataFetchKMB", "Error: " + response.code());
                return null;
            }

            String jsonData = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonData);
            return jsonObject.getJSONObject("data");

        } catch (Exception e) {
            Log.e("DataFetchKMB", "Error fetching stop name: " + e.getMessage());
            return null;
        }
    }

    public void fetchStopETA(String stopID, String route, String serviceType, String company,
                             Consumer<JSONArray> onSuccess, Consumer<String> onError) {
        Log.d("DataFetchKMB", "Fetching stop ETA information");
        if (company.equals("kmb")) {
            String url = KMB_BASE_URL + "eta/" + stopID + "/" + route + "/" + serviceType;
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Log.d("fetchStopETA", "Request URL: " + url);

            // Use enqueue for asynchronous network call instead of execute
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("DataFetchKMB", "In catch block");
                    Log.e("DataFetchKMB", "Error: " + e.getMessage());
                    mainHandler.post(() -> onError.accept("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            Log.d("DataFetchKMB", "Fetch unsuccessful");
                            Log.e("DataFetchKMB", "Error: " + response.code());
                            mainHandler.post(() -> onError.accept("API error: " + response.code()));
                            return;
                        }

                        if (response.body() != null) {
                            String jsonData = response.body().string();
                            Log.d("DataFetchKMBETA_" + route, "DATA for stopID " + stopID + "\n" + jsonData);

                            try {
                                JSONObject jsonObject = new JSONObject(jsonData);
                                JSONArray dataArray = jsonObject.getJSONArray("data");
                                mainHandler.post(() -> onSuccess.accept(dataArray));
                            } catch (JSONException e) {
                                Log.e("DataFetchKMB", "JSON parsing error: " + e.getMessage());
                                mainHandler.post(() -> onError.accept("JSON parsing error: " + e.getMessage()));
                            }
                        } else {
                            Log.e("DataFetchKMB", "Response body is null");
                            mainHandler.post(() -> onError.accept("Empty response"));
                        }
                    } catch (Exception e) {
                        Log.d("DataFetchKMB", "In catch block");
                        Log.e("DataFetchKMB", "Error processing response: " + e.getMessage());
                        mainHandler.post(() -> onError.accept("Error processing response: " + e.getMessage()));
                    }
                }
            });
        } else {
            Log.e("DataFetchKMB", "Error: " + "ETA for other bus company is coming soon");
            mainHandler.post(() -> onError.accept("ETA for other bus company is coming soon"));
        }
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

            databaseHelper.updateKMBRoute(route, bound, serviceType, origEn, origTc, origSc, destEn, destTc, destSc);
            routes.add(busRoute);
        }

        return routes;
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

    public void shutdown() {
        executorService.shutdown();
    }
    
    /**
     * Backs up the database to external storage
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
                databaseHelper = new DatabaseHelper(context);
                
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