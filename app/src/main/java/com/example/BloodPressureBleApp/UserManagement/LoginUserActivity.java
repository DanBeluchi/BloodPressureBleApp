package com.example.BloodPressureBleApp.UserManagement;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.BloodPressureBleApp.Data.BloodPressureMeasurement;
import com.example.BloodPressureBleApp.Data.User;
import com.example.BloodPressureBleApp.Database.Database;
import com.example.BloodPressureBleApp.MainActivity;
import com.example.BloodPressureBleApp.R;
import com.example.BloodPressureBleApp.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

import static com.example.BloodPressureBleApp.MainActivity.ACTIVE_USER_KEY;
import static com.example.BloodPressureBleApp.MainActivity.MEASUREMENT_LIST_KEY;
import static com.example.BloodPressureBleApp.MainActivity.USER_NOT_REGISTERED;
import static com.example.BloodPressureBleApp.MainActivity.USER_SWITCH_SUCCESS;


public class LoginUserActivity extends AppCompatActivity {
    public static final String ALL_USER_KEY = "all_user";

    static final int USER_REGISTERED = 7;

    EditText editFieldPassword;
    Spinner spUserList;
    TextView noPassword;
    Button btnEnter;
    String userName = "";
    String enteredPassword = "";
    User userFromDB = null;
    List<BloodPressureMeasurement> measurementsHistory;
    List<User> allUser = new ArrayList<>();
    User activeUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_user);
        spUserList = findViewById(R.id.sp_user_list);

        editFieldPassword = findViewById(R.id.et_password);
        btnEnter = findViewById(R.id.btn_enter);
        /* sign in button is only enabled if
        a username is selected,
        the password field has text in it
        and the confirmed password has the same input as the password field*/
        btnEnter.setEnabled(false);

        editFieldPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            /* pw has to be at least 4 characters to enable the sign in button*/
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 4) {
                    btnEnter.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        new Thread(new getAllUsersFromDB()).start();

        /* check if the entered password is right when the sign in button is clicked  */
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userName = spUserList.getSelectedItem().toString();
                enteredPassword = editFieldPassword.getText().toString();

                userFromDB = Database.mUserDao.fetchUserByName(userName);

                PasswordManager pwManager = new PasswordManager();
                /*check if the entered password is correct*/
                if (pwManager.isPasswordValid(enteredPassword, userFromDB.getmPassword())) {

                    measurementsHistory = Database.mMeasurementsResultsDao.fetchAllMeasurementsFromUserByID(userFromDB.getmId());

                    Intent returnIntent = new Intent(getApplicationContext(), MainActivity.class);
                    /* return user information and measurement history to MainActivity */
                    returnIntent.putExtra(ACTIVE_USER_KEY, (Parcelable) userFromDB);
                    returnIntent.putParcelableArrayListExtra(MEASUREMENT_LIST_KEY, (ArrayList<? extends Parcelable>) measurementsHistory);

                    /* return to MainActivity with entered username */
                    setResult(USER_SWITCH_SUCCESS, returnIntent);
                    finish();
                }
                /*wrong password entered*/
                else {

                    new AlertDialog.Builder(LoginUserActivity.this)
                            .setTitle("Entered password is wrong")
                            .setMessage("Please try again")

                            /*Specifying a listener allows you to take an action before dismissing the dialog.
                            The dialog is automatically dismissed when a dialog button is clicked.*/
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })

                            /*A null listener allows the button to dismiss the dialog and take no further action.*/
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                }
            }
        });
    }


    class getAllUsersFromDB implements Runnable {

        @Override
        public void run() {
            Log.d(LoginUserActivity.class.getCanonicalName(), "Fetching User from DB");
            allUser = Database.mUserDao.fetchAllUsers();
            Handler handler = new Handler(Looper.getMainLooper());
            /* fill spinner with user names from db */
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ArrayAdapter<User> adapter =
                            new ArrayAdapter<User>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, allUser);
                    spUserList.setAdapter(adapter);
                }
            });

        }
    }

    /* text "no_account_yet_register_now" clicked */
    public void onClick(View v) {
        /* start RegisterActivity */
        Intent i = new Intent(getApplicationContext(), RegisterUserActivity.class);
        startActivityForResult(i, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            /*returning from RegisterActivity*/
            if (resultCode == USER_REGISTERED) {
                if (data.hasExtra(ACTIVE_USER_KEY)) {
                    activeUser = data.getParcelableExtra(ACTIVE_USER_KEY);
                    /*create new list because new user has no measurement history*/
                    measurementsHistory = new ArrayList<>();

                    Intent returnIntent = new Intent(getApplicationContext(), MainActivity.class);
                    returnIntent.putExtra(ACTIVE_USER_KEY, (Parcelable) activeUser);
                    returnIntent.putParcelableArrayListExtra(MEASUREMENT_LIST_KEY, (ArrayList<? extends Parcelable>) measurementsHistory);
                    setResult(USER_SWITCH_SUCCESS, returnIntent);
                    finish();
                }
            }
        }
    }
}