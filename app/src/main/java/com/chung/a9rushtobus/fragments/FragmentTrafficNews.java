package com.chung.a9rushtobus.fragments;

import android.content.Context;
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

import com.chung.a9rushtobus.service.DataFetcher;
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
        databaseHelper = DatabaseHelper.getInstance(getContext());
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

    private boolean isFragmentValid() {
        return isAdded() && getActivity() != null && !isDetached();
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
        if (!isFragmentValid()) return;

        executorService.execute(() -> {
            // Check again since we're in a background thread
            if (!isFragmentValid()) return;

            try {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(true);
                        }
                    });
                }

                Future<List<RTHKTrafficEntry>> futureNews = dataFetcher.fetchTrafficNews();
                List<RTHKTrafficEntry> newEntries = futureNews.get();

                // Check again before posting to main thread
                if (!isFragmentValid()) return;

                mainHandler.post(() -> {
                    if (!isFragmentValid()) return;

                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    updateTrafficEntries(newEntries);
                });
            } catch (ExecutionException | InterruptedException e) {
                if (!isFragmentValid()) return;

                mainHandler.post(() -> {
                    if (!isFragmentValid()) return;

                    Context context = getContext();
                    if (context != null && swipeRefreshLayout != null) {
                        Toast.makeText(context,
                                "Error fetching traffic news: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void updateTrafficEntries(List<RTHKTrafficEntry> newEntries) {
        if (!isFragmentValid()) return;

        trafficEntries.clear();
        trafficEntries.addAll(newEntries);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (adapter != null && adapter.getItemCount() == 0) {
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "No new traffic news found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        // Cancel any pending operations
        mainHandler.removeCallbacksAndMessages(null);

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Force shutdown of any running tasks
        }

        if (dataFetcher != null) {
            dataFetcher.shutdown();
        }

        swipeRefreshLayout = null;
        adapter = null;
        dataFetcher = null;

        super.onDestroyView();
    }
}