package com.chung.a9rushtobus;

import android.app.UiModeManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private UserPreferences userPreferences;
    private DatabaseHelper dbHelper;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private boolean isDataReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userPreferences = new UserPreferences(this);
        dbHelper = new DatabaseHelper(this);
        dbHelper.onCreate(dbHelper.getWritableDatabase());

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> !isDataReady);

        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            // This code will be executed after the splash screen is dismissed.
            boolean isFirstTimeLaunch = UserPreferences.sharedPref.getBoolean(UserPreferences.ONBOARDING_COMPLETE, true);

            if (isFirstTimeLaunch) {
                startActivity(new Intent(this, OnboardingActivity.class));
                finish();
            } else {
                initTheme();

                bottomNav = findViewById(R.id.bottomNav_main);
                initListener();
            }
            // Remove the splash screen view from the view hierarchy.
            splashScreenView.remove();
        });

        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (Exception e){
                e.printStackTrace();
            }
            isDataReady = true;
        }).start();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNav = findViewById(R.id.bottomNav_main);
        bottomNav.setItemActiveIndicatorColor(
                ContextCompat.getColorStateList(this, R.color.brand_colorPrimary)
        );

        initTheme();
        initListener();
    }

    private void updateBottomNavVisibility() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav_main);
        boolean isVisible = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_FEATURE_SHOW_RTHK_NEWS, false);
        bottomNav.getMenu().findItem(R.id.menu_item_main_news).setVisible(isVisible);
    }

    private void initListener() {

        bottomNav.setOnItemSelectedListener(item -> {
            int menuItemId = item.getItemId();
            bottomNav.setItemActiveIndicatorColor(
                    ContextCompat.getColorStateList(this, R.color.brand_colorPrimary)
            );
            if (menuItemId == R.id.menu_item_main_saved) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentView_main, new FragmentSaved()).commit();
                return true;
            } else if (menuItemId == R.id.menu_item_main_nearby) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentView_main, new FragmentNearby()).commit();
                return true;
            } else if (menuItemId == R.id.menu_item_main_search) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentView_main, new FragmentSearch()).commit();
                return true;
            } else if (menuItemId == R.id.menu_item_main_news) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentView_main, new FragmentTrafficNews()).commit();
                return true;
            } else if (menuItemId == R.id.menu_item_main_settings) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentView_main, new FragmentSettings()).commit();
                return true;
            } else {
                return false;
            }
        });

        preferenceChangeListener = (sharedPreferences, key) -> {
            if (UserPreferences.SETTINGS_FEATURE_SHOW_RTHK_NEWS.equals(key)) {
                updateBottomNavVisibility();
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