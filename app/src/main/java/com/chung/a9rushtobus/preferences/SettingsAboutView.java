package com.chung.a9rushtobus.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.Utils;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsAboutView extends Fragment {
    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private TextView prefAboutPermission, prefAboutLibraries, prefAboutGitHub;
    private Utils utils;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_about, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        toolbar = requireActivity().findViewById(R.id.settingsToolBar);
        collapsingToolbar = (CollapsingToolbarLayout) toolbar.getParent();
        utils = new Utils(requireActivity(), view, getContext());
        prefAboutPermission = view.findViewById(R.id.pref_about_permission);
        prefAboutLibraries = view.findViewById(R.id.pref_about_libraries);
        prefAboutGitHub = view.findViewById(R.id.pref_about_github);
        listenerInit();
    }

    private void listenerInit() {

        prefAboutPermission.setOnClickListener(view -> {
            updateToolbarTitle("Permission");
            getParentFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    ).replace(R.id.fragmentContainerView, new PrefPermissionView())
                    .addToBackStack(null)
                    .commit();
        });

        prefAboutLibraries.setOnClickListener(view -> updateToolbarTitle("Libraries"));

        prefAboutGitHub.setOnClickListener(view -> utils.startUrlIntent("https://www.github.com"));

    }

    private void updateToolbarTitle(String title) {

        toolbar.setTitle(title);
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(title);
        }
    }

    public static class PrefPermissionView extends Fragment {
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_settings_permission_description, container, false);
        }

    }

    public static class PrefLibrariesView extends Fragment {

    }
}
