package com.example.BloodPressureBleApp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.BloodPressureBleApp.Database.Database;
import com.example.BloodPressureBleApp.Model.User;

import static com.example.BloodPressureBleApp.MainActivity.USER_SWITCH_SUCCESS;

public class SwitchUserActivity extends AppCompatActivity {

    EditText editFieldUserName;
    Button btnEnter;
    String userName = "";
    User activeUser = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_user);

        editFieldUserName = findViewById(R.id.et_person_name);
        btnEnter = findViewById(R.id.btn_enter);

        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName = editFieldUserName.getText().toString();
                if (!userName.isEmpty()) {
                    activeUser = Database.mUserDao.fetchUserByName(userName);
                    if (activeUser.getId() != -1) {

                        Intent returnIntent = new Intent(getApplicationContext(), MainActivity.class);
                        returnIntent.putExtra("activeUser", (Parcelable) activeUser);
                        /* return to MainActivity with entered username */
                        setResult(USER_SWITCH_SUCCESS, returnIntent);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "User does not exist", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }
}