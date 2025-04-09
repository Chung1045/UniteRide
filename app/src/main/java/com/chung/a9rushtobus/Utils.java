package com.chung.a9rushtobus;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Utils {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123;
    private static final String CHANNEL_ID = "dummy_channel";
    private final Activity activity;
    private final View view;
    private ToastUtils toastUtils;
    private SnackBarUtils snackBarUtils;
    private NotificationUtils notificationUtils;
    private DialogUtils dialogUtils;
    private IntentUtils intentUtils;
    private TimeUtils timeUtils;
    private static TextStyleUtils textStyleUtils;

    public Utils(Activity a, View v, Context c){
        this.activity = a;
        this.view = v;

        utilsInit(a, v, c);
    }

    public void utilsInit(Activity a , View v, Context c){
        toastUtils = new ToastUtils();
        snackBarUtils = new SnackBarUtils(v);
        notificationUtils = new NotificationUtils(a, v);
        dialogUtils = new DialogUtils(c);
        intentUtils = new IntentUtils(c);
        timeUtils = new TimeUtils();
        textStyleUtils = new TextStyleUtils(c);
    }

    // Functions
    public void showToastMessage(String message){
        toastUtils.showMessage(activity, message);
    }

    public void showSnackBarMessage(String message){
        snackBarUtils.setSnackBarMessage(message);
    }

    public void showActionSnackBar(String message, String buttonText){
        snackBarUtils.setActionSnackbar(message, buttonText);
    }

    public void showNotifications(String message){
        notificationUtils.showNotif(activity, "This is a notification", view);
    }

    public void showSimpleDialog(String message, int titleResourceID){
        dialogUtils.setSimpleDialog(message, titleResourceID);
    }

    public void showYesNoDialog(String message, int titleResourceID){
        dialogUtils.setYesNoDialog(message, titleResourceID );
    }

    public void startUrlIntent(String url){
        intentUtils.urlIntent(url);
    }

    public String getTimeDifference(String isoDateTime){
        return timeUtils.getTimeDifference(isoDateTime);
    }

    public String parseTime(String isoDateTime){
        return timeUtils.parseTime(isoDateTime);
    }

    public static class TimeUtils {
        public String getTimeDifference(String isoDateTime) {
            try {
                // Use ZonedDateTime to parse ISO date with timezone information
                java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(isoDateTime);
                // Convert to LocalDateTime in system default timezone for comparison
                java.time.LocalDateTime dateTime = zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
                Duration duration = Duration.between(LocalDateTime.now(), dateTime);
                return String.valueOf(duration.toMinutes());
            } catch (Exception e) {

                return "N/A";
            }
        }

        public String parseTime(String isoDateTime) {
            try {
                // Parse the ISO date-time string
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(isoDateTime);
                // Convert to LocalTime in system default timezone
                LocalTime time = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalTime();
                // Format time without seconds
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                return time.format(formatter);
            } catch (Exception e) {
                return "N/A";
            }
        }
    }

    //Classes to handel the functions
    public class ToastUtils {
        public void showMessage(Context context, String message){
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, message, duration);
            toast.show();
        }
    }

    public class SnackBarUtils {

        private final View view;

        public SnackBarUtils(View view){
            this.view = view;
        }

        public void setSnackBarMessage(String message) {
            Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        }

        public void setActionSnackbar(String message, String buttonText) {
            Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    .setAction(buttonText, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showToastMessage("Clicked");
                        }
                    });
            snackbar.show();
        }
    }

    public class NotificationUtils {

        private Activity activity;
        private View view;
        private NotificationManager notificationManager;
        private SnackBarUtils snackBarUtils;

        public NotificationUtils(Activity a, View v){
            this.activity = a;
            this.view = v;
            this.notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            snackBarUtils = new SnackBarUtils(view);
            createNotificationChannel();

        }

        private void createNotificationChannel() {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Important Notification Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("This notification contains important announcement, etc.");
            notificationManager.createNotificationChannel(channel);
        }

        public void showNotif(Context context, String message, View view) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_notifications_active_24)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(1, builder.build());
            } else {
                showSnackBarMessage("Notification permission is denied, as posting notification in or above Android 13 requires permission");
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

    }

    public class DialogUtils{

        private Context context;

        public DialogUtils(Context c){
            this.context = c;
        }

        public void setSimpleDialog(String message, int titleResourceId) {
            AlertDialog.Builder dialogBuilder;

            dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setMessage(message).setTitle(titleResourceId);
            showDialog(dialogBuilder);
        }

        public void setYesNoDialog(String message, int titleResourceId) {
            // Legacy (Material 2)
            //AlertDialog.Builder dialogBuilder;

            //dialogBuilder = new AlertDialog.Builder(context);
            //dialogBuilder.setMessage(message)
            //        .setTitle(titleResourceId)
            //        .setCancelable(false)
            //        .setPositiveButton("Yes", null)  // Pass null for no action
            //        .setNegativeButton("No", null);  // Pass null for no action

            new MaterialAlertDialogBuilder(context).setTitle(titleResourceId).setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Yes", null)
                    .setNegativeButton("No", null).show();

            //showDialog(dialogBuilder);
        }

        public void showDialog(AlertDialog.Builder alertBlueprint){
            AlertDialog alert = alertBlueprint.create();
            alert.show();
        }

    }

    private class IntentUtils{

        private Context context;

        public IntentUtils(Context c){
            context = c;
        }

        public void urlIntent(String url){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        }

    }

    public static class TextStyleUtils {
        private Context context;

        public TextStyleUtils(Context context) {
            this.context = context;
        }

        /**
         * Apply bold text style to the entire application
         * @param isBold true to enable bold text, false to use normal text
         */
        public static void applyBoldTextStyle(boolean isBold) {
            // Update the shared preference
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_ACCESS_BOLD_TEXT, isBold).apply();
            
            // The actual style change will be applied when activities are recreated
            // or when the app is restarted
        }
    }
}
