package com.petfilament.recycler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity class is the entry point of the application.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Button to navigate to control activity.
     */
    private Button buttonNavigate;

    /**
     * onCreate method initializes UI and sets navigation listener.
     * @param savedInstanceState Saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI component - navigation button
        buttonNavigate = findViewById(R.id.button_navigate);

        // Navigation button click listener
        buttonNavigate.setOnClickListener(v -> {
            // Navigate to ControlActivity
            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            startActivity(intent);

            // Optional: add transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    /**
     * onResume method for any cleanup or initialization on resume.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // When returning to MainActivity, can perform cleanup or init
    }
}