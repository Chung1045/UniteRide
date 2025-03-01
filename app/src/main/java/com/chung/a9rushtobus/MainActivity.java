package com.chung.a9rushtobus;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

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

        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
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
}