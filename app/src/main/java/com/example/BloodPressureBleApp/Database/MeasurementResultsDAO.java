package com.example.BloodPressureBleApp.Database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.BloodPressureBleApp.Data.BloodPressureMeasurement;

import java.util.ArrayList;
import java.util.List;

public class MeasurementResultsDAO {

    DatabaseHelper mDatabase;

    public MeasurementResultsDAO(DatabaseHelper db) {
        this.mDatabase = db;
    }


    public List<BloodPressureMeasurement> fetchAllMeasurementsFromUserByID(long id) {
        List<BloodPressureMeasurement> list = new ArrayList<>();

        String queryString = "SELECT * FROM " + MeasurementResultsContractClass.MeasurementEntry.TABLE_NAME + " WHERE " +
                MeasurementResultsContractClass.MeasurementEntry.COLUMN_USER_ID + " = " + id + ";";

        SQLiteDatabase db = mDatabase.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToLast()) {
            //loop through the results.
            do {
                long measurementID = cursor.getLong(0);
                String systolic = cursor.getString(1);
                String diastolic = cursor.getString(2);
                String pulse = cursor.getString(3);
                String timeStamp = cursor.getString(4);
                long userID = cursor.getLong(5);

                BloodPressureMeasurement newHealthInformation = new BloodPressureMeasurement(measurementID, systolic, diastolic, pulse, timeStamp, userID);
                list.add(newHealthInformation);
            } while (cursor.moveToPrevious());
        } else {
            //nothing found
        }
        //close cursor and db when finished
        cursor.close();
        db.close();
        return list;
    }


    public BloodPressureMeasurement addMeasurementResult(String systolic, String diastolic, String pulse, String timeStamp, long userID) {
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        ContentValues cv = new ContentValues();
        BloodPressureMeasurement data;

        cv.put(MeasurementResultsContractClass.MeasurementEntry.COLUMN_DIASTOLIC_VALUE, diastolic);
        cv.put(MeasurementResultsContractClass.MeasurementEntry.COLUMN_SYSTOLIC_VALUE, systolic);
        cv.put(MeasurementResultsContractClass.MeasurementEntry.COLUMN_PULSE_VALUE, pulse);
        cv.put(MeasurementResultsContractClass.MeasurementEntry.COLUMN_MEASUREMENT_DATE_TIME, timeStamp);
        cv.put(MeasurementResultsContractClass.MeasurementEntry.COLUMN_USER_ID, userID);

        long insert = db.insert(MeasurementResultsContractClass.MeasurementEntry.TABLE_NAME, null, cv);
        if (insert == -1) {
            return null;
        } else {
            data = new BloodPressureMeasurement(insert, systolic, diastolic, pulse, timeStamp, userID);
            return data;
        }
    }


    public int deleteMeasurementById(long measurementID) {


        SQLiteDatabase db = mDatabase.getWritableDatabase();

        // Cursor cursor = db.rawQuery(queryString, null);
        int numberOfRowsDeleted = db.delete(MeasurementResultsContractClass.MeasurementEntry.TABLE_NAME, MeasurementResultsContractClass.MeasurementEntry._ID + "=" + measurementID, null);

        db.close();
        return numberOfRowsDeleted;
    }
}
