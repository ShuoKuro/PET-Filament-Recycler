package com.petfilament.recycler;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LogActivity extends AppCompatActivity {

    private RecyclerView recyclerViewLogs;
    private Button buttonBack;
    private LogsAdapter logsAdapter;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        // Initialize views
        recyclerViewLogs = findViewById(R.id.recyclerview_logs);
        buttonBack = findViewById(R.id.button_back);

        // Initialize database
        databaseHelper = new DatabaseHelper(this);

        // Setup RecyclerView
        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(this));
        ArrayList<DatabaseHelper.LogEntry> logs = databaseHelper.getAllLogs();
        logsAdapter = new LogsAdapter(logs);
        recyclerViewLogs.setAdapter(logsAdapter);

        // Back button listener
        buttonBack.setOnClickListener(v -> finish());
    }
}