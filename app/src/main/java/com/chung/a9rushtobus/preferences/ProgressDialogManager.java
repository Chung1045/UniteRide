package com.chung.a9rushtobus.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chung.a9rushtobus.R;

/**
 * Helper class to manage a non-dismissable progress dialog
 */
public class ProgressDialogManager {
    private AlertDialog dialog;
    private TextView progressTextView;
    private OnCancelListener cancelListener;

    /**
     * Interface for handling dialog cancellation
     */
    public interface OnCancelListener {
        void onCancel();
    }

    /**
     * Creates and shows a non-dismissable progress dialog
     * 
     * @param context The context to use
     * @param title The title of the dialog
     * @param initialProgressText The initial progress text to display
     * @return The created ProgressDialogManager instance
     */
    public static ProgressDialogManager show(Context context, String title, String initialProgressText) {
        ProgressDialogManager manager = new ProgressDialogManager();
        manager.createAndShowDialog(context, title, initialProgressText);
        return manager;
    }

    /**
     * Sets a listener to be called when the cancel button is clicked
     * 
     * @param listener The listener to call
     */
    public void setOnCancelListener(OnCancelListener listener) {
        this.cancelListener = listener;
    }

    private void createAndShowDialog(Context context, String title, String initialProgressText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_progress, null);
        
        // Set the title if provided
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        if (title != null && !title.isEmpty()) {
            titleView.setText(title);
        }
        
        // Get reference to the progress text view
        progressTextView = dialogView.findViewById(R.id.progress_text);
        
        // Set initial progress text
        if (initialProgressText != null && !initialProgressText.isEmpty()) {
            progressTextView.setText(initialProgressText);
        }
        
        // Set up the cancel button
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            Log.d("ProgressDialogManager", "Cancel button clicked");
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
            dismiss();
        });
        
        // Create and show the dialog
        builder.setView(dialogView);
        builder.setCancelable(false); // Make it non-dismissable
        
        dialog = builder.create();
        dialog.show();
    }

    /**
     * Updates the progress text shown below the progress bar
     * 
     * @param progressText The new progress text to display
     */
    public void updateProgressText(String progressText) {
        if (progressTextView != null && progressText != null) {
            progressTextView.setText(progressText);
        }
    }

    /**
     * Dismisses the dialog
     */
    public void dismiss() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null; // Clear the reference to prevent memory leaks
            }
        } catch (Exception e) {
            // Handle any exceptions that might occur during dismissal
            // This can happen if the activity is no longer active
            Log.e("ProgressDialogManager", "Error dismissing dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if the dialog is currently showing
     * 
     * @return true if the dialog is showing, false otherwise
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
