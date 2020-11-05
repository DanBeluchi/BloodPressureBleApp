package com.example.BloodPressureBleApp.Database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.BloodPressureBleApp.Model.BloodPressureMeasurement;

import java.util.ArrayList;
import java.util.List;

import static com.example.BloodPressureBleApp.Database.DatabaseHelper.COLUMN_DIASTOLIC_VALUE;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.COLUMN_MEASUREMENT_DATE_TIME;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.COLUMN_MEASUREMENT_ID;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.COLUMN_PULSE_VALUE;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.COLUMN_SYSTOLIC_VALUE;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.COLUMN_USER_ID;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.HEALTH_INFORMATION_TABLE;

public class MeasurementResultsDAO implements IMeasurementResultsDAO {

    DatabaseHelper database;

    public MeasurementResultsDAO(DatabaseHelper db) {
        this.database = db;
    }

    @Override
    public List<BloodPressureMeasurement> fetchAllMeasurementsFromUserByID(long id) {
        List<BloodPressureMeasurement> list = new ArrayList<>();

        String queryString = "SELECT * FROM " + HEALTH_INFORMATION_TABLE + " WHERE " + COLUMN_USER_ID + " = " + id + ";";

        SQLiteDatabase db = database.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToLast()) {
            //loop through the results.
            do {
                int measurementID = cursor.getInt(0);
                String systolic = cursor.getString(1);
                String diastolic = cursor.getString(2);
                String pulse = cursor.getString(3);
                String timeStamp = cursor.getString(4);
                int userID = cursor.getInt(5);

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

    @Override
    public BloodPressureMeasurement addMeasurementResult(String systolic, String diastolic, String pulse, String timeStamp, long userID) {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues cv = new ContentValues();
        BloodPressureMeasurement data;

        cv.put(COLUMN_DIASTOLIC_VALUE, diastolic);
        cv.put(COLUMN_SYSTOLIC_VALUE, systolic);
        cv.put(COLUMN_PULSE_VALUE, pulse);
        cv.put(COLUMN_MEASUREMENT_DATE_TIME, timeStamp);
        cv.put(COLUMN_USER_ID, userID);

        long insert = db.insert(HEALTH_INFORMATION_TABLE, null, cv);
        if (insert == -1) {
            return null;
        } else {
            data = new BloodPressureMeasurement(insert, systolic, diastolic, pulse, timeStamp, userID);
            return data;
        }
    }

    @Override
    public int deleteMeasurementById(long measurementID) {
        String queryString = "DELETE FROM " + HEALTH_INFORMATION_TABLE + " WHERE " + COLUMN_MEASUREMENT_ID + " = " + measurementID;

        SQLiteDatabase db = database.getWritableDatabase();

        // Cursor cursor = db.rawQuery(queryString, null);
        int numberOfRowsDeleted = db.delete(HEALTH_INFORMATION_TABLE, COLUMN_MEASUREMENT_ID + "=" + measurementID, null);

        db.close();
        return numberOfRowsDeleted;
    }
}
