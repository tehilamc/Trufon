package com.tehil.trufon;

import com.google.firebase.firestore.FieldPath;
import android.app.AlarmManager;
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
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.Locale;

public class ReceiverUpdateFirestore extends BroadcastReceiver {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static final String APPROVE_ACTION = "approve";
    public static final String IGNORE_ACTION = "ignore";
    private static final String TAG = "ReceiverUpdateFirestore";

    String action;

    DocumentReference docRef;
    String documentId;

    @Override
    public void onReceive(Context context, Intent intent) {
        documentId = intent.getStringExtra("documentId");
        docRef = db.collection("details_of_medication").document(documentId);
        actionOfButton(intent, documentId, context);
    }

    //A method that checks the actions of the notifications and operates according to the type of action
    private void actionOfButton(Intent intent, String documentId, Context context) {
        int notificationId1 = intent.getIntExtra("notificationId", 0);
        int notificationId2 = intent.getIntExtra("notificationIdNotification2",0);
        if (intent != null) {
            action = intent.getAction();
            if (action != null) {
                //If the clicked action is confirmation, then the variable of taking the medicine is updated to "yes", in addition, it is updated in the database that an action has indeed taken place on the notification and the notification is removed from the screen and also updates the amount of notifications in a database
                if (action.equals(APPROVE_ACTION))
                {
                    updateActionHappened(db,documentId,context);
                    updateTakingInFirebase(documentId ,context);
                    // Update the notification number in Firestore
                    updateNotificationNumber(db, documentId, context, notificationId1,notificationId2);
                    setNotificationClicked(context, notificationId2, true);
                    context.getSystemService(NotificationManager.class).cancel(notificationId1);

                }
                //If the clicked action is a rejection, then the database is updated that an action has indeed taken place on the notification, the notification is lifted from the screen, and the number of notifications for that day is updated in the database
                else if (action.equals(IGNORE_ACTION)) {
                    updateActionHappened(db,documentId,context);
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                         context.getSystemService(NotificationManager.class).cancel(notificationId1);
                    }
                    // Update the notification number in Firestore
                    updateNotificationNumber(db, documentId, context, notificationId1,notificationId2);
                    setNotificationClicked(context, notificationId2, true);

                }
            }
        }
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


