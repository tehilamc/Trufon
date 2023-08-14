package com.tehil.trufon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class ReceiverNotification2 extends BroadcastReceiver {
    private static final String CHANNEL_NAME = "notification";
    public static final String CHANNEL_ID = "NotificationChannel";
    String nameOfMedication;
    private Handler handler;
    private Runnable runnable;

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationIdNotification2 = intent.getIntExtra("notificationId2", 0);
        ShowNotification2(context, intent, notificationIdNotification2);
        startTimer(context, intent, notificationIdNotification2);
    }

    // A method that shows the user a second notification for taking the medicine and according to the user's click on the notification it is sent to receiver2 and there it acts according to what was clicked
    private void ShowNotification2(Context context, Intent intent, int notificationIdNotification2) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        nameOfMedication = intent.getStringExtra("nameMedication");
        String documentId = intent.getStringExtra("documentId");

        // Create the Intent for the "Approve" action
        Intent approveIntent = new Intent(context, ReceiverUpdateFirestore.class);
        approveIntent.setAction("approve"); // Replace "your_action" with your desired action
        approveIntent.putExtra("documentId", documentId);
        approveIntent.putExtra("notificationIdNotification2", notificationIdNotification2);
        PendingIntent approvePendingIntent = PendingIntent.getBroadcast(
                context, notificationIdNotification2, approveIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create the Intent for the "IGNORE" action
        Intent ignoreIntent = new Intent(context, ReceiverUpdateFirestore.class);
        ignoreIntent.setAction("ignore"); // Replace "your_action" with your desired action
        ignoreIntent.putExtra("documentId", documentId);
        ignoreIntent.putExtra("notificationIdNotification2", notificationIdNotification2);
        PendingIntent ignorePendingIntent = PendingIntent.getBroadcast(
                context, notificationIdNotification2, ignoreIntent, PendingIntent.FLAG_IMMUTABLE);

        // 2. Create Notification Channel (JUST ONCE!)
        NotificationChannel notificationChannel = new NotificationChannel(
                CHANNEL_ID, // Constant for Channel ID
                CHANNEL_NAME, // Constant for Channel NAME
                NotificationManager.IMPORTANCE_DEFAULT);

        notificationManager.createNotificationChannel(notificationChannel);

        // 3. Create & show the Notification (Every time you want to show notification)
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_notify)
                .setContentTitle("Trufon")
                .setContentText("זוהי תזכורת 2 עבור "  + nameOfMedication)
                .addAction(0, "Approve", approvePendingIntent)
                .addAction(0, "Ignore", ignorePendingIntent)
                .build();

        notificationManager.notify(notificationIdNotification2, notification);
    }

    private void startTimer(final Context context, final Intent intent, final int notificationIdNotification2) {
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                // Check if the notification was clicked
                boolean isNotificationClicked = isNotificationClicked(context, notificationIdNotification2);
                if (!isNotificationClicked) {
                    sendSMS(context, nameOfMedication);
                    context.getSystemService(NotificationManager.class).cancel(notificationIdNotification2);

                }
            }
        };

        handler.postDelayed(runnable, 300 * 1000);
    }

    // A method to check if the notification was clicked
    private boolean isNotificationClicked(Context context, int notificationId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NotificationClicks", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(String.valueOf(notificationId), false);
    }



    // A method that pressures the contacts from the share preference and sent them a message to remind a friend to take the medicine
    private void sendSMS(Context context, String nameOfMedication) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String savedContact1 = sharedPreferences.getString("contact1", "");
        String savedContact2 = sharedPreferences.getString("contact2", "");
        if (savedContact1.isEmpty() || savedContact2.isEmpty()) {
            // Handle the case where there are not enough contacts available
            Toast.makeText(context, "Please add at least 2 contacts.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("mylog", "sendSMS: savedContact1"+savedContact1);
        Log.d("mylog", "sendSMS: savedContact1"+savedContact2);

        String phoneNumber1 = savedContact1.replaceAll("[^\\d+]", "");  // Remove non-digit characters from the saved contact
        String phoneNumber2 = savedContact2.replaceAll("[^\\d+]", "");  // Remove non-digit characters from the saved contact
        String[] contacts = {phoneNumber1, phoneNumber2};
        String message = wordingMessage(sharedPreferences, nameOfMedication);
        // Check if the SEND_SMS permission is granted
        SmsManager smsManager = SmsManager.getDefault();
        for (String contact : contacts) {
            smsManager.sendTextMessage(contact, null, message, null, null);
        }
    }

    // A method that formulates the message to contacts according to the gender selected in the settings
    private String wordingMessage(SharedPreferences sharedPreferences, String nameOfMedication) {
        String gender = sharedPreferences.getString("selectedGender", "");
        if (gender.equalsIgnoreCase("Male")) {
            return "Hi sir!\nJust a friendly reminder, please remind your friend to take medicine  "+ nameOfMedication +" as soon as possible. It is essential to his health.\nThank you " ;
        } else {
            return "Hi madam!\nJust a friendly reminder, please remind your friend to take medicine  "+ nameOfMedication +" as soon as possible. It is essential to his health.\nThank you ";
        }
    }
}
