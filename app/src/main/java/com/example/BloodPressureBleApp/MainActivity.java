package com.example.BloodPressureBleApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.BloodPressureBleApp.Ble.ADGattUUID;
import com.example.BloodPressureBleApp.Data.BloodPressureMeasurement;
import com.example.BloodPressureBleApp.Data.User;
import com.example.BloodPressureBleApp.Database.Database;
import com.example.BloodPressureBleApp.UserManagement.LoginUserActivity;
import com.example.BloodPressureBleApp.Ble.BluetoothLeService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.example.BloodPressureBleApp.Ble.BluetoothLeService.RECEIVE_DATA_OPERATION;
import static com.example.BloodPressureBleApp.SettingsActivity.DEVICE_PAIRED;
import static com.example.BloodPressureBleApp.SettingsActivity.PROFILE;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ACTIVE_USER_KEY = "activeUser";
    public static final String MEASUREMENT_LIST_KEY = "listOfMeasurements";
    public static final String SCANNING_STATUS_KEY = "scanningStatus";
    public static final String PAIRING_STATUS_KEY = "pairingStatus";
    public static final String DEVICE_PAIRED_KEY = "pairedDevice";
    public static final String CONNECTED_DEVICE = "connectedDevice";
    public static final String PAIRED_DEVICE_ADDRESS = "pairedDeviceAddress";
    public static final UUID BloodPressureService = ADGattUUID.uuidFromShortString("1810");

    private static final int REQUEST_ENABLE_BT = 1;
    public static final int USER_SWITCH_SUCCESS = 5;
    public static final int USER_NOT_REGISTERED = 6;

    private static final int STATE_DISCONNECTED = 0;

    SharedPreferences prefs;
    SharedPreferences.Editor prefEditor;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothLeScanner mBluetoothLeScanner;
    BluetoothDevice mBluetoothDevice;
    private boolean isScanning = false;
    private boolean isPaired = false;
    private boolean mIsBindBleService = false;
    private String mPairedDeviceAddress;

    //UI
    TextView tvUsername;
    private ListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Button mRcvDataBtn;
    ImageView bluetoothIcon;

    public static Database mDb;
    String standardUserName;
    User activeUser;
    List<BloodPressureMeasurement> measurementsHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Loads Shared preferences*/
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefEditor = prefs.edit();

        mPairedDeviceAddress = prefs.getString(PAIRED_DEVICE_ADDRESS, "");
        standardUserName = prefs.getString(PROFILE, "");
        /*Init UI */
        tvUsername = findViewById(R.id.tv_user_name);
        mRcvDataBtn = findViewById(R.id.btn_receive_data);
        mRecyclerView = findViewById(R.id.rv_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        bluetoothIcon = findViewById(R.id.bluetooth_receiving);

        /* Initializes Bluetooth adapter */
        Log.d(TAG, "Init Bluetooth");
        /*Bluetooth */
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        /* Ensures Bluetooth is available on the device and it is enabled. If not,
           displays a dialog requesting user permission to enable Bluetooth.*/
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        /* Always check if a device is paired*/
        try {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            /* Paired device found*/
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    /* check if paired device is the same as the prev paired one*/
                    if (deviceHardwareAddress.equals(mPairedDeviceAddress)) {
                        isPaired = true;
                    }
                }
                /* paired device but not our previously saved device */
                if (!isPaired) {
                    mPairedDeviceAddress = "";
                    prefEditor.putString(CONNECTED_DEVICE, "");
                    prefEditor.putString(PAIRED_DEVICE_ADDRESS, "");
                    prefEditor.apply();
                    isPaired = false;
                }
                /* no paired device */
            } else {
                mPairedDeviceAddress = "";
                prefEditor.putString(CONNECTED_DEVICE, "");
                prefEditor.putString(PAIRED_DEVICE_ADDRESS, "");
                prefEditor.apply();
                isPaired = false;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        mDb = new Database(this);
        try {
            mDb.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /*  bind bluetooth service */
        doBindBleService();

        /*Checking for saved state*/
        if (savedInstanceState == null) {
            /* no saved State */
            new Thread(new getUserDataFromDB()).start();
            /* check if a device was paired */
            /* no device paired*/
            if (mPairedDeviceAddress.isEmpty()) {
                Toast.makeText(getApplicationContext(), "No Device connected", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "Please pair a device under Settings-> Add new device", Toast.LENGTH_SHORT).show();
                mRcvDataBtn.setVisibility(View.GONE);
            } else {
                isPaired = true;
            }
        } else {
            /* saved State */
            /* check for parcelled file descriptors  */
            if (savedInstanceState.containsKey(ACTIVE_USER_KEY)) {
                /* assign information's from saved state*/
                activeUser = savedInstanceState.getParcelable(ACTIVE_USER_KEY);
                measurementsHistory = savedInstanceState.getParcelableArrayList(MEASUREMENT_LIST_KEY);

                tvUsername.setText("Hallo " + activeUser.getmName());

                mAdapter = new ListAdapter(measurementsHistory);
                mRecyclerView.setAdapter(mAdapter);

            } else {
                tvUsername.setText(R.string.no_profile_message);
            }
            /* check for other saved information's */
            if (savedInstanceState.containsKey(PAIRING_STATUS_KEY) && savedInstanceState.containsKey(SCANNING_STATUS_KEY)) {

                isScanning = savedInstanceState.getBoolean(SCANNING_STATUS_KEY);
                isPaired = savedInstanceState.getBoolean(PAIRING_STATUS_KEY);

                /* set button visibility*/
                if (isPaired) {
                    mRcvDataBtn.setVisibility(View.VISIBLE);
                } else {
                    mRcvDataBtn.setVisibility(View.GONE);
                }
            }
        }


        mRcvDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*stop scanning */
                if (isScanning) {
                    mRcvDataBtn.setText(getResources().getString(R.string.receive_data));
                    bluetoothIcon.setVisibility(View.INVISIBLE);
                    bluetoothIcon = (ImageView) makeMeBlink(bluetoothIcon, false);
                    Log.d(TAG, "Stopped Scanning");
                    mBluetoothLeScanner.stopScan(leScanCallback);
                    isScanning = false;
                } else {
                    /*start scanning*/
                    if (mBluetoothLeScanner != null) {
                        new Thread(new scanLeDevice()).start();
                        bluetoothIcon.setVisibility(View.VISIBLE);
                        bluetoothIcon = (ImageView) makeMeBlink(bluetoothIcon, true);
                    }
                    mRcvDataBtn.setText(R.string.text_stop_scan);
                    isScanning = true;
                }
            }
        });

        /*Register BroadcastReceiver for BluetoothLEService */
        IntentFilter filter = new IntentFilter(BluetoothLeService.ACTION_BLE_DATA_RECEIVED);
        registerReceiver(gattUpdateReceiver, filter);
    }

    /**
     * Fetches the user entry and the all measurement entries for the standard user.
     * After the data is fetched the UI os updated
     */
    class getUserDataFromDB implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Fetching Data from DB");
            activeUser = Database.mUserDao.fetchUserByName(standardUserName);

            Handler handler = new Handler(Looper.getMainLooper());

            if (activeUser != null) {
                measurementsHistory = Database.mMeasurementsResultsDao.fetchAllMeasurementsFromUserByID(activeUser.getmId());

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Username: " + activeUser.getmName() + ", Measurements found: " + measurementsHistory.size());
                        tvUsername.setText("Hallo " + activeUser.getmName());

                        mAdapter = new ListAdapter(measurementsHistory);
                        mRecyclerView.setAdapter(mAdapter);
                        if (isPaired) {
                            mRcvDataBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvUsername.setText(R.string.no_profile_message);
                        mRcvDataBtn.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    /**
     * Interface for monitoring the state of an application service.  See
     * {@link android.app.Service} and
     * {@link Context#bindService Context.bindService()} for more information.
     * <p>Like many callbacks from the system, the methods on this class are called
     * from the main thread of your process.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "Trying to bind service ");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.setOperation(RECEIVE_DATA_OPERATION);
            Log.d(TAG, "BluetoothLeService bound ");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }

    };


    @Override
    protected void onResume() {
        doBindBleService();
        super.onResume();
    }

    /**
     * Perform any final cleanup before an activity is destroyed.
     * Unbind service.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        /* Check if the activity is  finishing or just changing the orientation*/
        if (isFinishing()) {
            doUnbindBleRService();
            mBluetoothLeService = null;
        } else {
            //It's an orientation change.
        }
    }

    /**
     * Scan filter is set to only scan for BLE devices with a Blood Pressure Service.
     */
    class scanLeDevice implements Runnable {

        @Override
        public void run() {
            isScanning = true;
            /*Scan only for BloodPressure Service*/
            ScanFilter bpUuid = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BloodPressureService.toString())).build();
            ArrayList scanFilterList = new ArrayList();
            scanFilterList.add(bpUuid);

            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
            mBluetoothLeScanner.startScan(scanFilterList, scanSettings, leScanCallback);

            Log.d(TAG, "Started Scanning");
        }
    }

    /**
     * Bluetooth LE scan callbacks. Scan results are reported using these callbacks.
     *
     * @see BluetoothLeScanner#startScan
     */
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    mBluetoothDevice = result.getDevice();
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBluetoothDevice != null && mBluetoothDevice.getAddress().equals(mPairedDeviceAddress)) {
                                Log.d(TAG, "BLE device found " + mBluetoothDevice.getName());
                                mBluetoothLeService.connect(mBluetoothDevice.getAddress());
                            }
                        }
                    });
                }
            };

    /**
     * Handles various events fired by the BluetoothLeService.
     */
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            String sendingDevice = intent.getStringExtra("Device Address");
            if (BluetoothLeService.ACTION_BLE_DATA_RECEIVED.equals(action)) {
                Log.d(TAG, "Received the Blood Pressure data");
                /* stop scanning */
                Log.d(TAG, "Stopped Scanning");
                mBluetoothLeScanner.stopScan(leScanCallback);
                /* get data */
                String systolic = intent.getStringExtra("Systolic");
                String diastolic = intent.getStringExtra("Diastolic");
                String pulse = intent.getStringExtra("Pulse");
                String timeStamp = intent.getStringExtra("Timestamp");

                /*add data to db */
                BloodPressureMeasurement newData = Database.mMeasurementsResultsDao.addMeasurementResult(systolic, diastolic, pulse, timeStamp, activeUser.getmId());
                int position = 0;
                    /* if the database operation was successful we can add the received data to the measurement history so we do not need to
                    make another query to get the complete list with the new data to update the recycler view*/
                if (newData != null) {
                    /* update UI */
                    mRcvDataBtn.setText(getResources().getString(R.string.receive_data));
                    bluetoothIcon.setVisibility(View.INVISIBLE);
                    bluetoothIcon = (ImageView) makeMeBlink(bluetoothIcon, false);
                    measurementsHistory.add(position, newData);
                    mAdapter.notifyItemInserted(position);
                }
            }
        }
    };

    //////////////////////////* Create and setting up Menu *///////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_icons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.settings:
                mBluetoothLeScanner.stopScan(leScanCallback);
                doUnbindBleRService();
                Intent intentForSetting = new Intent(this, SettingsActivity.class);
                startActivityForResult(intentForSetting, 1);
                break;

            case R.id.switch_user:
                Intent intentForUserswitch = new Intent(this, LoginUserActivity.class);

                startActivityForResult(intentForUserswitch, 1);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);

    }

    //////////////////////////* Data returned from LoginUserActivity *///////////////////////////////

    /*check if result was returned from LoginUserActivity */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == USER_SWITCH_SUCCESS) {
                if (data.hasExtra(ACTIVE_USER_KEY)) {

                    activeUser = data.getParcelableExtra(ACTIVE_USER_KEY);
                    measurementsHistory = data.getParcelableArrayListExtra(MEASUREMENT_LIST_KEY);

                    prefEditor.putString(PROFILE, activeUser.getmName());
                    prefEditor.apply();
                    tvUsername.setText("Hallo " + activeUser.getmName());

                    mAdapter = new ListAdapter(measurementsHistory);
                    mRecyclerView.setAdapter(mAdapter);

                    if (isPaired) {
                        mRcvDataBtn.setVisibility(View.VISIBLE);
                    }
                }
            }

            if (resultCode == DEVICE_PAIRED) {
                if (data.hasExtra(DEVICE_PAIRED_KEY)) {
                    isPaired = data.getBooleanExtra(DEVICE_PAIRED_KEY, false);
                    mPairedDeviceAddress = data.getStringExtra(PAIRED_DEVICE_ADDRESS);
                    if (activeUser != null) {
                        mRcvDataBtn.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    /**
     * Save data for the next onCreate *
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "Trying to save instance ");
        /* Store active user  and health information's to the savedInstanceState */
        if (activeUser != null) {
            outState.putParcelable(ACTIVE_USER_KEY, activeUser);
            outState.putParcelableArrayList(MEASUREMENT_LIST_KEY, (ArrayList<? extends Parcelable>) measurementsHistory);
        }

        outState.putBoolean(SCANNING_STATUS_KEY, isScanning);
        outState.putBoolean(PAIRING_STATUS_KEY, isPaired);
        super.onSaveInstanceState(outState);
    }

    /**
     * bind to the BluetoothLeService
     */
    public void doBindBleService() {
        if (!mIsBindBleService) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            mIsBindBleService = true;
        }
    }

    /**
     * unbind from BluetoothLeService
     */
    public void doUnbindBleRService() {
        if (mIsBindBleService) {
            unbindService(mServiceConnection);
            mIsBindBleService = false;
        }
    }


    /**
     * Make a View Blink for a desired duration
     *
     * @param view   view that will be animated
     * @param enable true: start of the animation false: clears animation
     * @return returns the same view with animation properties
     */
    public static View makeMeBlink(View view, boolean enable) {

        if (enable) {
            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(250);
            anim.setStartOffset(50);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            view.startAnimation(anim);
        } else {
            view.clearAnimation();
        }

        return view;
    }

    /**
     * Items in der recycler view have a white background. The background changes if a
     * item is selected. Set the adapter, after context menu closes, to set the background
     * for each element back to white. Else the selected item stays colored
     */
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        mRecyclerView.setAdapter(mAdapter);
    }
}