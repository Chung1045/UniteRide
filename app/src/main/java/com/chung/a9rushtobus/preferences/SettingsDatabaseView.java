package com.chung.a9rushtobus.preferences;

import static org.chromium.base.ThreadUtils.runOnUiThread;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.service.DataFetcher;
import com.chung.a9rushtobus.service.DataFetcherCancellation;

public class SettingsDatabaseView extends PreferenceFragmentCompat {

    private DataFetcher dataFetcher;
    private DataFetcherCancellation dataFetcherCancellation;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_database, rootKey);
        dataFetcher = new DataFetcher(getContext());
        dataFetcherCancellation = new DataFetcherCancellation(dataFetcher);
        listenerInit();
    }

    private void listenerInit() {
        Preference dev_FetchData = findPreference("pref_database_fetch_optionName");
        assert dev_FetchData != null;

        dev_FetchData.setOnPreferenceClickListener(view -> {
            // Show the progress dialog
            ProgressDialogManager progressDialog = ProgressDialogManager.show(
                    getContext(),
                    "Updating Database",
                    "Preparing to fetch data..."
            );
            
            // Set up cancel listener to properly cancel all operations
            progressDialog.setOnCancelListener(() -> {
                // Cancel all ongoing operations in DataFetcher
                if (dataFetcherCancellation.cancelOperations()) {
                    Log.d("SettingsDatabaseView", "Data fetch cancelled by user - operations cancelled");
                    Toast.makeText(getContext(), "Operation cancelled by user", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("SettingsDatabaseView", "Data fetch cancelled by user - operations were already cancelled");
                }
            });

            // Start the data fetching in a background thread
            new Thread(() -> {
                try {
                    // Set a safety timeout to dismiss the dialog after 2 minutes (120,000 ms)
                    // in case something goes wrong and the dialog doesn't get dismissed properly
                    final Runnable timeoutRunnable = () -> {
                        try {
                            if (!dataFetcherCancellation.isCancelled()) {
                                Log.d("SettingsDatabaseView", "Safety timeout triggered to dismiss dialog");
                                dataFetcherCancellation.cancelOperations(); // Cancel operations on timeout
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "Operation timed out. Some data may not have been fetched.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("SettingsDatabaseView", "Error dismissing dialog on timeout: " + e.getMessage());
                        }
                    };
                    
                    // Post the timeout runnable
                    final long TIMEOUT_MS = 120000; // 2 minutes
                    mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);
                    
                    // Reset cancellation state before starting
                    dataFetcherCancellation.resetCancellationState();
                    
                    // Use the progress callback to update the dialog
                    dataFetcher.refreshAllData(new DataFetcher.ProgressCallback() {
                        @Override
                        public void onProgressUpdate(String progressText) {
                            runOnUiThread(() -> {
                                try {
                                    // Check if the dialog is still showing before updating
                                    if (progressDialog.isShowing() && !dataFetcherCancellation.isCancelled()) {
                                        Log.d("SettingsDatabaseView", "Progress update: " + progressText);
                                        progressDialog.updateProgressText(progressText);
                                    }
                                } catch (Exception e) {
                                    Log.e("SettingsDatabaseView", "Error updating progress: " + e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onComplete(boolean success, String message) {
                            // Remove the timeout runnable since we're done
                            mainHandler.removeCallbacks(timeoutRunnable);
                            
                            runOnUiThread(() -> {
                                try {
                                    Log.d("SettingsDatabaseView", "Data fetch completed with success=" + success + ": " + message);
                                    
                                    // Only show completion message if not cancelled
                                    if (!dataFetcherCancellation.isCancelled()) {
                                        // Dismiss the dialog if it's still showing
                                        if (progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }
                                        
                                        // Show a toast with the result
                                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                    }
                                } catch (Exception e) {
                                    Log.e("SettingsDatabaseView", "Error in onComplete: " + e.getMessage());
                                    
                                    // Try to dismiss the dialog again if there was an error
                                    try {
                                        if (progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }
                                    } catch (Exception ex) {
                                        Log.e("SettingsDatabaseView", "Error dismissing dialog: " + ex.getMessage());
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    // Remove any pending timeout
                    mainHandler.removeCallbacksAndMessages(null);
                    
                    runOnUiThread(() -> {
                        try {
                            // Dismiss the dialog if it's still showing
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            
                            // Show an error toast
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            
                            Log.e("SettingsDatabaseView", "Error in data fetch: " + e.getMessage());
                        } catch (Exception ex) {
                            Log.e("SettingsDatabaseView", "Error handling exception: " + ex.getMessage());
                        }
                    });
                }
            }).start();
            
            return true; // Return true to indicate the click was handled
        });
    }
}
