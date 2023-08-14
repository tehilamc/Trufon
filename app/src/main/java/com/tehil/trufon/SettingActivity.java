package com.tehil.trufon;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {

    private static final int PICK_CONTACT_REQUEST = 1;
    private TextView contact1Txv;
    private TextView contact2Txv;
    private Button selectContact1Button;
    private Button selectContact2Button;
    private SharedPreferences mSharedPreferences;
    private RadioGroup radioGroupGender;
    private RadioButton radioButtonMale, radioButtonFemale;
    private String selectedGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initializeViews();
        setSavedContacts();
        setSavedGender();
        setSelectContact1ButtonClickListener();
        setSelectContact2ButtonClickListener();
        saveContacts();
    }

    // A method that initializes all variables
    private void initializeViews() {
        mSharedPreferences = getApplicationContext().getSharedPreferences("Settings", MODE_PRIVATE);
        contact1Txv = findViewById(R.id.contact1TxvId);
        contact2Txv  = findViewById(R.id.contact2TxvId);
        selectContact1Button = findViewById(R.id.selectContact1ButtonId);
        selectContact2Button = findViewById(R.id.selectContact2ButtonId);

        // Initialize views
        radioGroupGender = findViewById(R.id.radioGroupGenderId);
        radioButtonMale = findViewById(R.id.radioButtonMaleId);
        radioButtonFemale = findViewById(R.id.radioButtonFemaleId);

        // Retrieve the last selected gender from SharedPreferences
        selectedGender = mSharedPreferences.getString("selectedGender", "");
        if (selectedGender.equals("Male")) {
            radioButtonMale.setChecked(true);
        } else if (selectedGender.equals("Female")) {
            radioButtonFemale.setChecked(true);
        }
    }

    // A method that displays the selected contacts on the screen
    private void setSavedContacts() {
        String contact1 = mSharedPreferences.getString("contact1", "");
        String contact2 = mSharedPreferences.getString("contact2", "");
        contact1Txv.setText(contact1);
        contact2Txv.setText(contact2);
    }



    // A method that displays the gender selected by the user
    private void setSavedGender() {
        Button saveGenderButton = findViewById(R.id.saveGenderButtonId);

        saveGenderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check which RadioButton was selected
                int checkedId = radioGroupGender.getCheckedRadioButtonId();
                if (checkedId == R.id.radioButtonMaleId) {
                    // Male RadioButton selected
                    selectedGender = "Male";
                    saveSelectedGender(selectedGender);
                    Toast.makeText(SettingActivity.this, "Male selected", Toast.LENGTH_SHORT).show();
                } else if (checkedId == R.id.radioButtonFemaleId) {
                    // Female RadioButton selected
                    selectedGender = "Female";
                    saveSelectedGender(selectedGender);
                    Toast.makeText(SettingActivity.this, "Female selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // A method that checks if the user has permission to access contacts,if so, sends them to a method that can select the contacts they want,and if not, displays a permission request
    private void setSelectContact1ButtonClickListener() {
        selectContact1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted, proceed with contact selection
                    pickContact(1);
                } else {
                    // Permission is not granted, request permission
                    requestContactsPermission();
                }
            }
        });
    }

    // A method that checks if the user has permission to access contacts,if so, sends them to a method that can select the contacts they want,and if not, displays a permission request
    private void setSelectContact2ButtonClickListener() {
        selectContact2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted, proceed with contact selection
                    pickContact(2);
                } else {
                    // Permission is not granted, request permission
                    requestContactsPermission();
                }
            }
        });
    }

    // A method that saves the contacts in shared preferences and checks if they are empty. If empty, it displays a message to the user and asks them to enter contacts.
    private void saveContacts() {
        Button saveContactsButton = findViewById(R.id.saveContactsButton);
        saveContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String contact1 = contact1Txv.getText().toString();
                String contact2 = contact2Txv.getText().toString();

                // Check if any contact is empty
                if (contact1.isEmpty() || contact2.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please fill in all the contacts", Toast.LENGTH_SHORT).show();
                    return; // Stop further execution
                }

                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString("contact1", contact1);
                editor.putString("contact2", contact2);
                editor.apply();

                Toast.makeText(getApplicationContext(), "Contacts saved", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // A method that saves the gender selected by the user in shared preferences
    private void saveSelectedGender(String gender) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("selectedGender", gender);
        editor.apply();
    }

    // A method that displays the user's Contacts application
    private void pickContact(int requestCode) {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(pickContactIntent, requestCode);
    }



    // Permission request to access contacts
    private void requestContactsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
            // Permission rationale should be shown
            showPermissionRationale();
        } else {
            // Permission rationale should not be shown, request permission directly
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PICK_CONTACT_REQUEST);
        }
    }

    // A method that explains to the user why access to contacts is necessary
    private void showPermissionRationale() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(SettingActivity.this);
        dialog.setTitle("Contacts Permission");
        dialog.setMessage("This app needs the contacts permission to access your contacts!");

        dialog.setCancelable(false);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogBox, int id) {
                // Request permission
                ActivityCompat.requestPermissions(SettingActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, PICK_CONTACT_REQUEST);
                dialogBox.dismiss();
            }
        });
        dialog.show();
    }

    // Permission request to access contacts
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with contact selection
                if (requestCode == 1) {
                    pickContact(1);
                } else if (requestCode == 2) {
                    pickContact(2);
                }
            } else {
                // Permission denied, show an AlertDialog
                AlertDialog.Builder dialog = new AlertDialog.Builder(SettingActivity.this);
                dialog.setTitle("Contacts Permission Denied");
                dialog.setMessage("This app needs the contacts permission to access your contacts!\n\nAllow permission by clicking on Settings.");

                dialog.setCancelable(false);
                dialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        // Open app settings to enable permission
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        dialogBox.dismiss();
                    }
                });
                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        dialogBox.cancel();
                    }
                });
                dialog.show();
            }
        }
    }

    // A method that returns the details of the selected contact and updates the display of contacts
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1) { // Contact selected for contact1
                // Set the selected contact to contact1TextView
                Uri contactUri = data.getData();
                String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
                Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String name = cursor.getString(nameColumnIndex);
                    String number = cursor.getString(numberColumnIndex);
                    cursor.close();
                    contact1Txv.setText(name + " " + number);
                }
            } else if (requestCode == 2) { // Contact selected for contact2
                // Set the selected contact to contact2Txv
                Uri contactUri = data.getData();
                String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
                Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String name = cursor.getString(nameColumnIndex);
                    String number = cursor.getString(numberColumnIndex);
                    cursor.close();
                    contact2Txv.setText(name + " " + number);
                }
            }
        }
    }
}














