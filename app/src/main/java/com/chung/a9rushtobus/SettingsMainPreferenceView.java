package com.chung.a9rushtobus;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsMainPreferenceView extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_main, rootKey);
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set initial toolbar title
        updateToolbarTitle("Settings");
    }

    private void updateToolbarTitle(String title) {
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.settingsToolBar);
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) toolbar.getParent();

        toolbar.setTitle(title);
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(title);
        }
    }

    private void listenerInit() {
        Preference applicationLanguage = findPreference("pref_main_language");
        assert applicationLanguage != null;

        applicationLanguage.setOnPreferenceClickListener(view -> {
            updateToolbarTitle("Language");
            Toast.makeText(getContext(), "clicked", Toast.LENGTH_SHORT).show();
            return false;
        });

        Preference applicationTheme = findPreference("pref_main_theme");
        assert applicationTheme != null;

        applicationTheme.setOnPreferenceClickListener(view -> {
            updateToolbarTitle("Theme");
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new SettingsThemePreferenceView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });

        Preference feauturePreference = findPreference("pref_main_features");
        assert feauturePreference != null;

        feauturePreference.setOnPreferenceClickListener(view -> {
            updateToolbarTitle("Features");
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new SettingsFeaturesPreferenceView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });
    }
}