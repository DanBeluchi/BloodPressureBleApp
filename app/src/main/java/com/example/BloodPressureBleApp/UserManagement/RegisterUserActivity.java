package com.example.BloodPressureBleApp.UserManagement;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.BloodPressureBleApp.R;

public class RegisterUserActivity extends AppCompatActivity {

    static final int USER_REGISTERED = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);
    }
}