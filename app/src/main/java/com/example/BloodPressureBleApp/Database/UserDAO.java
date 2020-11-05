package com.example.BloodPressureBleApp.Database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.BloodPressureBleApp.Model.User;

import java.util.ArrayList;
import java.util.List;

import static com.example.BloodPressureBleApp.Database.DatabaseHelper.COLUMN_USER_AGE;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.COLUMN_USER_NAME;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.HEALTH_INFORMATION_TABLE;
import static com.example.BloodPressureBleApp.Database.DatabaseHelper.USER_TABLE;

public class UserDAO implements IUserDAO {

    DatabaseHelper database;

    public UserDAO(DatabaseHelper db) {
        this.database = db;
    }

    @Override
    public User fetchUserById(int userId) {
        String queryString = "SELECT * FROM " + USER_TABLE + " WHERE " + COLUMN_USER_NAME + " = " + userId + ";";

        SQLiteDatabase db = database.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        User user = new User();

        if (cursor.moveToFirst()) {
            //loop through the results.
            do {
                user.setId((long) cursor.getInt(0));
                user.setName(cursor.getString(1));
                user.setAge(cursor.getInt(2));
            } while (cursor.moveToNext());

        } else {
            user.setId(-1);
        }
        //close cursor and db when finished
        cursor.close();
        db.close();
        return user;
    }

    @Override
    public User fetchUserByName(String name) {
        String queryString = "SELECT * FROM " + USER_TABLE + " WHERE " + COLUMN_USER_NAME + " LIKE " + '"' + name + '"' + ";";

        SQLiteDatabase db = database.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        User user = new User();

        if (cursor.moveToFirst()) {
            //loop through the results.
            do {
                user.setId((long) cursor.getInt(0));
                user.setName(cursor.getString(1));
                user.setAge(cursor.getInt(2));
            } while (cursor.moveToNext());

        } else {
            user.setId(-1);
        }
        //close cursor and db when finished
        cursor.close();
        db.close();
        return user;
    }

    @Override
    public List<User> fetchAllUsers() {
        List<User> list = new ArrayList<>();

        String queryString = "SELECT * FROM " + HEALTH_INFORMATION_TABLE + ";";

        SQLiteDatabase db = database.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        User user = new User();

        if (cursor.moveToFirst()) {
            //loop through the results.
            do {

                user.setId((long) cursor.getInt(0));
                user.setName(cursor.getString(1));
                user.setAge(cursor.getInt(2));

                list.add(user);
            } while (cursor.moveToNext());

        } else {
            //nothing found
        }
        //close cursor and db when finished
        cursor.close();
        db.close();
        return list;
    }

    @Override
    public long addUser(User user) {
        SQLiteDatabase db = database.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_USER_NAME, user.getName());
        cv.put(COLUMN_USER_AGE, user.getAge());

        long insert = db.insert(USER_TABLE, null, cv);

        if (insert == -1) {
            return -1;
        } else {
            return insert;
        }
    }

    @Override
    public boolean deleteAllUsers() {
        return false;
    }
}
