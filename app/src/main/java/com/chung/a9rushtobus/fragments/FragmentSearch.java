package com.chung.a9rushtobus.fragments;

import android.content.Context;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.chung.a9rushtobus.DataFetcher;
import com.chung.a9rushtobus.database.CTBDatabase;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.database.GMBDatabase;
import com.chung.a9rushtobus.database.KMBDatabase;
import com.chung.a9rushtobus.elements.BusRoute;
import com.chung.a9rushtobus.elements.BusRouteAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragmentSearch extends Fragment {

    private DataFetcher dataFetcher;
    private DatabaseHelper databaseHelper;
    private BusRouteAdapter adapter;
    private List<BusRoute> busRoutes;
    private List<BusRoute> allRoutes;
    private ProgressBar progressBar;
    private EditText searchEditText;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataFetcher = new DataFetcher(getContext());
        databaseHelper = new DatabaseHelper(requireContext());
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        searchEditText = view.findViewById(R.id.search_edit_text);

        busRoutes = new ArrayList<>();
        allRoutes = new ArrayList<>();
        adapter = new BusRouteAdapter(requireContext(), busRoutes);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        listenerInit();
        busRoutes.addAll(loadCachedRoutes());
        adapter.notifyDataSetChanged();
//        fetchBusRoutes();

        // Set up touch listener for the root view to hide keyboard when tapping outside
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboardOnOutsideTouch(event);
            }
            return false;
        });
    }

    private void listenerInit() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRoutes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private List<BusRoute> loadCachedRoutes() {
        List<BusRoute> routes = new ArrayList<>();
        Log.d("FragmentSearch", "Loading cached routes...");
        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();

            String[] projection = {
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE,
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND,
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE,
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN,
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_TC,
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_SC,
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_EN,
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_TC,
                    KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_SC
            };

            Cursor cursor = db.query(
                    KMBDatabase.Tables.KMB_ROUTES.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    null // No initial sorting
            );

            while (cursor.moveToNext()) {
                String route = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ROUTE));
                String bound = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_BOUND));
                String serviceType = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE));
                String originEn = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN));
                String originTc = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_TC));
                String originSc = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_ORIGIN_SC));
                String destEn = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_EN));
                String destTc = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_TC));
                String destSc = cursor.getString(cursor.getColumnIndex(KMBDatabase.Tables.KMB_ROUTES.COLUMN_DEST_SC));

                routes.add(new BusRoute(route, "kmb", bound, serviceType, originEn, originTc, originSc, destEn, destTc, destSc));
                allRoutes.add(new BusRoute(route, "kmb", bound, serviceType, originEn, originTc, originSc, destEn, destTc, destSc));
            }
            cursor.close();

            String[] ctbRoutesProjection = {
                    CTBDatabase.Tables.CTB_ROUTES.COLUMN_ROUTE,
                    CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_EN,
                    CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_TC,
                    CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_SC,
                    CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_EN,
                    CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_TC,
                    CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_SC
            };

            Cursor cursor2 = db.query(
                    CTBDatabase.Tables.CTB_ROUTES.TABLE_NAME,
                    ctbRoutesProjection,
                    null,
                    null,
                    null,
                    null,
                    null // No initial sorting
            );

            while (cursor2.moveToNext()) {
                String route = cursor2.getString(cursor2.getColumnIndex(CTBDatabase.Tables.CTB_ROUTES.COLUMN_ROUTE));
                String originEn = cursor2.getString(cursor2.getColumnIndex(CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_EN));
                String originTc = cursor2.getString(cursor2.getColumnIndex(CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_TC));
                String originSc = cursor2.getString(cursor2.getColumnIndex(CTBDatabase.Tables.CTB_ROUTES.COLUMN_ORIGIN_SC));
                String destEn = cursor2.getString(cursor2.getColumnIndex(CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_EN));
                String destTc = cursor2.getString(cursor2.getColumnIndex(CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_TC));
                String destSc = cursor2.getString(cursor2.getColumnIndex(CTBDatabase.Tables.CTB_ROUTES.COLUMN_DEST_SC));

                routes.add(new BusRoute(route, "ctb", "outbound", null, originEn, originTc, originSc, destEn, destTc, destSc));
                allRoutes.add(new BusRoute(route, "ctb", "outbound", null, originEn, originTc, originSc, destEn, destTc, destSc));

                // Flipped for inbound directions
                routes.add(new BusRoute(route, "ctb", "inbound", null, destEn, destTc, destSc, originEn, originTc, originSc));
                allRoutes.add(new BusRoute(route, "ctb", "inbound", null, destEn, destTc, destSc, originEn, originTc, originSc));
            }

            // Use rawQuery instead of query to have more control over the SQL
            Cursor cursor3 = db.rawQuery(
                "SELECT ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ID + ", " +
                "r." + GMBDatabase.Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_SEQ + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_EN + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_TC + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_SC + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_EN + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_TC + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_SC + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_EN + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_TC + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_SC + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_REGION + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_DESCRIPTION_EN + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_DESCRIPTION_TC + ", " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_DESCRIPTION_SC + " " +
                "FROM " + GMBDatabase.Tables.GMB_ROUTES_INFO.TABLE_NAME + " ri " +
                "JOIN " + GMBDatabase.Tables.GMB_ROUTES.TABLE_NAME + " r ON " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_NUMBER + " = " +
                "r." + GMBDatabase.Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER + " AND " +
                "ri." + GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_REGION + " = " +
                "r." + GMBDatabase.Tables.GMB_ROUTES.COLUMN_ROUTE_REGION, 
                null
            );

            if (cursor3.getCount() > 0) {
                Log.d("GMB", "Found " + cursor3.getCount() + " GMB routes");
                while (cursor3.moveToNext()) {
                    // Get column indices first to avoid repeated lookups
                    int routeIdIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ID);
                    int routeNumberIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES.COLUMN_ROUTE_NUMBER);
                    int routeSeqIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_SEQ);
                    int originEnIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_EN);
                    int originTcIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_TC);
                    int originScIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_ORIGIN_SC);
                    int destEnIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_EN);
                    int destTcIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_TC);
                    int destScIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_DEST_SC);
                    int regionIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_ROUTE_REGION);
                    int remarksEnIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_EN);
                    int remarksTcIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_TC);
                    int remarksScIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_REMARKS_SC);
                    int descriptionEnIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_DESCRIPTION_EN);
                    int descriptionTcIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_DESCRIPTION_TC);
                    int descriptionScIndex = cursor3.getColumnIndex(GMBDatabase.Tables.GMB_ROUTES_INFO.COLUMN_DESCRIPTION_SC);
                    
                    // Log all column names for debugging
                    for (int i = 0; i < cursor3.getColumnCount(); i++) {
                        Log.d("GMB", "Column " + i + ": " + cursor3.getColumnName(i));
                    }
                    
                    // Safely get values, using empty string as fallback if column not found
                    String routeId = routeIdIndex >= 0? cursor3.getString(routeIdIndex) : "";
                    String route = routeNumberIndex >= 0 ? cursor3.getString(routeNumberIndex) : "";
                    String routeSeq = routeSeqIndex >= 0? cursor3.getString(routeSeqIndex) : "";
                    String originEn = originEnIndex >= 0 ? cursor3.getString(originEnIndex) : "";
                    String originTc = originTcIndex >= 0 ? cursor3.getString(originTcIndex) : "";
                    String originSc = originScIndex >= 0 ? cursor3.getString(originScIndex) : "";
                    String destEn = destEnIndex >= 0 ? cursor3.getString(destEnIndex) : "";
                    String destTc = destTcIndex >= 0 ? cursor3.getString(destTcIndex) : "";
                    String destSc = destScIndex >= 0 ? cursor3.getString(destScIndex) : "";
                    String region = regionIndex >= 0 ? cursor3.getString(regionIndex) : "";
                    String remarksEn = remarksEnIndex >= 0 ? cursor3.getString(remarksEnIndex) : "";
                    String remarksTc = remarksTcIndex >= 0 ? cursor3.getString(remarksTcIndex) : "";
                    String remarksSc = remarksScIndex >= 0 ? cursor3.getString(remarksScIndex) : "";
                    String descriptionEn = descriptionEnIndex >= 0 ? cursor3.getString(descriptionEnIndex) : "";
                    String descriptionTc = descriptionTcIndex >= 0 ? cursor3.getString(descriptionTcIndex) : "";
                    String descriptionSc = descriptionScIndex >= 0 ? cursor3.getString(descriptionScIndex) : "";
                    
                    Log.d("GMB", "route: " + route + " origin: " + originEn + " dest: " + destEn + " remarks: " + remarksEn + " region: " + region + " routeSeq: " + routeSeq);

                    // Correctly create GMB BusRoute objects
                    BusRoute gmbRoute = new BusRoute(
                            routeId, route, region, routeSeq,
                            originEn, originTc, originSc,
                            destEn, destTc, destSc,
                            remarksEn, remarksTc, remarksSc, descriptionEn, descriptionTc, descriptionSc
                    );

                    // Add to both collections
                    routes.add(gmbRoute);
                    allRoutes.add(gmbRoute);
                }
                cursor3.close();
            } else {
                Log.d("GMB", "No GMB routes found in database");
            }

            // Sort routes using a safer comparator that sorts by route number first, then by company
            try {
                Comparator<BusRoute> routeComparator = new Comparator<BusRoute>() {
                    @Override
                    public int compare(BusRoute route1, BusRoute route2) {
                        if (route1 == null && route2 == null) {
                            return 0;
                        } else if (route1 == null) {
                            return -1;
                        } else if (route2 == null) {
                            return 1;
                        }
                        
                        try {
                            // First compare by route number
                            int routeCompare = route1.compareRouteNumber(route2);
                            
                            // If route numbers are the same, compare by company as a secondary sort
                            if (routeCompare == 0) {
                                String company1 = route1.getCompany() != null ? route1.getCompany() : "";
                                String company2 = route2.getCompany() != null ? route2.getCompany() : "";
                                
                                // If companies are the same, compare by bound/direction
                                int companyCompare = company1.compareTo(company2);
                                if (companyCompare == 0) {
                                    String bound1 = route1.getBound() != null ? route1.getBound() : "";
                                    String bound2 = route2.getBound() != null ? route2.getBound() : "";
                                    return bound1.compareTo(bound2);
                                }
                                return companyCompare;
                            }
                            
                            return routeCompare;
                        } catch (Exception e) {
                            // If comparison fails, fall back to string comparison of routes
                            String r1 = route1.getRoute() != null ? route1.getRoute() : "";
                            String r2 = route2.getRoute() != null ? route2.getRoute() : "";
                            return r1.compareTo(r2);
                        }
                    }
                };
                
                // Sort both collections using the same comparator
                Collections.sort(routes, routeComparator);
                Collections.sort(allRoutes, routeComparator);
                
                // Log the first few routes to verify sorting
                int logLimit = Math.min(10, routes.size());
                Log.d("FragmentSearch", "First " + logLimit + " sorted routes:");
                for (int i = 0; i < logLimit; i++) {
                    BusRoute route = routes.get(i);
                    Log.d("FragmentSearch", i + ": " + route.getRoute() + " (" + route.getCompany() + ")");
                }
                
                // Log the count of each type of route for debugging
                int kmbCount = 0, ctbCount = 0, gmbCount = 0;
                for (BusRoute route : routes) {
                    if (route.getCompany().equals("kmb")) kmbCount++;
                    else if (route.getCompany().equals("ctb")) ctbCount++;
                    else if (route.getCompany().equals("GMB")) gmbCount++;
                }
                
                Log.d("FragmentSearch", "Loaded " + routes.size() + " total routes: " +
                      kmbCount + " KMB, " + ctbCount + " CTB, " + gmbCount + " GMB routes");
            } catch (Exception e) {
                Log.e("FragmentSearch", "Error sorting routes", e);
                // If sorting fails, try a simpler approach
                sortRoutesByNumberOnly(routes);
                sortRoutesByNumberOnly(allRoutes);
            }
        } catch (Exception e) {
            Log.e("FragmentSearch", "Error loading cached routes", e);
            Toast.makeText(requireContext(),
                    "Error loading cached routes: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }

        return routes;
    }
        
    /**
     * A simpler sorting method that focuses only on route numbers
     * This is used as a fallback if the main sorting method fails
     */
    private void sortRoutesByNumberOnly(List<BusRoute> routes) {
        try {
            Collections.sort(routes, new Comparator<BusRoute>() {
                @Override
                public int compare(BusRoute r1, BusRoute r2) {
                    if (r1 == null || r1.getRoute() == null) return -1;
                    if (r2 == null || r2.getRoute() == null) return 1;
                    
                    String route1 = r1.getRoute().trim().toUpperCase();
                    String route2 = r2.getRoute().trim().toUpperCase();
                    
                    // Pattern to match numeric prefix and alphabetic suffix
                    Pattern pattern = Pattern.compile("^(\\d+)([A-Z]*)");
                    Matcher matcher1 = pattern.matcher(route1);
                    Matcher matcher2 = pattern.matcher(route2);
                    
                    boolean hasNumericPrefix1 = matcher1.find();
                    boolean hasNumericPrefix2 = matcher2.find();
                    
                    // Case 1: Both have numeric prefixes (most common case)
                    if (hasNumericPrefix1 && hasNumericPrefix2) {
                        try {
                            // Compare the numeric parts first
                            int num1 = Integer.parseInt(matcher1.group(1));
                            int num2 = Integer.parseInt(matcher2.group(1));
                            
                            if (num1 != num2) {
                                return Integer.compare(num1, num2);
                            }
                            
                            // If numeric parts are equal, compare the alphabetic suffixes
                            String suffix1 = matcher1.group(2);
                            String suffix2 = matcher2.group(2);
                            
                            // If one has no suffix and the other does, the one without comes first
                            if (suffix1.isEmpty() && !suffix2.isEmpty()) {
                                return -1;
                            } else if (!suffix1.isEmpty() && suffix2.isEmpty()) {
                                return 1;
                            }
                            
                            // Both have suffixes, compare them alphabetically
                            return suffix1.compareTo(suffix2);
                        } catch (Exception e) {
                            // If any parsing fails, fall back to string comparison
                            return route1.compareTo(route2);
                        }
                    }
                    // Case 2: Only the first has a numeric prefix
                    else if (hasNumericPrefix1) {
                        return -1; // Numeric prefixes come before non-numeric
                    }
                    // Case 3: Only the second has a numeric prefix
                    else if (hasNumericPrefix2) {
                        return 1; // Numeric prefixes come before non-numeric
                    }
                    // Case 4: Neither has a numeric prefix, compare as strings
                    else {
                        return route1.compareTo(route2);
                    }
                }
            });
            
            Log.d("FragmentSearch", "Fallback sorting completed successfully");
        } catch (Exception e) {
            Log.e("FragmentSearch", "Even fallback sorting failed", e);
            // At this point, we just leave the list as is
        }
    }

    private void filterRoutes(String query) {
        List<BusRoute> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (BusRoute route : allRoutes) {
            // Check route number
            if (route.getRoute().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(route);
            } 
            // Also check origin and destination for matching text
            else if (route.getOrigEn().toLowerCase().contains(lowerCaseQuery) || 
                     route.getDestEn().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(route);
            }
        }

        busRoutes.clear();
        busRoutes.addAll(filteredList);
        adapter.notifyDataSetChanged();
        
        Log.d("FragmentSearch", "Filtered to " + filteredList.size() + " routes for query: " + query);
    }

    private boolean hideKeyboardOnOutsideTouch(MotionEvent event) {
        View view = getActivity().getCurrentFocus();
        if (view instanceof EditText) {
            Rect outRect = new Rect();
            view.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                view.clearFocus();
                hideKeyboard(view);
                return true;
            }
        }
        return false;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}