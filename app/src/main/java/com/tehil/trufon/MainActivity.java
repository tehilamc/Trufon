package com.tehil.trufon;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import static android.content.ContentValues.TAG;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.concurrent.TimeUnit;
import android.Manifest;

public class MainActivity extends AppCompatActivity {
    private TextView txvTime;

    private int hour;
    private int minute;
    String nameMedication, periodTakingMedicine;

    EditText edtMedication, edtPeriod;
    Button btnSave, btnHistory;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    String myString;
    String documentId;
    private Intent alarmIntent;
    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int SMS_PERMISSION_REQUEST_CODE = 1;

    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;
    private AlertDialog alertDialog;

    private String readSmsPermission = Manifest.permission.READ_SMS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        checkSmsPermission();
        selectTime();
        setButtonListeners();
        scheduleCheckRemedies();

    }

    //Initializations of variables we use
    private void initializeViews() {
        txvTime = findViewById(R.id.txvTimeID);
        edtMedication = findViewById(R.id.edtMedicationID);
        btnHistory = findViewById(R.id.btnHistoryID);
        edtPeriod = findViewById(R.id.edtPeriodID);
        btnSave = findViewById(R.id.btnSaveID);
    }

    //Checks if there is permission to access messages, if not displays the request message
    private void checkSmsPermission() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, proceed with contact selection
        } else {
            // Permission is not granted, request permission
            requestSmsPermission();
        }
    }

    //A method that if we press one of the buttons on the screen
    private void setButtonListeners() {
        //If we click on saving the data, it goes to the method of checking contacts and there is an expanded name on the method
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkContact();
            }
        });
        //If we click on the medication history button, it sends to the medication history page
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

    }

    //Display a message access request
    private void requestSmsPermission() {
        // Check if the permission is granted
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
            // Permission rationale should be shown
            showPermissionRationale();
        } else {
            // Permission rationale should not be shown, request permission directly
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PICK_CONTACT_REQUEST);
        }
    }

    //If the condition is true, it means the user has granted the SMS permission, and you can continue to access SMS-related functionality. If the condition is false, it means that the user has denied the SMS permission or the request has been canceled and sent to display a message
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, you can access SMS
            } else {
                // Permission denied, handle accordingly (e.g., disable SMS-related functionality)
                showPermissionDeniedDialog();
            }
        }
    }

    //A method of granting access to messages shows the user why this access is important
    private void showPermissionRationale() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("SMS Permission");
        dialog.setMessage("This app needs the SMS permission to receive SMS messages!");

        dialog.setCancelable(false);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogBox, int id) {
                // Request permission
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{readSmsPermission}, SMS_PERMISSION_REQUEST_CODE);
                dialogBox.dismiss();
            }
        });
        dialog.show();
    }

    //A dialog, if he did not confirm the permission, it shows him the dialog and it can direct him to the settings where he can change or cancel the dialog
    private void showPermissionDeniedDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle("SMS Permission Denied");
        dialogBuilder.setMessage("This app needs the SMS permission to receive SMS messages!\n\nAllow permission by clicking on Settings.");

        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogBox, int id) {
                // Open app settings to enable permission
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                dialogBox.dismiss();
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogBox, int id) {
                dialogBox.cancel();
                showPermissionRationale();
            }
        });

        // Store the reference to the AlertDialog to dismiss it later if needed
        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    //Checks if the contacts are on the settings page, if so requires him to go and fill in and if not saves the data in a database and checks for each letter if there are notifications that need to be bounced
    private void checkContact() {
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String savedContact1 = sharedPreferences.getString("contact1", "");
        String savedContact2 = sharedPreferences.getString("contact2", "");
        String gender = sharedPreferences.getString("selectedGender", "");
        if (savedContact1.isEmpty() || savedContact2.isEmpty()) {
            // Handle the case where there are empty contacts
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("Empty Contacts");
            dialog.setMessage("Please go to the settings and enter at least 2 contacts.");

            dialog.setCancelable(false);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogBox, int id) {
                    // Open app settings to enter contacts
                    Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                    startActivity(intent);
                    dialogBox.dismiss();
                }
            });
            dialog.show();
        }
        else if (gender.isEmpty()){
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("Empty Gender");
            dialog.setMessage("Please go to the settings and enter a gender.");

            dialog.setCancelable(false);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogBox, int id) {
                    // Open app settings to enter contacts
                    Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                    startActivity(intent);
                    dialogBox.dismiss();
                }
            });
            dialog.show();
        }
        else {
            // Contacts are not empty, proceed with saving the details
            saveDetails(new TaskCallback() {
                @Override
                public void onTaskComplete() {
                    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    myString = txvTime.getText().toString();
                    editor.putString("myKey", myString);
                    editor.apply();
                }
            });
        }
    }

    //The method is used to schedule the checkRemedies() method to run every minute by receiving the current time
    private void scheduleCheckRemedies() {
        // Get the current time
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // Calculate the delay until the next minute
        int delayMinutes = 1 - currentMinute % 1;
        long delayMillis = TimeUnit.MINUTES.toMillis(delayMinutes);

        // Schedule the checkRemedies method to run every minute
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkRemedies();
                scheduleCheckRemedies();
            }
        }, delayMillis);
    }



