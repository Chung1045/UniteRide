package com.chung.a9rushtobus.fragments;

import android.content.Context;
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
import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.elements.BusRoute;
import com.chung.a9rushtobus.elements.BusRouteAdapter;

import java.util.ArrayList;
import java.util.List;

public class FragmentSearch extends Fragment {

    private DataFetcher dataFetcher;
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

        dataFetcher = new DataFetcher();
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        searchEditText = view.findViewById(R.id.search_edit_text);

        busRoutes = new ArrayList<>();
        allRoutes = new ArrayList<>();
        adapter = new BusRouteAdapter(requireContext(), busRoutes);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        listenerInit();
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
//            if (route.getRoute().toLowerCase().contains(query.toLowerCase())
//                    || route.getDestEn().toLowerCase().contains(query.toLowerCase())) {
//                filteredList.add(route);
//            }
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