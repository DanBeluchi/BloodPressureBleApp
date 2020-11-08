package com.example.BloodPressureBleApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.BloodPressureBleApp.Ble.ADGattUUID;
import com.example.BloodPressureBleApp.Data.BloodPressureMeasurement;
import com.example.BloodPressureBleApp.Data.User;
import com.example.BloodPressureBleApp.Database.Database;
import com.example.BloodPressureBleApp.UserManagement.LoginUserActivity;
import com.example.BloodPressureBleApp.Ble.BluetoothLeService;
//import com.example.BloodPressureBleApp.Ble.BluetoothLeService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public static final String ACTIVE_USER_KEY = "activeUser";
    public static final String MEASUREMENT_LIST_KEY = "listOfMeasurements";
    public static final UUID BloodPressureService = ADGattUUID.uuidFromShortString("1810");

    private static final int REQUEST_ENABLE_BT = 1;
    public static final int USER_SWITCH_SUCCESS = 5;
    public static final int USER_NOT_REGISTERED = 6;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    /*Bluetooth */
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothLeScanner mBluetoothLeScanner;
    BluetoothDevice mBluetoothDevice;
    private boolean mScanning;
    private boolean mConnected = false;
    private boolean mIsBindBleService = false;

    //UI
    TextView tvUsername;
    Button mConnectButton;
    private ListAdapter mAdapter;
    private RecyclerView mRecyclerView;

    public static Database mDb;
    String standardUserName = "Daniel";
    User activeUser;
    List<BloodPressureMeasurement> measurementsHistory;

    private int connectionState = STATE_DISCONNECTED;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Init UI */
        mConnectButton = findViewById(R.id.btn_connect);
        tvUsername = findViewById(R.id.tv_user_name);

        mRecyclerView = findViewById(R.id.rv_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // mAdapter = new ListAdapter(measurementsHistory);
        //recyclerView.setAdapter(mAdapter);

        mDb = new Database(this);
        try {
            mDb.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /*Checking for saved state*/
        if (savedInstanceState == null) {
            /* no saved State */
            new Thread(new getUserDataFromDB()).start();
        } else {
            /* saved State */
            /* assign saved User and Health information's  from saved state*/
            activeUser = savedInstanceState.getParcelable(ACTIVE_USER_KEY);
            measurementsHistory = savedInstanceState.getParcelableArrayList(MEASUREMENT_LIST_KEY);
            tvUsername.setText("Hallo " + activeUser.getmName());
        }

        /* Initializes Bluetooth adapter */
        Log.d("Bluetooth Scan", "Init Bluetooth");
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        /* Ensures Bluetooth is available on the device and it is enabled. If not,
           displays a dialog requesting user permission to enable Bluetooth.*/
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        doBindBleService();

        /*Register BroadcastReceiver for BluetoothLEService */
        IntentFilter filter = new IntentFilter(BluetoothLeService.ACTION_BLE_SERVICE);
        filter.addAction(BluetoothLeService.ACTION_BLE_DATA_RECEIVED);
        registerReceiver(gattUpdateReceiver, filter);

        /*start scanning*/
        if (mBluetoothLeScanner != null) {
            new Thread(new scanLeDevice()).start();
        }

    }

    /**
     * Fetches the user entry and the all measurement entries for the standard user.
     * After the data is fetched the UI os updated
     */
    class getUserDataFromDB implements Runnable {
        @Override
        public void run() {
            Log.d(LoginUserActivity.class.getCanonicalName(), "Fetching Data from DB");

            activeUser = Database.mUserDao.fetchUserByName(standardUserName);
            measurementsHistory = Database.mMeasurementsResultsDao.fetchAllMeasurementsFromUserByID(activeUser.getmId());

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(MainActivity.class.getCanonicalName(), "Username: " + activeUser.getmName() + ", Measurements found: " + measurementsHistory.size());
                    tvUsername.setText("Hallo " + activeUser.getmName());

                    mAdapter = new ListAdapter(measurementsHistory);
                    mRecyclerView.setAdapter(mAdapter);
                }
            });
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
            Log.d(MainActivity.class.getCanonicalName(), "Trying to bind service ");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("ServiceConnection", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }

    };

    /**
     * Perform any final cleanup before an activity is destroyed.
     * Unbind service.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindBleRService();
        mBluetoothLeService = null;
    }

    /**
     * Scan filter is set to only scan for Bluetooth devices with a Blood Pressure Service.
     */
    class scanLeDevice implements Runnable {

        @Override
        public void run() {
            mScanning = true;
            /*Scan only for BloodPressure Service*/
            ScanFilter bpUuid = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BloodPressureService.toString())).build();
            ArrayList scanFilterList = new ArrayList();
            scanFilterList.add(bpUuid);

            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
            mBluetoothLeScanner.startScan(scanFilterList, scanSettings, leScanCallback);

            Log.d(MainActivity.class.getCanonicalName(), "Started Scanning");
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
                            if (mBluetoothDevice != null) {
                                Log.d("Bluetooth Scan", "DEVICE FOUND" + mBluetoothDevice.getName());
                                mBluetoothLeService.connect(mBluetoothDevice.getAddress());
                            }
                        }
                    });
                }
            };

    /**
     * Handles various events fired by the Service. TODO
     */
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                Toast.makeText(getApplicationContext(), "GATT connected", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Toast.makeText(getApplicationContext(), "GATT disconnected", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            } else if (BluetoothLeService.ACTION_BLE_DATA_RECEIVED.equals(action)) {
                Log.d("AD", "Received the Blood Pressure data");
                String systolic = intent.getStringExtra("Systolic");
                String diastolic = intent.getStringExtra("Diastolic");
                String pulse = intent.getStringExtra("Pulse");
                String timeStamp = intent.getStringExtra("Timestamp");

                //add to database
                BloodPressureMeasurement newData = Database.mMeasurementsResultsDao.addMeasurementResult(systolic, diastolic, pulse, timeStamp, activeUser.getmId());
                int position = 0;
                    /* if the database operation was successful we can add the received data to the measurement history so we do not need to
                    make another query to get the complete list with the new data to update the recycler view*/
                if (newData != null) {
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
            case R.id.dummy:
                //TODO
                break;

            case R.id.dummy1:
                //todo
                break;

            case R.id.switch_user:
                Intent intent = new Intent(this, LoginUserActivity.class);

                startActivityForResult(intent, 1);
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
                activeUser = data.getParcelableExtra(ACTIVE_USER_KEY);
                measurementsHistory = data.getParcelableArrayListExtra(MEASUREMENT_LIST_KEY);

                tvUsername.setText("Hallo " + activeUser.getmName());

                mAdapter = new ListAdapter(measurementsHistory);
                mRecyclerView.setAdapter(mAdapter);
            }
        }
    }

    ///////////////////////////* Save data for the next onCreate *//////////////////////////////////
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(MainActivity.class.getCanonicalName(), "Trying to save instance ");
        /* Store active user  and health information's to the savedInstanceState */
        outState.putParcelable(ACTIVE_USER_KEY, activeUser);
        outState.putParcelableArrayList(MEASUREMENT_LIST_KEY, (ArrayList<? extends Parcelable>) measurementsHistory);

        super.onSaveInstanceState(outState);
    }


    private void doBindBleService() {
        if (!mIsBindBleService) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            mIsBindBleService = true;
        }
    }

    private void doUnbindBleRService() {
        if (mIsBindBleService) {
            unbindService(mServiceConnection);
            mIsBindBleService = false;
        }
    }
}