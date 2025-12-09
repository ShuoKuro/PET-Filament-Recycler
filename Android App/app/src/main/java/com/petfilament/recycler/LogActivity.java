package com.petfilament.recycler;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * LogActivity class displays the Bluetooth logs.
 */
public class LogActivity extends AppCompatActivity {

    /**
     * RecyclerView for displaying logs.
     */
    private RecyclerView recyclerViewLogs;

    /**
     * Button to go back.
     */
    private Button buttonBack;

    /**
     * Adapter for the logs RecyclerView.
     */
    private LogsAdapter logsAdapter;

    /**
     * Database helper for accessing logs.
     */
    private DatabaseHelper databaseHelper;

    /**
     * onCreate method initializes UI, database, RecyclerView, and loads logs.
     * @param savedInstanceState Saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        // Initialize view components
        recyclerViewLogs = findViewById(R.id.recyclerview_logs);
        buttonBack = findViewById(R.id.button_back);

        // Initialize database
        databaseHelper = new DatabaseHelper(this);

        // Set up RecyclerView with linear layout, load all logs, set adapter
        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(this));
        ArrayList<DatabaseHelper.LogEntry> logs = databaseHelper.getAllLogs();
        logsAdapter = new LogsAdapter(logs);
        recyclerViewLogs.setAdapter(logsAdapter);

        // Back button listener to finish activity
        buttonBack.setOnClickListener(v -> finish());
    }
}