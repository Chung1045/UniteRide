package com.chung.a9rushtobus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataFetcher {
    private static final String KMB_BASE_URL = "https://data.etabus.gov.hk/v1/transport/kmb/";
    private static final String BUS_ROUTES_URL = "https://data.etabus.gov.hk/v1/transport/kmb/route/";
    private static final String TRAFFIC_NEWS_URL = "https://programme.rthk.hk/channel/radio/trafficnews/index.php";
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();

    public DataFetcher(){
        this.executorService = Executors.newCachedThreadPool();
    }

    public void fetchAllBusRoutes(Consumer<List<BusRoute>> onSuccess, Consumer<String> onError) {
        Request request = new Request.Builder()
                .url(BUS_ROUTES_URL)
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

    public void fetchStopEtaInfo(){
        Log.d("DataFetchKMB", "Fetching stop ETA information");
        Request request = new Request.Builder()
                .url(KMB_BASE_URL + "eta/A60AE774B09A5E44/40/1")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DataFetchKMB", "Failed to fetch data: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("DataFetchKMB", "Error: " + response.code());
                }

                try {
                    String jsonData = response.body().string();
                    Log.d("DataFetchKMB", jsonData);

                } catch (Exception e) {
                    Log.e("DataFetchKMB", "Error parsing JSON: " + e.getMessage());
                }
            }
        });
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
                for (Iterator<String> it = stopObject.keys(); it.hasNext();) {
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
    
    // For backward compatibility
    public void fetchStopETA(String stopID, String route, String serviceType, String company) {
        fetchStopETA(stopID, route, serviceType, company, 
            data -> Log.d("DataFetchKMB", "ETA data received but no handler provided: " + data.length() + " items"), 
            error -> Log.e("DataFetchKMB", "ETA error: " + error));
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

            BusRoute busRoute = new BusRoute(route, bound, serviceType,
                    origEn, origTc, origSc,
                    destEn, destTc, destSc);
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
                entries.add(new RTHKTrafficEntry(newsText, date));
            }
        }

        return entries;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}