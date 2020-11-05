package com.example.BloodPressureBleApp.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Health.db";
    public static final int DATABASE_VERSION = 1;

    public static final String USER_TABLE = "USER_TABLE";
    public static final String HEALTH_INFORMATION_TABLE = "HEALTH_INFORMATION_TABLE";
    public static final String COLUMN_USER_NAME = "USER_NAME";
    public static final String COLUMN_USER_AGE = "USER_AGE";

    public static final String COLUMN_MEASUREMENT_ID = "ID";
    public static final String COLUMN_SYSTOLIC_VALUE = "SYSTOLIC_VALUE";
    public static final String COLUMN_DIASTOLIC_VALUE = "DIASTOLIC_VALUE";
    public static final String COLUMN_PULSE_VALUE = "PULSE_VALUE";
    public static final String COLUMN_MEASUREMENT_DATE_TIME = "MEASUREMENT_DATE_TIME";
    public static final String COLUMN_USER_ID = "USER_ID";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //called the first time a database is accessed
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTableStatement = "CREATE TABLE " + USER_TABLE + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_USER_NAME + " TEXT UNIQUE, " + COLUMN_USER_AGE + " INT)";
        String createBloodPressureTableStatement = "CREATE TABLE " + HEALTH_INFORMATION_TABLE + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_SYSTOLIC_VALUE + " TEXT, " + COLUMN_DIASTOLIC_VALUE + " TEXT, " + COLUMN_PULSE_VALUE + " TEXT, " + COLUMN_MEASUREMENT_DATE_TIME + " TEXT, " + COLUMN_USER_ID + " INTEGER, FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + USER_TABLE + "(ID))";

        db.execSQL(createUserTableStatement);
        db.execSQL(createBloodPressureTableStatement);


    }

    // Foreign Key Constraint is not enabled by default
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    //called if the database version number changes
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


}
