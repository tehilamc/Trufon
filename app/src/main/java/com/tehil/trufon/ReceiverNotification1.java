package com.tehil.trufon;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.Locale;
public class ReceiverNotification1 extends BroadcastReceiver {
    public static final String CHANNEL_ID = "NotificationChannel";
    private static final String CHANNEL_NAME = "notification";

    private static final String APPROVE_ACTION = "approve";
    private static final String IGNORE_ACTION = "ignore";


    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId1 = intent.getIntExtra("notification_id", 0);
        String documentId = intent.getStringExtra("documentId");
        notificationCheckingCorrectnessPrinting( notificationId1, documentId,context);
    }

    //check if have connect to internet
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    //A method that gets me today's current date
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);
        return dateFormat.format(new Date());
    }

    //A method that handles the display of alerts for medication reminders.It checks if a field called "notification_number" exists in the document for the current date. If it exists and it counts, it continues to the next steps.
    //If periodTakingMedicine1 (parsed from a field named "period_taking_medicine") is 0, it means that the duration of taking the medicine is over, so it cancels the alarm associated with the message using the AlarmManager and cancel() method.If periodTakingMedicine1 is greater than 0, it decrements the periodTakingMedicine1 value by 1 and updates the "period_taking_medicine" field in the document using the update() method.
    //After the document is updated, it calls the showNotification() method to display the notification.
    private void notificationCheckingCorrectnessPrinting(int notificationId1, String documentId, Context context) {
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db.collection("details_of_medication")
                        .document(documentId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {
                                    // Retrieves the data for the medicine I need
                                    String nameMedication = documentSnapshot.getString("name_medication");
                                    String NotificationOneDay = documentSnapshot.getString("NotificationOneDay");

                                    int counterFromSnapshot = documentSnapshot.getLong("counter").intValue();
                                    int periodTakingMedicine1 = Integer.parseInt(documentSnapshot.getString("period_taking_medicine"));
                                    String currentDate = getCurrentDate();
                                    Object notificationNumberField = documentSnapshot.get(FieldPath.of(currentDate, "notification_number"));

                                    if (notificationNumberField != null && notificationNumberField instanceof Number) {
                                        // If the duration of taking the drug is zero, cancel the alarm
                                        if (periodTakingMedicine1 == 0) {
                                            Intent alarmIntent = new Intent(context, ReceiverNotification1.class);
                                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId1, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
                                            context.getSystemService(AlarmManager.class).cancel(pendingIntent);
                                        }
                                        // If the period of taking the medicine is greater than zero
                                        else if (periodTakingMedicine1 > 0) {
                                            periodTakingMedicine1--;
                                            final int finalPeriodTakingMedicine = periodTakingMedicine1;
                                            db.collection("details_of_medication")
                                                    .document(documentId)
                                                    .update("period_taking_medicine", String.valueOf(finalPeriodTakingMedicine))
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            showNotification(db, documentId, context, notificationId1, nameMedication, NotificationOneDay);
                                                            updateCounter(documentId, db, counterFromSnapshot, context, notificationId1, nameMedication);
                                                        }
                                                    });
                                        }
                                    }
                                }
                            }
                        });
            }
        });
        thread.start();
        CreateChannelNotification(context);
    }
    // Displays a notification for medication reminders.
    private void showNotification(FirebaseFirestore db, String documentId,Context context,int notificationId1,String nameMedication, String NotificationOneDay) {
        Log.d("mylog", "showNotification: ");
        updateCounterDaysPast(documentId,db,context,NotificationOneDay);
        Intent approveIntent = new Intent(context, ReceiverUpdateFirestore.class);
        approveIntent.setAction(APPROVE_ACTION);
        approveIntent.putExtra("documentId", documentId);
        approveIntent.putExtra("notificationId", notificationId1);
        PendingIntent approvePendingIntent = PendingIntent.getBroadcast(
                context, notificationId1, approveIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent ignoreIntent = new Intent(context, ReceiverUpdateFirestore.class);
        ignoreIntent.setAction(IGNORE_ACTION);
        ignoreIntent.putExtra("documentId", documentId);
        ignoreIntent.putExtra("notificationId", notificationId1);
        PendingIntent ignorePendingIntent = PendingIntent.getBroadcast(
                context, notificationId1, ignoreIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_notify)
                .setContentTitle("Trufon")
                .setAutoCancel(true)
                .setContentText("תזכורת עבור נטילת התרופה " + nameMedication)
                .addAction(0, "Approve", approvePendingIntent)
                .addAction(0, "Ignore", ignorePendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Use a unique notificationId for each notification
        notificationManager.notify(notificationId1, notification);
    }


    // Create the notification channel (required for Android 8.0 and higher)
    private void CreateChannelNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
    //Changes the change that saves whether it is the first day for the reminder and increases it so that if it is not the first day it will print a notification that does not go through the database and return the notation according to the alarm
    private void updateCounterDaysPast(String documentId, FirebaseFirestore db, Context context, String NotificationOneDay) {
        Log.d("mylog", "updateCounterDaysPast: ");
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db.collection("details_of_medication")
                        .document(documentId)
                        .update("NotificationOneDay", NotificationOneDay + 1)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("mylog", "Counter updated successfully");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("mylog", "Failed to update counter: " + e.getMessage());
                            }
                        });
            }
        });

        thread.start();
    }

    //A method that updates the notification count variable in a database, every time a notification is displayed on the screen it updates the variable by +1 and then goes to a method that will check if an action happened on the notification or not
    private void updateCounter(String documentId, FirebaseFirestore db, int counterFromSnapshot, Context context,int notificationId1, String name) {
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db.collection("details_of_medication")
                        .document(documentId)
                        .update("counter", counterFromSnapshot + 1)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("mylog", "Counter updated successfully");
                                checkNoComment(documentId, db, context, notificationId1, name);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("mylog", "Failed to update counter: " + e.getMessage());
                            }
                        });
            }
        });

        thread.start();

    }

    //A method that checks whether there is a response to the notification from the user,
    private void checkNoComment(String documentId, FirebaseFirestore db, Context context, int notificationId1, String name ) {
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize the handler and runnable in your setup or initialization code
        Handler handler = new Handler();
        Runnable timerRunnable = new Runnable() {
            boolean conditionMet = false; // Flag to track if the condition has been met,Setting a flag that will help us track that it won't do the actions if we don't click any button in the notification more than once for a certain notification

            @Override
            public void run() {
                if (conditionMet) {
                    return; // Exit if the condition has already been met
                }

                // Move the database operation to a separate thread
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        db.collection("details_of_medication")
                                .document(documentId)
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        String currentDate = getCurrentDate();
                                        Object notificationNumberField = documentSnapshot.get(FieldPath.of(currentDate, "notification_number"));
                                        Number notificationNumber = (Number) notificationNumberField;
                                        int notificationNumberInt = notificationNumber.intValue();
                                        int counterData = documentSnapshot.getLong("counter").intValue();
                                        String boleeee = documentSnapshot.getString("BooleanActionPress");
                                        //If there was no response to the notification
                                        if (boleeee.equals("false")) {
                                            if (notificationNumberInt == 0 && counterData == 1) {
                                                sendSMS(context, name);
                                                updateCounterToZero(documentId, db, context);
                                                conditionMet = true; // Set the flag to true to indicate that the condition has been met
                                                context.getSystemService(NotificationManager.class).cancel(notificationId1);
                                            }
                                        } else {
                                            updateCounterToZero(documentId, db, context);
                                            updateClickAction(db,documentId,context);

                                        }
                                    }
                                });
                    }
                });

                // Start the thread
                thread.start();
            }
        };

    // Delay the execution of the runnable for one minute
        handler.postDelayed(timerRunnable, 300000); // 60000 milliseconds = 1 minute

    }

    //A method that updates the notification count variable in the database to zero after waiting a minute to check if there is a response from the user to the notification or not, in any case it updates it to zero for the following days of taking the drug
    private void updateCounterToZero(String documentId,FirebaseFirestore db, Context context ) {
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        // Move the database update operation to a separate thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db.collection("details_of_medication")
                        .document(documentId)
                        .update("counter", 0)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("mylog", "Counter updated successfully");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("mylog", "Failed to update counter: " + e.getMessage());
                            }
                        });
            }
        });

        // Start the thread
        thread.start();

    }

    //A method that updates in a database the variable that saves us whether we click on any button in the notification or not, it updates it after it detects that there was a click and becomes true, it updates it to false for the rest of the days of taking the medicine
    private void updateClickAction(FirebaseFirestore db, String documentId, Context context) {
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db.collection("details_of_medication")
                        .document(documentId)
                        .update("BooleanActionPress", "false")
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("mylog", "Counter updated successfully");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("mylog", "Failed to update counter: " + e.getMessage());
                            }
                        });
            }
        });

        thread.start();
    }

    //A method that pressures the contacts from the shaer prefernce and sent them a message to remind a friend to take the medicine
    private void sendSMS(Context context, String name) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String savedContact1 = sharedPreferences.getString("contact1", "");
        String savedContact2 = sharedPreferences.getString("contact2", "");

        if (savedContact1.isEmpty() || savedContact2.isEmpty()) {
            // Handle the case where there are not enough contacts available
            Toast.makeText(context, "Please add at least 2 contacts.", Toast.LENGTH_SHORT).show();
            return;
        }
        String phoneNumber1 = savedContact1.replaceAll("[^\\d+]", "");  // Remove non-digit characters from the saved contact
        String phoneNumber2 = savedContact2.replaceAll("[^\\d+]", "");  // Remove non-digit characters from the saved contact
        String[] contacts = {phoneNumber1, phoneNumber2};
        String message = wordingMessage(sharedPreferences,name);
        // Check if the SEND_SMS permission is granted
        SmsManager smsManager = SmsManager.getDefault();
        for (String contact : contacts) {
            smsManager.sendTextMessage(contact, null, message, null, null);
        }
    }

    //A method that formulates the message to contacts according to the gender selected in the settings
    private String wordingMessage(SharedPreferences sharedPreferences, String name ) {
        String gender = sharedPreferences.getString("selectedGender", "");
        if (gender.equalsIgnoreCase("Male")) {
            return "Hi sir!\nJust a friendly reminder, please remind your friend to take medicine "+name+" as soon as possible. It is essential to his health.\nThank you ";
        } else {
            return "Hi madam!\nJust a friendly reminder, please remind your friend to take medicine "+name+" as soon as possible. It is essential to his health.\nThank you ";
        }
    }

}




