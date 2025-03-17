package com.chung.a9rushtobus.fragments;

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
        return inflater.inflate(R.layout.fragment_traffic_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataFetcher = new DataFetcher();
        
        // Initialize views
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::fetchTrafficNews);

        // Initial load
        fetchTrafficNews();
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