package com.chung.a9rushtobus.preferences;

import static org.chromium.base.ThreadUtils.runOnUiThread;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.chung.a9rushtobus.service.DataFetcher;
import com.chung.a9rushtobus.OnboardingActivity;
import com.chung.a9rushtobus.R;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsMainPreferenceView extends PreferenceFragmentCompat {

    private static final String KEY_CURRENT_TITLE = "current_title";
    private BottomNavigationView bottomNavigationView;
    private DataFetcher dataFetcher;
    private String currentTitle;

    private MaterialToolbar toolbar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_main, rootKey);
        dataFetcher = new DataFetcher(requireActivity());
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bottomNavigationView = requireActivity().findViewById(R.id.bottomNav_main);

        // Restore saved title or set default
        if (savedInstanceState != null) {
            currentTitle = savedInstanceState.getString(KEY_CURRENT_TITLE);
        }
        if (currentTitle == null) {
            currentTitle = getString(R.string.bottomNav_settings_tabName);
        }

        // Update title directly to ensure it's applied
        toolbar = getParentFragment().getView().findViewById(R.id.settingsToolBar);
        if (toolbar != null) {
            toolbar.setTitle(currentTitle);

            // Find the CollapsingToolbarLayout directly
            CollapsingToolbarLayout collapsingToolbarLayout =
                (CollapsingToolbarLayout) toolbar.getParent();
            if (collapsingToolbarLayout != null) {
                collapsingToolbarLayout.setTitle(currentTitle);
            }

            Log.d("SettingsMainPreference", "onViewCreated: Title set to: " + currentTitle);
        }

        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getParentFragmentManager().getBackStackEntryCount() == 0) {
            String defaultTitle = getString(R.string.bottomNav_settings_tabName);
            updateToolbarTitle(defaultTitle);
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
    }

    private void updateToolbarTitle(String title) {
        if (!isAdded() || getActivity() == null) {
            return; // Don't proceed if fragment is not attached
        }

        requireActivity().runOnUiThread(() -> {
            try {
                if (toolbar != null) {
                    toolbar.setTitle(title);

                    // Find the CollapsingToolbarLayout directly
                    CollapsingToolbarLayout collapsingToolbarLayout =
                        (CollapsingToolbarLayout) toolbar.getParent();
                    if (collapsingToolbarLayout != null) {
                        collapsingToolbarLayout.setTitle(title);
                    }

                    // Save the current title
                    currentTitle = title;

                    // Log successful title update
                    Log.d("SettingsMainPreference", "Title updated to: " + title);
                } else {
                    Log.e("SettingsMainPreference", "Toolbar not found");
                }
            } catch (Exception e) {
                Log.e("SettingsMainPreference", "Error updating toolbar title: " + e.getMessage(), e);
            }
        });
    }

    private void listenerInit() {

        Preference applicationLanguage = findPreference("pref_main_language");
        assert applicationLanguage != null;

        applicationLanguage.setOnPreferenceClickListener(view -> {
            String newTitle = getString(R.string.settings_category_langOption_name);

            // Update title and ensure it's applied before fragment transaction
            updateToolbarTitle(newTitle);

            // Add a small delay to ensure UI updates before fragment transaction
            new Handler().postDelayed(() -> {
                if (isAdded()) {  // Check if fragment is still attached
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                            ).replace(R.id.fragmentContainerView, SettingsLanguagePreferenceView.class, null)
                            .addToBackStack(null)
                            .commit();
                    bottomNavigationView.setVisibility(View.GONE);
                }
            }, 50);  // Small delay to ensure UI updates

            return true;  // Indicate we handled the click
        });

        Preference applicationTheme = findPreference("pref_main_theme");
        assert applicationTheme != null;

        applicationTheme.setOnPreferenceClickListener(view -> {
            String newTitle = getString(R.string.settings_category_themeOption_name);

            // Update title and ensure it's applied before fragment transaction
            updateToolbarTitle(newTitle);

            // Add a small delay to ensure UI updates before fragment transaction
            new Handler().postDelayed(() -> {
                if (isAdded()) {  // Check if fragment is still attached
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                            ).replace(R.id.fragmentContainerView, new SettingsThemePreferenceView())
                            .addToBackStack(null)
                            .commit();
                    bottomNavigationView.setVisibility(View.GONE);
                }
            }, 50);  // Small delay to ensure UI updates

            return true;
        });

        Preference feauturePreference = findPreference("pref_main_features");
        assert feauturePreference != null;

        feauturePreference.setOnPreferenceClickListener(view -> {
            String newTitle = getString(R.string.settings_category_featuresOption_name);

            updateToolbarTitle(newTitle);

            // Add a small delay to ensure UI updates before fragment transaction
            new Handler().postDelayed(() -> {
                if (isAdded()) {  // Check if fragment is still attached
                    getParentFragmentManager().beginTransaction().setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                            ).replace(R.id.fragmentContainerView, new SettingsFeaturesPreferenceView())
                            .addToBackStack(null)
                            .commit();
                    bottomNavigationView.setVisibility(View.GONE);
                }
            }, 50);  // Small delay to ensure UI updates

            return true;
        });

        Preference aboutPreference = findPreference("pref_main_about");
        assert aboutPreference != null;

        aboutPreference.setOnPreferenceClickListener(view -> {
            String newTitle = getString(R.string.settings_category_aboutOption_name);

            // Update title and ensure it's applied before fragment transaction
            updateToolbarTitle(newTitle);

            // Add a small delay to ensure UI updates before fragment transaction
            new Handler().postDelayed(() -> {
                if (isAdded()) {  // Check if fragment is still attached
                    getParentFragmentManager().beginTransaction().setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                            ).replace(R.id.fragmentContainerView, new SettingsAboutView())
                            .addToBackStack(null)
                            .commit();
                    bottomNavigationView.setVisibility(View.GONE);
                }
            }, 50);  // Small delay to ensure UI updates

            return true;
        });

        Preference accessibilityPreference = findPreference("pref_main_accessibility");
        assert accessibilityPreference != null;

        accessibilityPreference.setOnPreferenceClickListener(view -> {
            String newTitle = getString(R.string.settings_category_accessibilityOption_name);

            // Update title and ensure it's applied before fragment transaction
            updateToolbarTitle(newTitle);

            // Add a small delay to ensure UI updates before fragment transaction
            new Handler().postDelayed(() -> {
                if (isAdded()) {  // Check if fragment is still attached
                    getParentFragmentManager().beginTransaction().setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                            ).replace(R.id.fragmentContainerView, new SettingsAccessibilityPreferenceView())
                            .addToBackStack(null)
                            .commit();
                    bottomNavigationView.setVisibility(View.GONE);
                }
            }, 50);  // Small delay to ensure UI updates

            return true;
        });

        Preference dev_OnboardPreference = findPreference("pref_dev_onboard");
        assert dev_OnboardPreference != null;

        dev_OnboardPreference.setOnPreferenceClickListener(view -> {
            Intent newActivity = new Intent(view.getContext(), OnboardingActivity.class);
            startActivity(newActivity, null);
            return false;
        });

        Preference dev_FetchData = findPreference("pref_dev_fetchData");
        assert dev_FetchData!= null;

        dev_FetchData.setOnPreferenceClickListener(view -> {
            new Thread(() -> {
                try {
                    runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Fetching data...", Toast.LENGTH_SHORT).show();
                        dataFetcher.refreshAllData();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
            return false;
        });

    }
}