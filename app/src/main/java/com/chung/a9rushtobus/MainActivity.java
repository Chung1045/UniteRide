package com.chung.a9rushtobus;

import android.app.UiModeManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private UserPreferences userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userPreferences = new UserPreferences(this);
        initTheme();

        bottomNav = findViewById(R.id.bottomNav_main);
        initListener();

    }

    private void initListener() {

        bottomNav.setOnItemSelectedListener(item -> {
            int menuItemId = item.getItemId();
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