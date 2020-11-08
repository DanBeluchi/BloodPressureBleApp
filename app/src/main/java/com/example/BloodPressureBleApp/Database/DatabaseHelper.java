package com.example.BloodPressureBleApp.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Health.db";
    public static final int DATABASE_VERSION = 1;


    private static final String SQL_CREATE_USER_TABLE = "CREATE TABLE " + UserContractClass.UserEntry.TABLE_NAME +
            "(" + UserContractClass.UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            UserContractClass.UserEntry.COLUMN_USER_NAME + " TEXT UNIQUE, " +
            UserContractClass.UserEntry.COLUMN_USER_PASSWORD + " TEXT, " +
            UserContractClass.UserEntry.COLUMN_USER_AGE + " INT)";


    private static final String SQL_CREATE_MEASUREMENTS_TABLE = "CREATE TABLE " + MeasurementResultsContractClass.MeasurementEntry.TABLE_NAME +
            "(" + MeasurementResultsContractClass.MeasurementEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            MeasurementResultsContractClass.MeasurementEntry.COLUMN_SYSTOLIC_VALUE + " TEXT, " +
            MeasurementResultsContractClass.MeasurementEntry.COLUMN_DIASTOLIC_VALUE + " TEXT, " +
            MeasurementResultsContractClass.MeasurementEntry.COLUMN_PULSE_VALUE + " TEXT, " +
            MeasurementResultsContractClass.MeasurementEntry.COLUMN_MEASUREMENT_DATE_TIME + " TEXT, " +
            MeasurementResultsContractClass.MeasurementEntry.COLUMN_USER_ID + " INTEGER, FOREIGN KEY" +
            "(" + MeasurementResultsContractClass.MeasurementEntry.COLUMN_USER_ID + ") REFERENCES " +
            UserContractClass.UserEntry.TABLE_NAME + "(" + UserContractClass.UserEntry._ID + "))";

    private static final String SQL_DELETE_USER_ENTRIES = "DROP TABLE IF EXISTS " + UserContractClass.UserEntry.TABLE_NAME;
    private static final String SQL_DELETE_MEASUREMENT_ENTRIES = "DROP TABLE IF EXISTS " + MeasurementResultsContractClass.MeasurementEntry.TABLE_NAME;

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //called the first time a database is accessed
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USER_TABLE);
        db.execSQL(SQL_CREATE_MEASUREMENTS_TABLE);
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
        db.execSQL(SQL_DELETE_USER_ENTRIES);
        db.execSQL(SQL_DELETE_MEASUREMENT_ENTRIES);
        onCreate(db);
    }


}
