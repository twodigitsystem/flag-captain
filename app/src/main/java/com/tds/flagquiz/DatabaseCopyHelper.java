package com.tds.flagquiz;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseCopyHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseCopyHelper"; // For logging
    private static final String DB_NAME = "flagquizdb.db";
    private static final int DB_VERSION = 1; // Start with version 1

    private final Context myContext;
    private final String dbPath;
    private SQLiteDatabase myDataBase;

    public DatabaseCopyHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.myContext = context;
        // More robust way to get the database path
        this.dbPath = context.getDatabasePath(DB_NAME).getPath();
    }

    /**
     * Creates the database if it doesn't exist by copying from assets.
     * This method should be called before attempting to open or use the database.
     */
    public void createAndOpenDatabase() throws IOException, SQLException {
        boolean dbExist = checkDataBaseExists();

        if (!dbExist) {
            Log.i(TAG, "Database does not exist. Copying from assets...");
            // By calling this method, an empty database will be created into the default system path
            // of your application so we are gonna be able to overwrite that database with our database.
            // This also ensures the databases directory is created.
            try {
                getReadableDatabase(); // Create an empty DB that SQLiteOpenHelper knows about
                close(); // Close the empty DB before overwriting.
                // SQLiteOpenHelper opens a handle when getReadableDatabase or getWritableDatabase is called.
                // We need to close this handle before attempting to overwrite the file.
            } catch (SQLiteException e) {
                // This might happen if the disk is full or other low-level issues.
                throw new IOException("Failed to create empty database structure.", e);
            }


            try {
                copyDataBaseFromAssets();
                Log.i(TAG, "Database copied successfully.");
            } catch (IOException e) {
                // If copy fails, attempt to delete the partially created or empty database file
                // to avoid issues on next attempt.
                File dbFile = new File(dbPath);
                if (dbFile.exists()) {
                    dbFile.delete();
                }
                throw new IOException("Error copying database from assets: " + e.getMessage(), e);
            }
        } else {
            Log.i(TAG, "Database already exists.");
        }

        openDataBase(); // Open the actual database (either existing or newly copied)
    }

    /**
     * Check if the database already exists.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBaseExists() {
        File dbFile = new File(dbPath);
        return dbFile.exists();
    }

    /**
     * Copies your database from your local assets-folder to the application's
     * system folder.
     */
    private void copyDataBaseFromAssets() throws IOException {
        // Using try-with-resources for automatic stream closing
        try (InputStream myInput = myContext.getAssets().open(DB_NAME);
             OutputStream myOutput = new FileOutputStream(dbPath)) {

            byte[] buffer = new byte[4096]; // Increased buffer size
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            myOutput.flush();
        }
    }

    /**
     * Opens the database.
     * @throws SQLException if the database cannot be opened.
     */
    public void openDataBase() throws SQLException {
        try {
            myDataBase = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
            Log.i(TAG, "Database opened successfully at " + dbPath);
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed to open database at " + dbPath, e);
            throw new SQLException("Failed to open database: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void close() {
        if (myDataBase != null && myDataBase.isOpen()) {
            myDataBase.close();
            myDataBase = null; // Help GC
            Log.i(TAG, "Custom database instance closed.");
        }
        super.close(); // Important to call super.close()
    }

    /**
     * This method is called when the database is created for the first time.
     * Since we are copying a pre-existing database, this method will be called
     * by getReadableDatabase() if the DB file doesn't exist, creating an empty shell.
     * We don't need to do anything here as the actual schema is in the copied DB.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "SQLiteOpenHelper onCreate called (expected for initial empty DB creation by helper).");
        // No explicit table creation needed here as we copy a pre-populated database.
    }

    /**
     * This method is called when the database needs to be upgraded.
     * If you release a new version of your app with an updated database in assets,
     * you'll need to implement logic here to handle the upgrade (e.g., re-copy).
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "SQLiteOpenHelper onUpgrade called from version " + oldVersion + " to " + newVersion);
        if (oldVersion < newVersion) {
            // For a simple overwrite strategy if the DB in assets is newer:
            // 1. Delete the old database file.
            // 2. Call a method to re-copy from assets.
            // This is a basic approach. More complex migrations might be needed for user data.
            Log.w(TAG, "Database schema is outdated. Consider implementing upgrade logic to re-copy from assets or migrate data.");
            // Example:
            // File dbFile = new File(dbPath);
            // if (dbFile.exists()) {
            //     dbFile.delete();
            // }
            // try {
            //     copyDataBaseFromAssets();
            // } catch (IOException e) {
            //     Log.e(TAG, "Failed to re-copy database during upgrade.", e);
            // }
        }
    }

    /**
     * Called when the database has been opened.
     * You can override this to perform actions once the database is open,
     * such as enabling/disabling WAL or other settings.
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // db.enableWriteAheadLogging(); // Generally recommended for performance [2]
        db.disableWriteAheadLogging(); // Your original setting
        Log.i(TAG, "SQLiteOpenHelper onOpen called. WAL disabled as per original code.");
    }
}
