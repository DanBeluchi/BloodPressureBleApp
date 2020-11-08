package com.example.BloodPressureBleApp.Database;

import android.provider.BaseColumns;

public class MeasurementResultsContractClass implements BaseColumns {

    public static class MeasurementEntry implements BaseColumns {
        public static final String TABLE_NAME = "MEASUREMENTS_TABLE";

        public static final String COLUMN_SYSTOLIC_VALUE = "SYSTOLIC_VALUE";
        public static final String COLUMN_DIASTOLIC_VALUE = "DIASTOLIC_VALUE";
        public static final String COLUMN_PULSE_VALUE = "PULSE_VALUE";
        public static final String COLUMN_MEASUREMENT_DATE_TIME = "MEASUREMENT_DATE_TIME";
        public static final String COLUMN_USER_ID = "USER_ID";
    }
}
