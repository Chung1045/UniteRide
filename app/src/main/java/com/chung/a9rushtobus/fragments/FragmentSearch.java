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
import com.chung.a9rushtobus.elements.BusRoute;
import com.chung.a9rushtobus.elements.BusRouteAdapter;

import java.util.ArrayList;
import java.util.List;

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
        fetchBusRoutes();

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
        try{
            SQLiteDatabase db = databaseHelper.getReadableDatabase();

            String[] projection = {
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ROUTE,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_BOUND,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ORIGIN_TC,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ORIGIN_SC,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_DEST_EN,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_DEST_TC,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_DEST_SC
            };

            Cursor cursor = db.query(
                    DatabaseHelper.Tables.KMB_ROUTES.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ROUTE + " ASC" // not good enough, need custom sort
            );

            while (cursor.moveToNext()) {
                String route = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ROUTE));
                String bound = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_BOUND));
                String serviceType = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_SERVICE_TYPE));
                String originEn = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ORIGIN_EN));
                String originTc = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ORIGIN_TC));
                String originSc = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_ORIGIN_SC));
                String destEn = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_DEST_EN));
                String destTc = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_DEST_TC));
                String destSc = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Tables.KMB_ROUTES.COLUMN_DEST_SC));

                routes.add(new BusRoute(route, "kmb", bound, serviceType, originEn, originTc, originSc, destEn, destTc, destSc));
            }
            cursor.close();
            db.close();

        } catch (Exception e){
            Toast.makeText(requireContext(),
                    "Error loading cached news: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }

        return routes;

    }

    private void fetchBusRoutes() {
        progressBar.setVisibility(View.VISIBLE);
        
        dataFetcher.fetchAllBusRoutes(
            routes -> {
                if (getActivity() == null) return;
                
                progressBar.setVisibility(View.GONE);
                allRoutes.clear();
                allRoutes.addAll(routes);
                busRoutes.clear();
                busRoutes.addAll(routes);
                adapter.notifyDataSetChanged();

                if (getContext() != null) {
                    if (routes.isEmpty()) {
                        Toast.makeText(getContext(), "No routes found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Found " + routes.size() + " routes", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            errorMessage -> {
                if (getActivity() == null) return;
                
                progressBar.setVisibility(View.GONE);
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        );
    }

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