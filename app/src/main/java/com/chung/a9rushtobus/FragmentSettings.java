package com.chung.a9rushtobus;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.appbar.MaterialToolbar;

public class FragmentSettings extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new SettingsMainPreferenceView())
                    .commit();
        }

        return view;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.settingsToolBar);
        if (toolbar != null) {
            // Set initial state
            updateNavigationIcon(toolbar);

            toolbar.setNavigationOnClickListener(v ->
                    getChildFragmentManager().popBackStack()
            );

            // Listen for back stack changes
            getChildFragmentManager().addOnBackStackChangedListener(() ->
                    updateNavigationIcon(toolbar)
            );
        }
    }

    private void updateNavigationIcon(MaterialToolbar toolbar) {
        int backStackCount = getChildFragmentManager().getBackStackEntryCount();
        if (backStackCount > 0) {
            toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24);
        } else {
            toolbar.setNavigationIcon(null);
        }
    }

}