    //A method that updates a database that an action has indeed occurred on the notification and updates the variable "BooleanActionPress" with a value of true
    private void updateActionHappened(FirebaseFirestore db, String documentId, Context context) {

        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        String boolee="true";
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db.collection("details_of_medication")
                        .document(documentId)
                        .update("BooleanActionPress", boolee)
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


// A method that updates the number of clicks by the person on the notifications he received that day and performs actions accordingly
private void updateNotificationNumber(FirebaseFirestore db, String documentId, Context context, int notificationId1, int notificationId2) {
    if (!isNetworkAvailable(context)) {
        // There is no internet connection
        // Handle the absence of internet connection
        Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
        return;
    }
    String currentDate = getCurrentDate();
    DocumentReference documentRef = db.collection("details_of_medication").document(documentId);
    documentRef.get()
            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    handleDocumentSnapshot(documentSnapshot, documentRef, currentDate, context, notificationId1, notificationId2);
                }
            });
}

    // Handle the document snapshot
    private void handleDocumentSnapshot(DocumentSnapshot documentSnapshot, DocumentReference documentRef, String currentDate, Context context, int notificationId1, int notificationId2) {
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        if (documentSnapshot.exists()) {
            if (documentSnapshot.contains(FieldPath.of(currentDate))) {
                documentRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot nestedDocumentSnapshot) {
                                handleNestedDocumentSnapshot(nestedDocumentSnapshot, documentRef, currentDate, context, notificationId1, notificationId2);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Error retrieving nested document
                                Log.d("mylog", "Error retrieving nested document");
                            }
                        });
            }
            else {
                // Specific date field does not exist
                Log.d("mylog", "Field for current date does not exist");
            }
        }
        else {
            // Document does not exist
            Log.d("mylog", "Document does not exist");
        }
    }

    // Handle the nested document snapshot
    private void handleNestedDocumentSnapshot(DocumentSnapshot nestedDocumentSnapshot, DocumentReference documentRef, String currentDate, Context context, int notificationId1, int notificationId2) {
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nestedDocumentSnapshot.exists()) {
            documentRef.update(FieldPath.of(currentDate, "notification_number"), FieldValue.increment(1))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            String nameOfMedication = nestedDocumentSnapshot.getString("name_medication");
                            String notification_number = nestedDocumentSnapshot.get(FieldPath.of(currentDate, "notification_number")).toString();
                            int notificationNumber = Integer.parseInt(notification_number);
                            final int finalNotificationNumber = notificationNumber + 1;
                            int notificationIdOfNotification2 = (int) System.currentTimeMillis();
                            final String action1 = action;

                            if (finalNotificationNumber == 1 && action1.equals(IGNORE_ACTION)) {
                                setOneTimeAlert(context, nameOfMedication, notificationIdOfNotification2, documentId);
                            } else if (finalNotificationNumber == 2 && action1.equals(APPROVE_ACTION)) {
                                cancelNotification(context, notificationId2);
                                updateTakingInFirebase(documentId, context);
                            } else if (finalNotificationNumber == 2 && action1.equals(IGNORE_ACTION)) {
                                cancelNotification(context, notificationId2);
                                sendSMS(context, nameOfMedication);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                        }
                    });
        }
    }

    // Set a one-time alert
    private void setOneTimeAlert(Context context, String nameOfMedication, int notificationIdOfNotification2, String documentId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, ReceiverNotification2.class);
        alarmIntent.putExtra("nameMedication", nameOfMedication);
        alarmIntent.putExtra("notificationId2", notificationIdOfNotification2);
        alarmIntent.putExtra("documentId", documentId);

        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, notificationIdOfNotification2, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
        long triggerTimeMS = System.currentTimeMillis() + 1800 * 1000;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMS, alarmPendingIntent);
        }
    }
    // Cancel a notification
    private void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.cancel(notificationId);
    }

    // Update the "taking" status in Firebase
    private void setNotificationClicked(Context context, int notificationId, boolean isClicked) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NotificationClicks", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(String.valueOf(notificationId), isClicked);
        editor.apply();
    }
    //A method that pressures the contacts from the shaer prefernce and sent them a message to remind a friend to take the medicine
    private void sendSMS(Context context,String name) {
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
    private String wordingMessage(SharedPreferences sharedPreferences, String name) {
        String gender = sharedPreferences.getString("selectedGender", "");
        if (gender.equalsIgnoreCase("Male")) {
            return "Hi sir!\nJust a friendly reminder, please remind your friend to take medicine "+ name+" as soon as possible. It is essential to his health.\nThank you ";
        } else  {
            return "Hi madam!\nJust a friendly reminder, please remind your friend to take medicine "+ name+ " as soon as possible. It is essential to his health.\nThank you ";
        }
    }

    //A method that gets me today's current date
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);
        return dateFormat.format(new Date());
    }

    //A method that updates the firestore if we click OK in a notification that updates that it did take on the same day
    private void updateTakingInFirebase(String documentId,Context context) {
        if (!isNetworkAvailable(context)) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        // Log.d(TAG, "documentId in updateInFirebase: " + documentId);
        if (documentId != null) {
            DocumentReference docRef = db.collection("details_of_medication").document(documentId);

            // Get the current date
            String currentDate = getCurrentDate();

            // Build the field path using FieldPath
            FieldPath fieldPath = FieldPath.of(currentDate, "took_medicine");

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    docRef.update(fieldPath, "YES")
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Document successfully updated
                                    Log.d(TAG, "Document updated successfully");

                                    // Handle the updated field as needed
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Error updating document
                                    Log.w(TAG, "Error updating document", e);

                                }
                            });
                }
            });

            thread.start();
        }
    }
}