private void checkRemedies() {
    // Get the current time
    Calendar calendar = Calendar.getInstance();
    int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
    int currentMinute = calendar.get(Calendar.MINUTE);
    if (!isNetworkAvailable()) {
        // There is no internet connection
        // Handle the absence of internet connection
        Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        return;
    }

    // Format the current time as "HH:mm"
    String formattedTime = String.format(Locale.ENGLISH, "%02d:%02d", currentHour, currentMinute);
    Log.d(TAG, "checkRemedies: formattedTime "+ formattedTime);
    db.collection("details_of_medication")
         .whereGreaterThan("period_taking_medicine", "0")
                .whereEqualTo("time", formattedTime)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Process the query results
                    runOnUiThread(() -> {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            String documentId1 = documentSnapshot.getId();
                            String NotificationOneDay = documentSnapshot.getString("NotificationOneDay");
                            Log.d(TAG, "NotificationOneDay: NotificationOneDay"+NotificationOneDay);

                            String nameMedication1 = documentSnapshot.getString("name_medication");
                            String periodTakingMedicineStr = documentSnapshot.getString("period_taking_medicine");
                            Log.d(TAG, "checkRemedies:nameMedication1 "+ nameMedication1);
                            int periodTakingMedicine1 = 0;
                            try {
                                periodTakingMedicine1 = Integer.parseInt(periodTakingMedicineStr);
                            } catch (NumberFormatException e) {
                                continue; // Skip this document and proceed to the next one
                            }
                            // Check if this remedy has already been matched and if the period is greater than 0
                            if (periodTakingMedicine1 >= 0) {
                                boolean isMatched = checkRemedyMatched(documentId1); // Check if the remedy is already matched
                                if (!isMatched) {
                                    if(NotificationOneDay.equals("1")) {
                                        Log.d(TAG, "checkRemedies: enterrrrrrrrr");
                                        sendNotification(documentId1, nameMedication1, periodTakingMedicine1);
                                    }
                                }
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking remedies", e));
}

    // Check if a remedy is already matched
    private boolean checkRemedyMatched(String documentId) {
        return false;
    }

    //check if connect to internet
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    // Setting an alert to remind the person which will be activated once a day at the time he chose to remind him
    private void sendNotification(String documentId, String nameMedication, int periodTakingMedicine1) {
        Log.e(TAG, "sendNotification: ");
        alarmManager = getSystemService(AlarmManager.class);
        alarmIntent = new Intent(this, ReceiverNotification1.class);
        int notificationId = (int) System.currentTimeMillis();
        alarmIntent.putExtra("notification_id", notificationId);
        alarmIntent.putExtra("documentId", documentId);
        alarmIntent.putExtra("nameMedication", nameMedication);

        pendingIntent = PendingIntent.getBroadcast(this, notificationId, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
        alarmIntent.putExtra("periodTakingMedicine", periodTakingMedicine1);
        alarmIntent.putExtra("alarmPendingIntent", pendingIntent);
        alarmIntent.putExtra("alarmManager", pendingIntent.getService(this, 0, new Intent(), 0));

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String myString1 = sharedPreferences.getString("myKey", "default value");

        String[] timeParts = myString1.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long triggerTimeMS = calendar.getTimeInMillis();

        long repeatIntervalMS = 24 * 60 * 60 * 1000;  // fire repeating alarm every 24 hours

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTimeMS, repeatIntervalMS, pendingIntent);
    }

    //Saves the data in the firestore such as the name of the      drug, a count of notifications that have been displayed up to a certain moment, a variable that will save us whether the notification was clicked or not, the duration of the drug taking period and the time when the user wants the reminder,
    //In addition to each drug, it stores a date for each day according to the number of days of taking the drug, where each day will have additional settings
    private void saveDetails(TaskCallback callback) {
        nameMedication = edtMedication.getText().toString();
        periodTakingMedicine = edtPeriod.getText().toString();
        int counter=0;

        // Check if nameMedication or periodTakingMedicine is empty
        if (nameMedication.isEmpty() || periodTakingMedicine.isEmpty()) {
            // Display a toast message to the user
            Toast.makeText(getApplicationContext(), "Please fill in all fields", Toast.LENGTH_LONG).show();
            return; // Return without saving the data
        }

        if (!isNetworkAvailable()) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        String boole="false";
        String time = txvTime.getText().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("name_medication", nameMedication);
        data.put("counter", counter);
        data.put("NotificationOneDay", "1");
        data.put("BooleanActionPress", boole);
        data.put("period_taking_medicine", periodTakingMedicine);
        data.put("time", time); // Add the time to the data

        // Run the task on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.collection("details_of_medication")
                        .add(data)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                documentId = documentReference.getId();
                                Toast.makeText(getApplicationContext(), "Your details have been successfully saved!", Toast.LENGTH_LONG).show();

                                        // Save the day data as fields within the same document
                                        int period = Integer.parseInt(periodTakingMedicine);
                                        for (int i = 1; i <= period; i++) {
                                            saveDayData(i, documentId, 0, "NO");
                                        }
                                        callback.onTaskComplete(); // Notify task completion
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), "There was an error saving data, please check the internet.", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }).start();
    }

    //The method is used to save day-specific data in a Firestore database by updating a document with the specified documentId and adding the day data for the corresponding FormattedDate field. The appropriate date for the day.
    //A map called dayData is created to hold today's data.
    //The relevant data is added to the dayData map, including the documentId, notificationNumber
    private void saveDayData(int dayNumber, String documentId, int notificationNumber, String tookMedicine) {
        // Get the appropriate date for the day
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, dayNumber - 1); // Subtract 1 from dayNumber to get the correct offset
        Date currentDate = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);
        String formattedDate = dateFormat.format(currentDate);

        if (!isNetworkAvailable()) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the main document with the day data
        Map<String, Object> dayData = new HashMap<>();
        dayData.put("document_id", documentId);
        dayData.put("notification_number", notificationNumber);
        dayData.put("took_medicine", tookMedicine);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db.collection("details_of_medication").document(documentId)
                        .update(FieldPath.of(formattedDate), dayData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("mylog", "Day data added for day " + dayNumber);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("mylog", "Error adding day data for day " + dayNumber, e);

                            }
                        });
            }
        });
        thread.start();
    }

    //This initializes a Calendar instance to get the current time. and gives the option of choosing a time for the user
    private void selectTime() {
        // Get the current time
        final Calendar calendar = Calendar.getInstance();
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        // Set the text of the TextView to the current time
        updateTimeTextView(hour, minute);

        // Set an OnClickListener for the Button to show the time picker dialog
        Button timePickerButton = findViewById(R.id.btnTimeID);
        timePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minuteOfDay) {
                        // Update the hour and minute variables
                        hour = hourOfDay;
                        minute = minuteOfDay;
                        // Update the TextView with the selected time
                        updateTimeTextView(hour, minute);
                    }
                }, hour, minute, false);
                timePickerDialog.show();
            }
        });
    }

    //A method that displays the time selected by the user on the screen
    private void updateTimeTextView(int hour, int minute) {
        // Format the selected time as a String
        String timeString = String.format("%02d:%02d", hour, minute);
        txvTime.setText(timeString);
    }

    // Define the interface for the task callback
    private interface TaskCallback {
        void onTaskComplete();
    }

    //Three point menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem aboutMenu = menu.add("About");
        MenuItem settingsMenu = menu.add("Settings");
        MenuItem exitMenu = menu.add("Exit");

        aboutMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                aboutAlertDialog();
                return false;
            }
        });
        settingsMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                settingsAlertDialog();
                return false;
            }

        });
        exitMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                exitAlertDialog();
                return false;
            }
        });

        return true;
    }

    private void settingsAlertDialog() {
        Intent moveSettings = new Intent(MainActivity.this, SettingActivity.class);
        startActivity(moveSettings);
    }

    private void aboutAlertDialog() {
        String strDeviceOS = "Android OS " + Build.VERSION.RELEASE + " API " + Build.VERSION.SDK_INT;

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("About App");
        dialog.setMessage("\nMy First Android mobile App!" + "\n\n" + strDeviceOS + "\n\n" + "By TEHILA COHEN 2023."+"\n\n"+ "Date of Submission: 19.06.23.");

        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();   // close this dialog
            }
        });
        dialog.show();
    }

    private void exitAlertDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(R.drawable.icon_exit);
        dialog.setTitle("Exit App");
        dialog.setMessage("Are you sure ?");
        dialog.setCancelable(false);

        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                 finish();   // destroy this activity
            }
        });
        dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();   // close this dialog
            }
        });
        dialog.show();
    }


}