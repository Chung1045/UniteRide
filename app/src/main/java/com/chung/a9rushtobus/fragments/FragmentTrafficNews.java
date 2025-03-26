package com.chung.a9rushtobus.fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chung.a9rushtobus.DataFetcher;
import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.elements.RTHKTrafficAdapter;
import com.chung.a9rushtobus.elements.RTHKTrafficEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FragmentTrafficNews extends Fragment {
    private DataFetcher dataFetcher;
    private DatabaseHelper databaseHelper;
    private RTHKTrafficAdapter adapter;
    private List<RTHKTrafficEntry> trafficEntries;
    private Handler mainHandler;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ExecutorService executorService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        trafficEntries = new ArrayList<>();
        adapter = new RTHKTrafficAdapter(trafficEntries);
        databaseHelper = new DatabaseHelper(getContext());
        return inflater.inflate(R.layout.fragment_traffic_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataFetcher = new DataFetcher(getContext());
        
        // Initialize views
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        List<RTHKTrafficEntry> cachedNews = loadCachedNewsFromDatabase();
        trafficEntries.addAll(cachedNews);
        adapter.notifyDataSetChanged();

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::fetchTrafficNews);

        // Initial load
        fetchTrafficNews();
    }

    private List<RTHKTrafficEntry> loadCachedNewsFromDatabase() {
        List<RTHKTrafficEntry> cachedEntries = new ArrayList<>();

        try {
            // Get readable database
            SQLiteDatabase db = databaseHelper.getReadableDatabase();

            // Define projection (columns to retrieve)
            String[] projection = {
                    DatabaseHelper.Tables.RTHK_NEWS.COLUMN_CONTENT,
                    DatabaseHelper.Tables.RTHK_NEWS.COLUMN_DATE
            };

            // Query the database
            Cursor cursor = db.query(
                    DatabaseHelper.Tables.RTHK_NEWS.TABLE_NAME,
                    projection,
                    null,  // No WHERE clause
                    null,  // No WHERE arguments
                    null,  // No GROUP BY
                    null,  // No HAVING
                    DatabaseHelper.Tables.RTHK_NEWS.COLUMN_DATE + " DESC"  // Order by date descending
            );

            // Process the results
            while (cursor.moveToNext()) {
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.RTHK_NEWS.COLUMN_CONTENT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.Tables.RTHK_NEWS.COLUMN_DATE));

                cachedEntries.add(new RTHKTrafficEntry(content, date));
            }

            cursor.close();
        } catch (Exception e) {
            // Handle any database errors
            mainHandler.post(() -> {
                Toast.makeText(requireContext(),
                        "Error loading cached news: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            });
        }

        return cachedEntries;
    }

    private void fetchTrafficNews() {
        executorService.execute(() -> {
            try {
                swipeRefreshLayout.setRefreshing(true);
                Future<List<RTHKTrafficEntry>> futureNews = dataFetcher.fetchTrafficNews();
                List<RTHKTrafficEntry> newEntries = futureNews.get();

                mainHandler.post(() -> {
                    updateTrafficEntries(newEntries);
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (ExecutionException | InterruptedException e) {
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(),
                            "Error fetching traffic news: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }

    private void updateTrafficEntries(List<RTHKTrafficEntry> newEntries) {
        trafficEntries.clear();
        trafficEntries.addAll(newEntries);
        adapter.notifyDataSetChanged();

        if (adapter.getItemCount() == 0) {
            Toast.makeText(requireContext(), "No new traffic news found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dataFetcher.shutdown();
        executorService.shutdown();
    }
}