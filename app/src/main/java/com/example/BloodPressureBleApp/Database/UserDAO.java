package com.example.BloodPressureBleApp.Database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.example.BloodPressureBleApp.Data.User;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    DatabaseHelper mDatabase;

    public UserDAO(DatabaseHelper db) {
        this.mDatabase = db;
    }

    public User fetchUserById(int userId) {
        String queryString = "SELECT * FROM " + UserContractClass.UserEntry.TABLE_NAME + " WHERE " + UserContractClass.UserEntry._ID + " = " + userId + ";";

        SQLiteDatabase db = mDatabase.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        User user = new User();

        if (cursor.moveToFirst()) {
            //loop through the results.
            do {
                user.setmId((long) cursor.getLong(0));
                user.setmName(cursor.getString(1));
                user.setmPassword(cursor.getString(2));
                user.setmAge(cursor.getInt(3));
            } while (cursor.moveToNext());

        } else {
            user.setmId(-1);
        }
        //close cursor and db when finished
        cursor.close();
        db.close();
        return user;
    }


    public User fetchUserByName(String name) {
        String queryString = "SELECT * FROM " + UserContractClass.UserEntry.TABLE_NAME + " WHERE " +
                UserContractClass.UserEntry.COLUMN_USER_NAME + " LIKE " + '"' + name + '"' + ";";

        SQLiteDatabase db = mDatabase.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);
        User user = new User();

        if (cursor.moveToFirst()) {
            //loop through the results.
            do {
                user.setmId(cursor.getLong(0));
                user.setmName(cursor.getString(1));
                user.setmPassword(cursor.getString(2));
                user.setmAge(cursor.getInt(3));
            } while (cursor.moveToNext());

        } else {
            user.setmId(-1);
        }
        //close cursor and db when finished
        cursor.close();
        db.close();
        return user;
    }


    public List<User> fetchAllUsers() {
        List<User> list = new ArrayList<>();

        String queryString = "SELECT * FROM " + UserContractClass.UserEntry.TABLE_NAME + ";";

        SQLiteDatabase db = mDatabase.getReadableDatabase();

        Cursor cursor = db.rawQuery(queryString, null);

        if (cursor.moveToFirst()) {

            //loop through the results.
            do {
                User user = new User();
                user.setmId(cursor.getLong(0));
                user.setmName(cursor.getString(1));
                user.setmPassword(cursor.getString(2));
                user.setmAge(cursor.getInt(3));

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


    public long addUser(User user) throws SQLiteConstraintException{
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        ContentValues cv = new ContentValues();
        long insert;

        cv.put(UserContractClass.UserEntry.COLUMN_USER_NAME, user.getmName());
        cv.put(UserContractClass.UserEntry.COLUMN_USER_PASSWORD, user.getmPassword());
        cv.put(UserContractClass.UserEntry.COLUMN_USER_AGE, user.getmAge());
        try{
            insert = db.insertOrThrow(UserContractClass.UserEntry.TABLE_NAME, null, cv);
        } catch (SQLiteConstraintException ex) {
            throw new SQLiteConstraintException();
        } finally {
            db.close();
        }

        return insert;

    }


    public boolean deleteAllUsers() {
        return false;
    }
}
