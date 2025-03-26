package com.chung.a9rushtobus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class OnboardingFragments {
    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;

    public static class FragmentOnBoarding1 extends Fragment{
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_1_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Find your button
            Button nextButton = view.findViewById(R.id.onboarding_1_5_next_button); // Replace with your actual button ID

            // Set up click listener
            nextButton.setOnClickListener(v -> {
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).goToNextPage();
                }
            });
        }
    }

    public static class FragmentOnBoarding2 extends Fragment{
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_2_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Find your button
            Button nextButton = view.findViewById(R.id.onboarding_2_5_next_button); // Replace with your actual button ID

            // Set up click listener
            nextButton.setOnClickListener(v -> {
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).goToNextPage();
                }
            });
        }
    }

    public static class FragmentOnBoarding3 extends Fragment {
        private static final int NOTIFICATION_PERMISSION_CODE = 100;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_3_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Find your buttons
            Button nextButton = view.findViewById(R.id.onboarding_3_5_next_button);
            Button accessButton = view.findViewById(R.id.onboarding_3_5_access_button);

            // Set up click listener for next button
            nextButton.setOnClickListener(v -> {
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).goToNextPage();
                }
            });

            // Set up click listener for access button to request notification permission
            accessButton.setOnClickListener(v -> {
                requestRuntimePermission();
            });
        }

        private void requestRuntimePermission() {
            // Check if permission is already granted
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission Granted. You can use the API which requires the permission.", Toast.LENGTH_LONG).show();
            } else {
                // Permission is not granted, request it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // POST_NOTIFICATIONS was added in API 33 (Tiramisu)
                    requestPermissions(
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE
                    );
                } else {
                    // For older Android versions, notification permission isn't required
                    Toast.makeText(requireContext(), "Notification permission not required for this Android version", Toast.LENGTH_LONG).show();
                }
            }
        }

        // Handle the permission request result
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == NOTIFICATION_PERMISSION_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Permission Granted. You can use the API which requires the permission.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Permission Denied. You cannot use the API which requires the permission.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public static class FragmentOnBoarding4 extends Fragment {
        private static final int LOCATION_PERMISSION_CODE = 101;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_4_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Find your buttons
            Button nextButton = view.findViewById(R.id.onboarding_4_5_next_button);
            Button accessButton = view.findViewById(R.id.onboarding_4_5_access_button);

            // Set up click listener for next button
            nextButton.setOnClickListener(v -> {
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).goToNextPage();
                }
            });

            // Set up click listener for access button to request location permission
            accessButton.setOnClickListener(v -> {
                requestRuntimePermission();
            });
        }

        private void requestRuntimePermission() {
            // Check if permission is already granted
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission Granted. You can use the API which requires the permission.", Toast.LENGTH_LONG).show();
            } else {
                // Permission is not granted, request it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // POST_NOTIFICATIONS was added in API 33 (Tiramisu)
                    requestPermissions(
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_CODE
                    );
                } else {
                    // For older Android versions, notification permission isn't required
                    Toast.makeText(requireContext(), "Notification permission not required for this Android version", Toast.LENGTH_LONG).show();
                }
            }
        }

        // Handle the permission request result
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == NOTIFICATION_PERMISSION_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Permission Granted. You can use the API which requires the permission.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Permission Denied. You cannot use the API which requires the permission.", Toast.LENGTH_LONG).show();
                }
            }
        }

    }

    public static class FragmentOnBoarding5 extends Fragment{
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_5_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Find the finish button
            Button finishButton = view.findViewById(R.id.onboarding_5_5_next_button);

            // Set up finish button click listener
            finishButton.setOnClickListener(v -> {
                // Complete onboarding and proceed to the main activity
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                requireActivity().overridePendingTransition(0, 0); // Disable transition animation
                requireActivity().finish();
            });
        }
    }
}
