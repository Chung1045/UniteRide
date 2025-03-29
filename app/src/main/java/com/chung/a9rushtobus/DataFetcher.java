package com.chung.a9rushtobus;

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
    private static final String KMB_BASE_URL = "https://data.etabus.gov.hk/v1/transport/kmb/";
    private static final String CTB_BASE_URL = "https://rt.data.gov.hk/v2/transport/citybus/";
    private static final String TRAFFIC_NEWS_URL = "https://programme.rthk.hk/channel/radio/trafficnews/index.php";
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client;
    private DatabaseHelper databaseHelper;
    private Context context;

    public DataFetcher(Context context) {
        databaseHelper = new DatabaseHelper(context);
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

    public void refreshAllData() {
        backupDatabase(success -> {
            if (success) {
                Log.d("DataFetcher", "Database backup successful");
                databaseHelper.removeAllValues();

                // Total number of fetch operations
                final int totalTasks = 4;
                final AtomicInteger tasksPending = new AtomicInteger(totalTasks);
                final AtomicBoolean errorOccurred = new AtomicBoolean(false);

                // Helper method to check if all tasks have completed
                Runnable checkCompletion = () -> {
                    if (tasksPending.decrementAndGet() == 0) {
                        // All tasks are complete; log and show a toast message on the main thread
                        if (errorOccurred.get()) {
                            Log.e("DataFetcher", "Data fetch completed with errors");
                            mainHandler.post(() -> Toast.makeText(context, "Data fetch completed with errors", Toast.LENGTH_LONG).show());
                        } else {
                            Log.d("DataFetcher", "All data fetched successfully");
                            mainHandler.post(() -> Toast.makeText(context, "All data fetched successfully", Toast.LENGTH_LONG).show());
                        }
                    }
                };

                // Fetch all bus routes
                fetchAllBusRoutes(
                        routes -> {
                            Log.d("DataFetcher", "All bus routes fetched successfully");
                            checkCompletion.run();
                        },
                        error -> {
                            if (!errorOccurred.get()) {
                                errorOccurred.set(true);
                                Log.e("DataFetcher", "Error fetching bus routes: " + error);
                                handleDataFetchFailure();
                            }
                            checkCompletion.run();
                        }
                );

                // Fetch all KMB bus stops
                fetchAllKMBBusStop(
                        message -> {
                            Log.d("DataFetcher", "All KMB bus stops fetched successfully, now processing");
                            checkCompletion.run();
                        },
                        error -> {
                            if (!errorOccurred.get()) {
                                errorOccurred.set(true);
                                Log.e("DataFetcher", "Error fetching KMB bus stops: " + error);
                                handleDataFetchFailure();
                            }
                            checkCompletion.run();
                        }
                );

                // Fetch all KMB route-stop data
                fetchAllKMBRouteStop(
                        message -> {
                            Log.d("DataFetcher", "All KMB route-stop fetched successfully, now processing");
                            checkCompletion.run();
                        },
                        error -> {
                            if (!errorOccurred.get()) {
                                errorOccurred.set(true);
                                Log.e("DataFetcher", "Error fetching KMB route-stop: " + error);
                                handleDataFetchFailure();
                            }
                            checkCompletion.run();
                        }
                );

                // Fetch all CTB routes (which internally triggers fetching of CTB route stops and stops)
                fetchAllCTBRoutes(
                        message -> {
                            Log.d("DataFetcher", "All CTB routes fetched successfully");
                            checkCompletion.run();
                        },
                        error -> {
                            if (!errorOccurred.get()) {
                                errorOccurred.set(true);
                                Log.e("DataFetcher", "Error fetching CTB routes: " + error);
                                // Optionally, you could call handleDataFetchFailure() here as well
                            }
                            checkCompletion.run();
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

    public void fetchAllKMBBusStop(Consumer<String> onSuccess, Consumer<String> onError) {
        Request request = new Request.Builder()
                .url(KMB_BASE_URL + "stop")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchLog", "Error Fetching all kmb bus stops: " + e.getMessage());
                Log.d("DataFetcher", "Error Fetching all kmb bus stops: " + e.getMessage());
                onError.accept("Failed to fetch data: " + e.getMessage());
                return;
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.e("FetchLog", "All KMB Stop Response: " + response);
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> Log.e("DataFetchehKMB", "Error Fetching all kmb bus stops: " + response.code()));
                    Log.e("FetchLog", "Response is un-successful");
                    onError.accept("Error: " + response.code());
                    return;
                }

                executorService.execute(() -> {
                    try {
                        String jsonData = response.body().string();
                        processAllKMBBusStop(jsonData);
                        onSuccess.accept("All kmb bus stops fetched successfully, now processing");
                    } catch (Exception e) {
                        Log.e("DataFetch", "Error Fetching all kmb bus stops: " + e.getMessage());
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
                databaseHelper.kmbDatabase.updateKMBStop(stop.getString("stop"), stop.getString("name_en"), stop.getString("name_tc"), stop.getString("name_sc"), stop.getString("lat"), stop.getString("long"));
            }

        } catch (Exception e) {
            Log.e("DataFetchKMBSTOP", "Error: Unable to parse and save stop info to database:  " + e.getMessage());
        }
    }

    private AtomicBoolean ctbStopsFetched = new AtomicBoolean(false);
    private static final int BATCH_SIZE = 100;

    private void fetchAllCTBStops() {
        // Prevent duplicate fetching if this method is called multiple times
        if (ctbStopsFetched.get()) {
            Log.d("FetchLog", "CTB stops already fetched; skipping duplicate call.");
            return;
        }
        ctbStopsFetched.set(true);

        Log.d("FetchLog", "Starting fetchAllCTBStops");
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT stop_id FROM ctb_route_stops", null);
        List<String> stopIds = new ArrayList<>();

        while (cursor.moveToNext()) {
            @SuppressLint("Range") String stopId = cursor.getString(cursor.getColumnIndex("stop_id"));
            stopIds.add(stopId);
        }
        cursor.close();

        Log.d("FetchLog", "Total stops to fetch: " + stopIds.size());

        // Process stops in batches
        for (int i = 0; i < stopIds.size(); i += BATCH_SIZE) {
            List<String> batch = stopIds.subList(i, Math.min(i + BATCH_SIZE, stopIds.size()));
            Log.d("FetchLog", "Processing Stop batch " + (i / BATCH_SIZE + 1) +
                    " (stops " + i + " to " + Math.min(i + BATCH_SIZE, stopIds.size()) + ")");
            processCTBStopBatch(batch);
        }
    }

    private void processCTBStopBatch(List<String> stopIds) {
        // For each stop in the batch, perform an asynchronous network call.
        // This avoids holding up a background thread with synchronous calls.
        for (String stopId : stopIds) {
            fetchAndProcessSingleCTBStop(stopId);
        }
    }

    private void fetchAndProcessSingleCTBStop(String stopId) {
        String url = CTB_BASE_URL + "stop/" + stopId;
        Request request = new Request.Builder().url(url).build();
        Log.d("FetchLog", "Making request for stop " + stopId + " to URL: " + url);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FetchLog", "Error fetching stop " + stopId + " - " + e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("FetchLog", "Error fetching stop " + stopId + " - " + response.code());
                    return;
                }
                String jsonData = response.body().string();
                Log.d("FetchLog", "Received data for stop " + stopId +
                        ": " + jsonData.substring(0, Math.min(100, jsonData.length())) + "...");
                try {
                    processCTBStop(jsonData);
                } catch (JSONException e) {
                    Log.e("FetchLog", "Error processing stop " + stopId, e);
                }
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
            fetchSingleCTBRouteStops(routeId, "inbound");
            fetchSingleCTBRouteStops(routeId, "outbound");
        }
    }

    private void fetchSingleCTBRouteStops(String routeId, String bound) {
        String url = CTB_BASE_URL + "route-stop/CTB/" + routeId + "/" + bound;
        Request request = new Request.Builder().url(url).build();

        executorService.execute(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e("FetchLog", "Error fetching route stops for " + routeId + " " + response.code());
                    Log.e("DataFetcher", "Error fetching route stops for " + routeId);
                    return;
                }
                String jsonData = response.body().string();
                processCTBRouteStops(jsonData);
            } catch (IOException | JSONException e) {
                Log.e("DataFetcher", "Error: " + e.getMessage());
            }
        });
    }

    private void processCTBRouteStops(String jsonData) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray stopsArray = jsonObject.getJSONArray("data");
        for (int i = 0; i < stopsArray.length(); i++) {
            JSONObject routeStop = stopsArray.getJSONObject(i);
            databaseHelper.ctbDatabase.updateCTBRouteStops(routeStop.getString("stop"), routeStop.getString("route"), routeStop.getString("dir"), routeStop.getString("seq"));
        }
        fetchAllCTBStops();
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
                databaseHelper.kmbDatabase.updateKMBRouteStops(routeStop.getString("stop"), routeStop.getString("route"), routeStop.getString("bound"), routeStop.getString("service_type"), routeStop.getString("seq"));
            }
        } catch (Exception e) {
            Log.e("DataFetchKMB", "Error: Unable to parse and save route-stop info to database:  " + e.getMessage());
        }
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

        Log.d("fetchStopETA", "Request URL: " + url);
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