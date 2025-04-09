package com.chung.a9rushtobus.fragments;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.preferences.SettingsAboutView;
import com.chung.a9rushtobus.preferences.SettingsAccessibilityPreferenceView;
import com.chung.a9rushtobus.preferences.SettingsDatabaseView;
import com.chung.a9rushtobus.preferences.SettingsFeaturesPreferenceView;
import com.chung.a9rushtobus.preferences.SettingsLanguagePreferenceView;
import com.chung.a9rushtobus.preferences.SettingsMainPreferenceView;
import com.chung.a9rushtobus.preferences.SettingsThemePreferenceView;
import com.google.android.material.appbar.MaterialToolbar;

public class FragmentSettings extends Fragment {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Update toolbar title if needed
        if (getView() != null) {
            MaterialToolbar toolbar = getView().findViewById(R.id.settingsToolBar);
            if (toolbar != null) {
                // Get current fragment in container
                Fragment currentFragment = getChildFragmentManager().findFragmentById(R.id.fragmentContainerView);
                
                // Update title based on current fragment
                if (currentFragment instanceof SettingsMainPreferenceView) {
                    toolbar.setTitle(getString(R.string.bottomNav_settings_tabName));
                } else if (currentFragment instanceof SettingsLanguagePreferenceView) {
                    toolbar.setTitle(getString(R.string.settings_category_langOption_name));
                } else if (currentFragment instanceof SettingsThemePreferenceView) {
                    toolbar.setTitle(getString(R.string.settings_category_themeOption_name));
                } else if (currentFragment instanceof SettingsFeaturesPreferenceView) {
                    toolbar.setTitle(getString(R.string.settings_category_featuresOption_name));
                } else if (currentFragment instanceof SettingsAccessibilityPreferenceView) {
                    toolbar.setTitle(getString(R.string.settings_category_accessibilityOption_name));
                } else if (currentFragment instanceof SettingsAboutView) {
                    toolbar.setTitle(getString(R.string.settings_category_aboutOption_name));
                } else if (currentFragment instanceof SettingsDatabaseView) {
                    toolbar.setTitle(getString(R.string.settings_category_database_name));
                }
            }
        }
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
            toolbar.setNavigationIcon(R.drawable.back);
        } else {
            toolbar.setNavigationIcon(null);
        }
    }

}