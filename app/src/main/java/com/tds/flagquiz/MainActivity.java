package com.tds.flagquiz;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MaterialButton startQuizButton;
    private ProgressBar progressBar; // To show loading state

    private boolean isBackPressedOnce = false;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper()); // final and initialized once
    private OnBackPressedCallback backPressedCallback;

    private DatabaseCopyHelper dbHelper;
    private ExecutorService databaseExecutor; // For background database operations

    // Flag to indicate if database setup was successful
    private boolean isDatabaseReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startQuizButton = findViewById(R.id.start_quiz_button);
        progressBar = findViewById(R.id.progressBar);

        // Disable button initially until DB is ready
        startQuizButton.setEnabled(false);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE); // Show progress
        }

        dbHelper = new DatabaseCopyHelper(this);
        databaseExecutor = Executors.newSingleThreadExecutor(); // Initialize executor

        initializeDatabase(); // Call the method to handle DB setup

        startQuizButton.setOnClickListener(v -> {
            if (isDatabaseReady) {
                startActivity(new Intent(MainActivity.this, QuizActivity.class));
                // Do not call finish() here if you want the "Press again to exit"
                // behavior to work from MainActivity after coming back from QuizActivity.
                // If MainActivity is just a splash/entry point, then finish() is okay.
                // For this example, let's assume it's not just a splash.
            } else {
                Toast.makeText(MainActivity.this, "Database is not ready. Please wait or try again.", Toast.LENGTH_LONG).show();
            }
        });

        setupBackPressedCallback();

    } // --------------------------------------------------------------------------------------------------

    private void initializeDatabase() {
        databaseExecutor.execute(() -> {
            try {
                Log.d(TAG, "Initializing database...");
                dbHelper.createAndOpenDatabase(); // This method now handles copy and open
                isDatabaseReady = true;
                Log.d(TAG, "Database initialized successfully.");

                mainThreadHandler.post(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    startQuizButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Database ready!", Toast.LENGTH_SHORT).show();
                });

            } catch (IOException | SQLException e) {
                isDatabaseReady = false;
                Log.e(TAG, "Error initializing database", e);
                mainThreadHandler.post(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    startQuizButton.setEnabled(false); // Keep button disabled or handle error
                    // Show a more persistent error, maybe a TextView or a Dialog
                    Toast.makeText(MainActivity.this, "Error initializing database: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // You might want to provide an option to retry or exit
                });
            }
            // Note: dbHelper.close() will be called in onDestroy of MainActivity
        });
    }

    private void setupBackPressedCallback() {
        backPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (isBackPressedOnce) {
                    // If pressed again within 2 seconds, call finish() to exit
                    // and remove the callback to allow default back navigation if activity is not finished
                    setEnabled(false); // Disable this callback
                    getOnBackPressedDispatcher().onBackPressed(); // Perform default back action (finishes activity)
                    // Alternatively, you could call finish() directly if that's the only desired behavior
                    // finish();
                } else {
                    isBackPressedOnce = true;
                    Toast.makeText(MainActivity.this, "Press again to exit", Toast.LENGTH_SHORT).show();
                    mainThreadHandler.postDelayed(() -> {
                        isBackPressedOnce = false;
                        // Re-enable the callback if the user didn't press back again in time.
                        // This is important if you want the "Press again to exit" logic
                        // to work consistently every time the user initiates it.
                        // However, if the activity is already finishing/finished,
                        // trying to re-enable might not be necessary or could lead to issues.
                        // Consider the lifecycle of your activity.
                        // if (getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) && !isEnabled()) {
                        //     setEnabled(true);
                        // }
                    }, 2000);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Shutdown executor
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
            Log.d(TAG, "DatabaseExecutor shutdown.");
        }

        // Close database helper
        if (dbHelper != null) {
            dbHelper.close(); // Ensure DatabaseCopyHelper's close() is called
            Log.d(TAG, "DatabaseCopyHelper closed.");
        }

        // Clean up handler messages
        // mainThreadHandler is final, no need to check for null, but removeCallbacksAndMessages is good practice
        mainThreadHandler.removeCallbacksAndMessages(null);


        // OnBackPressedCallback is lifecycle-aware and should be removed automatically
        // when the activity is destroyed if added with a LifecycleOwner.
        // Explicit removal:
        // if (backPressedCallback != null) {
        //     backPressedCallback.remove();
        // }
        Log.d(TAG, "onDestroy completed.");
    }

//    @Override
//    public void onBackPressed() {
//
//        if (isBackPressedOnce){
//            super.onBackPressed();
//            return;
//        }
//
//        Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
//        isBackPressedOnce = true;
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                isBackPressedOnce = false;
//            }
//        }, 2000);
//    }
}