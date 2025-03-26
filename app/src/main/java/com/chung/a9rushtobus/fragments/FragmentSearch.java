package com.chung.a9rushtobus.fragments;

import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.chung.a9rushtobus.DataFetcher;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.R;
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
        busRoutes.addAll(loadCachedKMBRoutes());
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRoutes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private List<BusRoute> loadCachedKMBRoutes(){
        List<BusRoute> routes = new ArrayList<>();
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
            db.close();

            // Custom sorting: First sort numerically, then alphabetically
            Collections.sort(routes, (route1, route2) -> {
                String routeStr1 = route1.getRoute();  // Assuming BusRoute has getRoute() method
                String routeStr2 = route2.getRoute();

                // Use a regular expression to split the number and the letter suffix
                Pattern pattern = Pattern.compile("([0-9]+)([A-Za-z]*)");
                Matcher matcher1 = pattern.matcher(routeStr1);
                Matcher matcher2 = pattern.matcher(routeStr2);

                if (matcher1.matches() && matcher2.matches()) {
                    // Get the numeric part
                    int num1 = Integer.parseInt(matcher1.group(1));
                    int num2 = Integer.parseInt(matcher2.group(1));

                    // First, compare by the numeric part
                    if (num1 != num2) {
                        return Integer.compare(num1, num2);
                    }

                    // If the numbers are equal, compare the alphabetic suffix
                    String suffix1 = matcher1.group(2);
                    String suffix2 = matcher2.group(2);
                    return suffix1.compareTo(suffix2);
                }

                return 0; // Fallback in case matching fails
            });

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Error loading cached news: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }

        return routes;
    }


//    private void fetchBusRoutes() {
//        progressBar.setVisibility(View.VISIBLE);
//
//        dataFetcher.fetchAllBusRoutes(
//            routes -> {
//                if (getActivity() == null) return;
//
//                progressBar.setVisibility(View.GONE);
//                allRoutes.clear();
//                allRoutes.addAll(routes);
//                busRoutes.clear();
//                busRoutes.addAll(routes);
//                adapter.notifyDataSetChanged();
//
//                if (getContext() != null) {
//                    if (routes.isEmpty()) {
//                        Toast.makeText(getContext(), "No routes found", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(getContext(), "Found " + routes.size() + " routes", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            },
//            errorMessage -> {
//                if (getActivity() == null) return;
//
//                progressBar.setVisibility(View.GONE);
//                if (getContext() != null) {
//                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
//                }
//            }
//        );
//    }

    private void filterRoutes(String query) {
        List<BusRoute> filteredList = new ArrayList<>();

        for (BusRoute route : allRoutes) {
            if (route.getRoute().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(route);
            }
        }

        busRoutes.clear();
        busRoutes.addAll(filteredList);
        adapter.notifyDataSetChanged();
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