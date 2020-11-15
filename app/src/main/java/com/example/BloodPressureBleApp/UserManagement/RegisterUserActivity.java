package com.example.BloodPressureBleApp.UserManagement;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.BloodPressureBleApp.Data.User;
import com.example.BloodPressureBleApp.Database.Database;
import com.example.BloodPressureBleApp.R;

import java.util.ArrayList;
import java.util.List;

import static com.example.BloodPressureBleApp.MainActivity.ACTIVE_USER_KEY;
import static com.example.BloodPressureBleApp.UserManagement.LoginUserActivity.ALL_USER_KEY;
import static com.example.BloodPressureBleApp.UserManagement.LoginUserActivity.USER_REGISTERED;

public class RegisterUserActivity extends AppCompatActivity {

    EditText et_userName;
    EditText et_password;
    EditText et_confirmedPassword;
    EditText et_age;
    Button btn_register;
    boolean pwEntered = false;
    boolean confirmedPwEntered = false;
    boolean ageEntered = false;
    List<User> allUser = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);


/*
        //get the list with all users so we can check if the username is already in use while typing
        Intent i = getIntent();
        if (i.hasExtra(ALL_USER_KEY)) {
            allUser = i.getParcelableArrayListExtra(ALL_USER_KEY);
        }*/

        final Drawable mCheckIcon = getResources().getDrawable(R.drawable.ic_baseline_check_24);
        final Drawable mErrorIcon = getResources().getDrawable(R.drawable.ic_baseline_error_24);

        mCheckIcon.setBounds(0, 0, mCheckIcon.getIntrinsicWidth(), mCheckIcon.getIntrinsicHeight());
        mErrorIcon.setBounds(0, 0, mErrorIcon.getIntrinsicWidth(), mErrorIcon.getIntrinsicHeight());

        et_userName = findViewById(R.id.et_person_name);
        et_password = findViewById(R.id.et_password);
        et_confirmedPassword = findViewById(R.id.et_password_again);
        et_age = findViewById(R.id.et_person_age);


        btn_register = findViewById(R.id.btn_register);
        btn_register.setEnabled(false);

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PasswordManager pwManager = new PasswordManager();
                String userName;
                String password;
                int age;


                try {
                    userName = et_userName.getText().toString();
                    password = et_password.getText().toString();
                    age = Integer.parseInt(et_age.getText().toString());

                    User userToRegister = new User(userName, pwManager.hashPassword(password), age);

                    try {
                        long operationResult = Database.mUserDao.addUser(userToRegister);
                        if (operationResult < 0) {
                            new AlertDialog.Builder(RegisterUserActivity.this)
                                    .setTitle("Error while registering user")
                                    .setMessage("Please try again")

                                    // Specifying a listener allows you to take an action before dismissing the dialog.
                                    // The dialog is automatically dismissed when a dialog button is clicked.
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })

                                    // A null listener allows the button to dismiss the dialog and take no further action.
                                    .setNegativeButton(android.R.string.no, null)
                                    .show();
                        } else {
                            Intent returnIntent = new Intent(getApplicationContext(), LoginUserActivity.class);
                            returnIntent.putExtra(ACTIVE_USER_KEY, (Parcelable) userToRegister);
                            setResult(USER_REGISTERED, returnIntent);
                            finish();
                        }
                        //username already exists
                    } catch (SQLiteConstraintException ex) {
                        et_userName.setError("Username already in use", mErrorIcon);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });


        //password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_check_24, 0);
        // confirmedPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_check_24, 0);
        et_userName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
/*                for (User u : allUser) {
                    if (u.getmName().contentEquals(s)) {
                        et_userName.setError("Username already in use", mErrorIcon);
                    }
                }*/
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        et_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 5) {
                    et_password.setError("Password should be greater than 4 characters!", mErrorIcon);
                    pwEntered = false;
                } else {
                    pwEntered = true;
                    et_password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_check_24, 0);
                    //password.setError("Good", mCheckIcon);
                }
                if (!checkPasswordFieldMatch()) {
                    et_confirmedPassword.setError("Not Matching", mErrorIcon);
                    confirmedPwEntered = false;
                }

                btn_register.setEnabled(pwEntered && confirmedPwEntered && ageEntered);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et_confirmedPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!checkPasswordFieldMatch()) {
                    et_confirmedPassword.setError("Not Matching", mErrorIcon);
                    confirmedPwEntered = false;
                } else if (pwEntered && checkPasswordFieldMatch()) {
                    et_confirmedPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_check_24, 0);
                    confirmedPwEntered = true;
                }
                btn_register.setEnabled(pwEntered && confirmedPwEntered && ageEntered);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        et_age.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    ageEntered = true;
                } else {
                    ageEntered = false;
                }

                btn_register.setEnabled(pwEntered && confirmedPwEntered && ageEntered);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public boolean checkPasswordFieldMatch() {
        return et_password.getText().toString().equals(et_confirmedPassword.getText().toString());
    }

    ;
}