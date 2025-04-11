package com.chung.a9rushtobus;

import android.app.UiModeManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chung.a9rushtobus.database.DatabaseHelper;
import com.chung.a9rushtobus.fragments.FragmentNearby;
import com.chung.a9rushtobus.fragments.FragmentSaved;
import com.chung.a9rushtobus.fragments.FragmentSearch;
import com.chung.a9rushtobus.fragments.FragmentSettings;
import com.chung.a9rushtobus.fragments.FragmentTrafficNews;
import com.chung.a9rushtobus.preferences.LocaleHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;

import java.io.File;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private UserPreferences userPreferences;
    private DatabaseHelper dbHelper;
    private LocaleHelper localeHelper;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private boolean isDataReady = false;
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Check if this is a locale configuration change
        if (localeHelper != null) {
            // Update all UI elements to reflect the new language
            localeHelper.updateAllUIElements(this);
            
            // Force refresh all main fragments
            refreshAllMainFragments();
        }
    }
    
    /**
     * Forces a refresh of all main fragments to update their UI with the new language
     */
    private void refreshAllMainFragments() {
        // Get all fragments from the activity's fragment manager
        for (androidx.fragment.app.Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.isAdded()) {
                // Force fragment to refresh its UI
                View fragmentView = fragment.getView();
                if (fragmentView != null) {
                    // Trigger a layout pass
                    fragmentView.requestLayout();
                    
                    // If this is a specific fragment type, handle it specially
                    if (fragment instanceof FragmentSaved) {
                        // Update saved fragment UI
                        updateSavedFragmentUI(fragment);
                    } else if (fragment instanceof FragmentNearby) {
                        // Update nearby fragment UI
                        updateNearbyFragmentUI(fragment);
                    } else if (fragment instanceof FragmentSearch) {
                        // Update search fragment UI
                        updateSearchFragmentUI(fragment);
                    } else if (fragment instanceof FragmentSettings) {
                        // Settings fragment is already handled elsewhere
                    }
                }
            }
        }
    }
    
    /**
     * Updates the UI of the saved fragment
     */
    private void updateSavedFragmentUI(androidx.fragment.app.Fragment fragment) {
        View view = fragment.getView();
        if (view == null) return;
        
        // Update "No saved stops" text
        TextView noSavedText = view.findViewById(R.id.text_no_saved_stops);
        if (noSavedText != null) {
            noSavedText.setText(R.string.frag_saved_noSaved_title);
        }
        
        // Update toolbar title
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolBar_saved);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.bottomNav_saved_tabName));
        }
    }
    
    /**
     * Updates the UI of the nearby fragment
     */
    private void updateNearbyFragmentUI(androidx.fragment.app.Fragment fragment) {
        View view = fragment.getView();
        if (view == null) return;
        
        // Update bottom sheet title
        TextView sheetTitle = view.findViewById(R.id.sheetTitle);
        if (sheetTitle != null) {
            sheetTitle.setText(R.string.bottomNav_nearby_tabName);
        }
        
        // Update location info text
        TextView locationInfo = view.findViewById(R.id.locationInfo);
        if (locationInfo != null) {
            locationInfo.setText(R.string.settings_location_required_req_name);
        }
        
        // Update location permission button
        com.google.android.material.button.MaterialButton btnLocationPermission = 
            view.findViewById(R.id.btnLocationPermission);
        if (btnLocationPermission != null) {
            btnLocationPermission.setText(R.string.onboarding_4_5_access_button_content);
        }
    }
    
    /**
     * Updates the UI of the search fragment
     */
    private void updateSearchFragmentUI(androidx.fragment.app.Fragment fragment) {
        View view = fragment.getView();
        if (view == null) return;
        
        // Update search bar hint
        EditText searchEditText = view.findViewById(R.id.search_edit_text);
        if (searchEditText != null) {
            searchEditText.setHint(getString(R.string.frag_search_searchBar_hint));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);

        // Initialize user preferences
        userPreferences = new UserPreferences(this);

        // Always initialize locale helper and apply language settings
        // regardless of savedInstanceState to preserve language during theme changes
        localeHelper = new LocaleHelper();
        String localeCode = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
        
        // Only apply locale and recreate if this is the first creation (not a recreation due to theme change)
        if (savedInstanceState == null) {
            localeHelper.setAppLocale(this, localeCode);
            recreate();
        } else {
            // For recreations (like theme changes), just apply the locale without recreating again
            localeHelper.setAppLocaleWithoutRecreate(this, localeCode);
        }

        // Continue with existing logic - use singleton pattern
        dbHelper = DatabaseHelper.getInstance(this);

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> !isDataReady);

        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            // This code will be executed after the splash screen is dismissed.
            boolean isFirstTimeLaunch = !UserPreferences.sharedPref.getBoolean(UserPreferences.ONBOARDING_COMPLETE, false);

            Log.e("MainActivity", "isFirstTimeLaunch: " + isFirstTimeLaunch);
            if (isFirstTimeLaunch) {
                startActivity(new Intent(this, OnboardingActivity.class));
                finish();
            }
            splashScreenView.remove();
        });

        new Thread(() -> {
            try {
                // Simulate background work like database loading
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isDataReady = true;
            runOnUiThread(() -> splashScreen.setKeepOnScreenCondition(() -> false));
        }).start();
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav_main);
        initTheme();
        initListener();
    }


    private void updateBottomNavVisibility() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav_main);
        boolean isVisible = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_FEATURE_SHOW_RTHK_NEWS, false);
        bottomNav.getMenu().findItem(R.id.menu_item_main_news).setVisible(isVisible);
    }

    private void initListener() {
        bottomNav.setItemActiveIndicatorColor(
                ContextCompat.getColorStateList(this, R.color.brand_colorPrimary)
        );

        // Create fragments once and reuse them
        final FragmentSaved savedFragment = new FragmentSaved();
        final FragmentNearby nearbyFragment = new FragmentNearby();
        final FragmentSearch searchFragment = new FragmentSearch();
        final FragmentTrafficNews trafficNewsFragment = new FragmentTrafficNews();
        final FragmentSettings settingsFragment = new FragmentSettings();
        
        // Add the initial fragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentView_main, savedFragment)
                .add(R.id.fragmentView_main, nearbyFragment)
                .add(R.id.fragmentView_main, searchFragment)
                .add(R.id.fragmentView_main, trafficNewsFragment)
                .add(R.id.fragmentView_main, settingsFragment)
                .hide(savedFragment)
                .hide(nearbyFragment)
                .hide(searchFragment)
                .hide(trafficNewsFragment)
                .hide(settingsFragment)
                .show(savedFragment) // Show the default fragment
                .commit();
        
        bottomNav.setOnItemSelectedListener(item -> {
            int menuItemId = item.getItemId();
            bottomNav.setItemActiveIndicatorColor(
                    ContextCompat.getColorStateList(this, R.color.brand_colorPrimary)
            );
            
            // Use show/hide instead of replace for better performance
            if (menuItemId == R.id.menu_item_main_saved) {
                getSupportFragmentManager().beginTransaction()
                        .hide(nearbyFragment)
                        .hide(searchFragment)
                        .hide(trafficNewsFragment)
                        .hide(settingsFragment)
                        .show(savedFragment)
                        .commit();
                return true;
            } else if (menuItemId == R.id.menu_item_main_nearby) {
                getSupportFragmentManager().beginTransaction()
                        .hide(savedFragment)
                        .hide(searchFragment)
                        .hide(trafficNewsFragment)
                        .hide(settingsFragment)
                        .show(nearbyFragment)
                        .commit();
                return true;
            } else if (menuItemId == R.id.menu_item_main_search) {
                getSupportFragmentManager().beginTransaction()
                        .hide(savedFragment)
                        .hide(nearbyFragment)
                        .hide(trafficNewsFragment)
                        .hide(settingsFragment)
                        .show(searchFragment)
                        .commit();
                return true;
            } else if (menuItemId == R.id.menu_item_main_news) {
                getSupportFragmentManager().beginTransaction()
                        .hide(savedFragment)
                        .hide(nearbyFragment)
                        .hide(searchFragment)
                        .hide(settingsFragment)
                        .show(trafficNewsFragment)
                        .commit();
                return true;
            } else if (menuItemId == R.id.menu_item_main_settings) {
                getSupportFragmentManager().beginTransaction()
                        .hide(savedFragment)
                        .hide(nearbyFragment)
                        .hide(searchFragment)
                        .hide(trafficNewsFragment)
                        .show(settingsFragment)
                        .commit();
                return true;
            } else {
                return false;
            }
        });

        preferenceChangeListener = (sharedPreferences, key) -> {
            if (UserPreferences.SETTINGS_FEATURE_SHOW_RTHK_NEWS.equals(key)) {
                updateBottomNavVisibility();
            } else if (UserPreferences.SETTINGS_ACCESS_BOLD_TEXT.equals(key)) {
                // Recreate the activity to apply the new theme
                recreate();
            }
        };

        UserPreferences.sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void initTheme() {
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        int currentMode = uiModeManager.getNightMode();

        if (UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_FOLLOW_SYSTEM, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            if (UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_DARK, false)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_LIGHT, false)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                // This is the default setting for first time user
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_FOLLOW_SYSTEM, true).commit();
                if (currentMode == UiModeManager.MODE_NIGHT_YES) {
                    UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_DARK, true).commit();
                    UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_LIGHT, false).commit();
                } else {
                    UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_LIGHT, true).commit();
                    UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_DARK, false).commit();
                }
            }
        }
    }
}