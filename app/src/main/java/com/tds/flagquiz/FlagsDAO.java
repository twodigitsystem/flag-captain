package com.tds.flagquiz;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FlagsDAO {

    private static final String TAG = "FlagsDAO";

    // Define table and column names as constants for better maintainability and to avoid typos
    public static final String TABLE_NAME = "flagquiztable";
    public static final String COLUMN_FLAG_ID = "flag_id";
    public static final String COLUMN_FLAG_NAME = "flag_name";
    public static final String COLUMN_FLAG_IMAGE = "flag_image";

    /**
     * Retrieves a specified number of random flag questions from the database.
     *
     * @param fd         The FlagsDatabase helper instance.
     * @param limit      The maximum number of random questions to retrieve.
     * @return A List of FlagsModel objects, or an empty list if an error occurs or no data is found.
     */

    public List<FlagsModel> getRandomQuestions(FlagsDatabase fd, int limit) {
        List<FlagsModel> modelList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        if (limit <= 0) {
            Log.w(TAG, "getRandomQuestions: Limit cannot be zero or negative. Returning empty list.");
            return modelList; // Return empty list if limit is invalid
        }

        try {
            db = fd.getReadableDatabase(); // Use readable database for select operations

            // Use selection arguments for safety and clarity, though RANDOM() doesn't directly benefit from it.
            // The main benefit here is consistency and if 'limit' were a string.
            // String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY RANDOM() LIMIT ?";
            // Using direct concatenation for LIMIT as it's a number and part of SQL standard syntax.
            // For general parameters, placeholders are preferred.
            String query = "SELECT " + COLUMN_FLAG_ID + ", " + COLUMN_FLAG_NAME + ", " + COLUMN_FLAG_IMAGE +
                    " FROM " + TABLE_NAME +
                    " ORDER BY RANDOM() LIMIT " + limit;

            cursor = db.rawQuery(query, null); // No selection args needed for this specific query structure

            if (cursor != null && cursor.moveToFirst()) { // Check if cursor is not null and has data
                int flagIdIndex = cursor.getColumnIndexOrThrow(COLUMN_FLAG_ID);
                int flagNameIndex = cursor.getColumnIndexOrThrow(COLUMN_FLAG_NAME);
                int flagImageIndex = cursor.getColumnIndexOrThrow(COLUMN_FLAG_IMAGE);

                do {
                    FlagsModel model = new FlagsModel(
                            cursor.getInt(flagIdIndex),
                            cursor.getString(flagNameIndex),
                            cursor.getString(flagImageIndex)
                    );
                    modelList.add(model);
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "getRandomQuestions: No flags found or cursor is empty.");
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error getting random questions from database", e);
            // Depending on the app's requirements, you might re-throw a custom exception
            // or return an empty list as is currently done.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // SQLiteOpenHelper manages the closing of the database instance itself when the helper is closed.
            // So, typically you don't call db.close() here if db was obtained from the helper.
            // The helper's close() method (which you'd call when your DAO/Activity is done) handles it.
        }
        return modelList;
    }

    /**
     * Retrieves a specified number of random flag options, excluding a specific flag ID.
     *
     * @param fd                The FlagsDatabase helper instance.
     * @param excludedFlagId    The ID of the flag to exclude from the options.
     * @param numberOfOptions   The number of random options to retrieve.
     * @return A List of FlagsModel objects for the options, or an empty list if an error occurs or no data.
     */
    public List<FlagsModel> getRandomOptions(FlagsDatabase fd, int excludedFlagId, int numberOfOptions) {
        List<FlagsModel> modelList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        if (numberOfOptions <= 0) {
            Log.w(TAG, "getRandomOptions: Number of options cannot be zero or negative. Returning empty list.");
            return modelList;
        }

        try {
            db = fd.getReadableDatabase();

            String query = "SELECT " + COLUMN_FLAG_ID + ", " + COLUMN_FLAG_NAME + ", " + COLUMN_FLAG_IMAGE +
                    " FROM " + TABLE_NAME +
                    " WHERE " + COLUMN_FLAG_ID + " != ?" +
                    " ORDER BY RANDOM() LIMIT ?";

            String[] selectionArgs = {String.valueOf(excludedFlagId), String.valueOf(numberOfOptions)};

            cursor = db.rawQuery(query, selectionArgs);

            if (cursor != null && cursor.moveToFirst()) {
                // Re-fetch column indices as it's a new cursor, or make them member variables if always the same
                int flagIdIndex = cursor.getColumnIndexOrThrow(COLUMN_FLAG_ID);
                int flagNameIndex = cursor.getColumnIndexOrThrow(COLUMN_FLAG_NAME);
                int flagImageIndex = cursor.getColumnIndexOrThrow(COLUMN_FLAG_IMAGE);

                do {
                    FlagsModel model = new FlagsModel(
                            cursor.getInt(flagIdIndex),
                            cursor.getString(flagNameIndex),
                            cursor.getString(flagImageIndex)
                    );
                    modelList.add(model);
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "getRandomOptions: No options found for excluded ID " + excludedFlagId + " or cursor is empty.");
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error getting random options from database for excluded ID " + excludedFlagId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // db from helper, no explicit close needed here.
        }
        return modelList;
    }

    // You could potentially add a private helper method to reduce cursor processing duplication
    // if you had many more similar methods, but for two it's acceptable.
    // private FlagsModel cursorToFlagModel(Cursor cursor, int idIdx, int nameIdx, int imgIdx) {
    //     return new FlagsModel(cursor.getInt(idIdx), cursor.getString(nameIdx), cursor.getString(imgIdx));
    // }


//    public ArrayList<FlagsModel> getRandomTenQuestion(FlagsDatabase fd)
//    {
//        ArrayList<FlagsModel> modelArrayList = new ArrayList<>();
//        SQLiteDatabase liteDatabase = fd.getWritableDatabase();
//        Cursor cursor = liteDatabase.rawQuery("SELECT * FROM flagquiztable ORDER BY RANDOM () LIMIT "+QuizActivity.TOTAL_NUMBER_OF_FLAGS,null);
//
//        int flagIdIndex = cursor.getColumnIndex("flag_id");
//        int flagNameIndex = cursor.getColumnIndex("flag_name");
//        int flagImageIndex = cursor.getColumnIndex("flag_image");
//
//        while (cursor.moveToNext())
//        {
//            FlagsModel model = new FlagsModel(cursor.getInt(flagIdIndex)
//                    ,cursor.getString(flagNameIndex)
//                    ,cursor.getString(flagImageIndex));
//
//            modelArrayList.add(model);
//        }
//        return modelArrayList;
//    }

//    public ArrayList<FlagsModel> getRandomThreeOptions(FlagsDatabase fd, int flag_id)
//    {
//        ArrayList<FlagsModel> modelArrayList = new ArrayList<>();
//        SQLiteDatabase liteDatabase = fd.getWritableDatabase();
//        Cursor cursor = liteDatabase.rawQuery("SELECT * FROM flagquiztable WHERE flag_id !="+flag_id+" ORDER BY RANDOM () LIMIT 3",null);
//
//        int flagIdIndex = cursor.getColumnIndex("flag_id");
//        int flagNameIndex = cursor.getColumnIndex("flag_name");
//        int flagImageIndex = cursor.getColumnIndex("flag_image");
//
//        while (cursor.moveToNext())
//        {
//            FlagsModel model = new FlagsModel(cursor.getInt(flagIdIndex)
//                    ,cursor.getString(flagNameIndex)
//                    ,cursor.getString(flagImageIndex));
//
//            modelArrayList.add(model);
//        }
//        return modelArrayList;
//    }
}
