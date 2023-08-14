package com.tehil.trufon;

import static android.content.ContentValues.TAG;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity
{
    private ListView listView;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("details_of_medication");
    private ArrayList<String> dataArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        listView = findViewById(R.id.list_view);
        DisplayingMedicationHistoryTable();
    }
    //A method that shows the history table of the medications that the person took, for each medication it shows the time when the reminder appeared according to the user's request,
    //The dates on which the medicine was taken and for each date whether the medicine was taken or not
    private void DisplayingMedicationHistoryTable() {
        // Create a custom adapter to populate the ListView
        ChangeListAdapter arrayAdapter = new ChangeListAdapter(this, dataArrayList);
        listView.setAdapter(arrayAdapter);

        if (!isNetworkAvailable()) {
            // There is no internet connection
            // Handle the absence of internet connection
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                collectionReference
                        .orderBy("name_medication", Query.Direction.ASCENDING)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException error) {
                                if (error != null) {
                                    Log.w(TAG, "Listen failed.", error);
                                    return;
                                }
                                dataArrayList.clear();
                                for (QueryDocumentSnapshot document : querySnapshot) {
                                    String nameMedicationShow = document.getString("name_medication");
                                    String timeShow = document.getString("time");

                                    String medicationText = "Medication: " + nameMedicationShow;
                                    String timeText = " | Time: " + timeShow;

                                    dataArrayList.add(medicationText + timeText);

                                    // Sort the medication entries by key (date) in ascending order
                                    List<String> dateFields = new ArrayList<>(document.getData().keySet());
                                    Collections.sort(dateFields);

                                    // Iterate over sorted medication entries
                                    for (String dateField : dateFields) {
                                        if (!dateField.equals("name_medication") && !dateField.equals("period_taking_medicine")) {
                                            String tookMedicine = (String) document.get(FieldPath.of(dateField, "took_medicine"));
                                            if (tookMedicine != null) {
                                                String dateText = "Date: " + dateField;
                                                String tookMedicineText = " | Took Medicine: " + tookMedicine;
                                                dataArrayList.add(dateText + tookMedicineText);
                                            }
                                        }
                                    }
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        arrayAdapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        });
            }
        });
        thread.start();
    }

    //check if have connect to internet
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

}
