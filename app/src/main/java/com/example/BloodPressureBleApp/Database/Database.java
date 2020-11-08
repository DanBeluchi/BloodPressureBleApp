package com.example.BloodPressureBleApp.Database;

import android.content.Context;

import java.sql.SQLException;

public class Database {
    private final Context mContext;
    public static UserDAO mUserDao;
    public static MeasurementResultsDAO mMeasurementsResultsDao;

    private DatabaseHelper mDbHelper;
    // Increment DB Version on any schema change
    private static final int DATABASE_VERSION = 1;


    public void open() throws SQLException {
        mDbHelper = new DatabaseHelper(mContext);

        mUserDao = new UserDAO(mDbHelper);
        mMeasurementsResultsDao = new MeasurementResultsDAO(mDbHelper);
    }

    public void close() {
        mDbHelper.close();
    }

    public Database(Context context) {
        this.mContext = context;
    }
}
