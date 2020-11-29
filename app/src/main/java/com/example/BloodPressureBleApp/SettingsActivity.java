package com.example.BloodPressureBleApp;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.BloodPressureBleApp.Ble.PairDeviceActivity;
import com.example.BloodPressureBleApp.Data.BloodPressureMeasurement;
import com.example.BloodPressureBleApp.Data.User;
import com.example.BloodPressureBleApp.UserManagement.LoginUserActivity;
import com.example.BloodPressureBleApp.UserManagement.RegisterUserActivity;

import java.util.ArrayList;
import java.util.List;

import static com.example.BloodPressureBleApp.MainActivity.ACTIVE_USER_KEY;
import static com.example.BloodPressureBleApp.MainActivity.DEVICE_PAIRED_KEY;
import static com.example.BloodPressureBleApp.MainActivity.MEASUREMENT_LIST_KEY;
import static com.example.BloodPressureBleApp.MainActivity.USER_SWITCH_SUCCESS;

public class SettingsActivity extends AppCompatActivity {

    public static final String PROFILE = "profile";
    public static final String DEVICE_KEY = "device_key";
    public static final int DEVICE_PAIRED = 5;
    static final int USER_REGISTERED = 7;


    String mUserName;
    String mDeviceUuid;
    static SharedPreferences prefs;
    SharedPreferences.Editor editor;
    User activeUser;
    BluetoothDevice mBluetoothDevice;
    boolean mDevicePaired;
    List<BloodPressureMeasurement> measurementsHistory;

    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //Loads Shared preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        editor = prefs.edit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference profileName = (Preference) findPreference("profile");
            assert profileName != null;
            profileName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(SettingsActivity.class.getCanonicalName(), preference.getKey().toString());
                    Intent i;
                    i = new Intent(getContext(), LoginUserActivity.class);
                    getActivity().startActivityForResult(i, 1);
                    return false;
                }
            });

            final Preference addDevice = (Preference) findPreference("add_device");
            assert addDevice != null;
            addDevice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(SettingsActivity.class.getCanonicalName(), preference.getKey().toString());
                    Intent i;
                    i = new Intent(getContext(), PairDeviceActivity.class);
                    getActivity().startActivityForResult(i, 1);
                    return false;
                }
            });

            final Preference createProfile = (Preference) findPreference("create_profile");
            assert createProfile != null;
            createProfile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(SettingsActivity.class.getCanonicalName(), preference.getKey().toString());
                    Intent i;
                    i = new Intent(getContext(), RegisterUserActivity.class);
                    getActivity().startActivityForResult(i, 1);
                    return false;
                }
            });


            final Preference connectedDevice = (Preference) findPreference("connected_device");
            assert connectedDevice != null;

            /* get the newest values and set the summary */
            profileName.setSummary(prefs.getString(PROFILE, ""));
            connectedDevice.setSummary(prefs.getString("connected_device", ""));


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == USER_SWITCH_SUCCESS) {
                if (data.hasExtra(ACTIVE_USER_KEY)) {
                    Toast.makeText(getApplicationContext(), "Sign In successful", Toast.LENGTH_SHORT).show();

                    activeUser = data.getParcelableExtra(ACTIVE_USER_KEY);
                    measurementsHistory = data.getParcelableArrayListExtra(MEASUREMENT_LIST_KEY);

                    /* save user in shared preferences */
                    editor.putString(PROFILE, activeUser.getmName());
                    editor.apply();

                    Intent returnIntent = new Intent(getApplicationContext(), MainActivity.class);
                    returnIntent.putExtra(ACTIVE_USER_KEY, (Parcelable) activeUser);
                    returnIntent.putParcelableArrayListExtra(MEASUREMENT_LIST_KEY, (ArrayList<? extends Parcelable>) measurementsHistory);
                    setResult(USER_SWITCH_SUCCESS, returnIntent);
                    finish();
                }
            }

            if (resultCode == USER_REGISTERED) {
                if (data.hasExtra(ACTIVE_USER_KEY)) {
                    activeUser = data.getParcelableExtra(ACTIVE_USER_KEY);
                    /* create new list because new user has no measurement history */
                    measurementsHistory = new ArrayList<>();

                    /* save user in shared preferences */
                    editor.putString(PROFILE, activeUser.getmName());
                    editor.apply();

                    Intent returnIntent = new Intent(getApplicationContext(), MainActivity.class);
                    returnIntent.putExtra(ACTIVE_USER_KEY, (Parcelable) activeUser);
                    returnIntent.putParcelableArrayListExtra(MEASUREMENT_LIST_KEY, (ArrayList<? extends Parcelable>) measurementsHistory);
                    setResult(USER_SWITCH_SUCCESS, returnIntent);
                    finish();
                }
            }

            if (resultCode == DEVICE_PAIRED) {
                if (data.hasExtra(DEVICE_KEY)) {

                    mBluetoothDevice = data.getParcelableExtra(DEVICE_KEY);
                    Toast.makeText(getApplicationContext(), "Pairing successful", Toast.LENGTH_SHORT).show();
                    editor.putString("connected_device", mBluetoothDevice.getName() + System.lineSeparator() + mBluetoothDevice.getAddress());
                    editor.apply();

                    /* set successful pairing result for MainActivity */
                    Intent returnIntent = new Intent(getApplicationContext(), MainActivity.class);
                    returnIntent.putExtra(DEVICE_PAIRED_KEY, true);
                    setResult(DEVICE_PAIRED, returnIntent);
                    finish();
                }
            }
        }
    }
}