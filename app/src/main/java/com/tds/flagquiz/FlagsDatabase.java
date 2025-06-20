package com.tds.flagquiz;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class FlagsDatabase extends SQLiteOpenHelper {

    public FlagsDatabase(@Nullable Context context) {
        super(context, "flagquizdb.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS \"flagquiztable\" (\n" +
                "\t\"flag_id\"\tINTEGER,\n" +
                "\t\"flag_name\"\tTEXT,\n" +
                "\t\"flag_image\"\tTEXT\n" +
                ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS flagquiztable");
        onCreate(db);

    }
}
