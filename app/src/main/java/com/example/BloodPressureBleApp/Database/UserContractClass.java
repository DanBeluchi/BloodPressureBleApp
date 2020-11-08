package com.example.BloodPressureBleApp.Database;

import android.provider.BaseColumns;

public class UserContractClass {

    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "USER_TABLE";

        public static final String COLUMN_USER_NAME = "USER_NAME";
        public static final String COLUMN_USER_PASSWORD = "USER_PASSWORD";
        public static final String COLUMN_USER_AGE = "USER_AGE";

    }
}
