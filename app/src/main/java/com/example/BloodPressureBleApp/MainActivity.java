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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.BloodPressureBleApp.Database.Database;
import com.example.BloodPressureBleApp.Model.BloodPressureMeasurement;
import com.example.BloodPressureBleApp.Model.User;
import com.example.BloodPressureBleApp.ble.BluetoothLeService;
//import com.example.BloodPressureBleApp.ble.BluetoothLeService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothLeScanner bluetoothLeScanner;
    //UI
    TextView tvUsername;

    private static final int REQUEST_ENABLE_BT = 1;
    static final int USER_SWITCH_SUCCESS = 5;
    private Button connectButton;
    private boolean mScanning;
    private boolean mConnected = false;
    BluetoothDevice device;
    List<BloodPressureMeasurement> measurementsHistory;
    private Listadapter mAdapter;
    private RecyclerView list;
    private boolean mIsBindBleServivce = false;
    public static Database mDb;
    long userID;
    User activeUser;
    String standardUserName = "Daniel";

    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    public static final UUID BloodPressureService = uuidFromShortString("1810");
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvUsername = findViewById(R.id.tv_user_name);

        /* Initializes Bluetooth adapter */
        Log.d("Bluetooth Scan", "Init Bluetooth");
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        /* Ensures Bluetooth is available on the device and it is enabled. If not,
           displays a dialog requesting user permission to enable Bluetooth.*/
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mDb = new Database(this);
        try {
            mDb.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Intent intent1 = new Intent(this, BluetoothLeService.class);
        //startService(intent1);

        doBindBleService();

        if (savedInstanceState == null) {
            /* no saved State */
            activeUser = Database.mUserDao.fetchUserByName(standardUserName);
            measurementsHistory = Database.mMeasurementsResultsDao.fetchAllMeasurementsFromUserByID(activeUser.getId());
            Toast.makeText(getApplicationContext(), "Getting History for  = " + activeUser.getName(), Toast.LENGTH_SHORT).show();
        } else {
            /* saved State */
            /* assign saved User and Health information's  from saved state*/
            activeUser = savedInstanceState.getParcelable("activeUser");
            measurementsHistory = savedInstanceState.getParcelableArrayList("healtInformation");
        }

        tvUsername.setText("Hallo " + activeUser.getName());

        /*Init UI */
        connectButton = findViewById(R.id.btn_connect);
        connectButton.setVisibility(View.INVISIBLE);

        if (bluetoothLeScanner != null) {
            new Thread(new scanLeDevice()).start();
        }

        list = findViewById(R.id.rv_list);
        list.setHasFixedSize(true);

        list.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new Listadapter(measurementsHistory);
        list.setAdapter(mAdapter);


        IntentFilter filter = new IntentFilter(BluetoothLeService.ACTION_BLE_SERVICE);
        filter.addAction(BluetoothLeService.ACTION_BLE_DATA_RECEIVED);
        registerReceiver(gattUpdateReceiver, filter);

        connectButton.setVisibility(View.VISIBLE);
        Log.d("Starting", "Service started");
        final Handler handler = new Handler(Looper.getMainLooper());
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        BloodPressureMeasurement debugData = new BloodPressureMeasurement(99, "99", "99", "99", "99", 1);
                        measurementsHistory.add(0, debugData);
                        mAdapter.notifyDataSetChanged();
                        Toast.makeText(getApplicationContext(), "UserID = " + userID, Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
            Log.d("ServiceConnection", "Trying to bind service ");
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

    class scanLeDevice implements Runnable {

        @Override
        public void run() {
            mScanning = true;
            /*Scan only for BloodPressure Service*/
            ScanFilter bpUuid = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BloodPressureService.toString())).build();
            ArrayList scanFilterList = new ArrayList();
            scanFilterList.add(bpUuid);

            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
            bluetoothLeScanner.startScan(scanFilterList, scanSettings, leScanCallback);

            Log.d("Bluetooth Scan", "Started Scanning");
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    device = result.getDevice();
                    Log.d("Bluetooth Scan", "DEVICE FOUND");
                    mBluetoothLeService.connect(device.getAddress());
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (device != null) {
                                Log.d("Bluetooth Scan", "DEVICE FOUND" + device.getName());
                            }
                        }
                    });
                }
            };

    // Handles various events fired by the Service.
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
                BloodPressureMeasurement newData = Database.mMeasurementsResultsDao.addMeasurementResult(systolic, diastolic, pulse, timeStamp, activeUser.getId());
                int position = 0;
                    /* if the database operation was successful we can add the received data to the measurement history so we do not need to
                    make another query to get the complete list with the new data to update the recycler view*/
                if (newData != null) {
                    measurementsHistory.add(0, newData);
                    mAdapter.notifyDataSetChanged();
                }

                mAdapter.notifyItemInserted(position);
            }
        }
    };

    public static UUID uuidFromShortString(String uuid) {
        return UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", uuid));
    }

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
                //todo
                break;

            case R.id.dummy1:
                //todo
                break;

            case R.id.switch_user:
                /* refresh weather data */
                Intent intent = new Intent(this, SwitchUserActivity.class);

                /* start FavoritesActivity */
                startActivityForResult(intent, 1);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);

    }

    //////////////////////////* Data returned from SwitchUserActivity *///////////////////////////////

    /*check if result was returned from SwitchUserActivity */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == USER_SWITCH_SUCCESS) {
                activeUser = data.getParcelableExtra("activeUser");

                tvUsername.setText("Hallo " + activeUser.getName());

                measurementsHistory = Database.mMeasurementsResultsDao.fetchAllMeasurementsFromUserByID(activeUser.getId());

                mAdapter = new Listadapter(measurementsHistory);
                list.setAdapter(mAdapter);
            }
        }
    }

    ///////////////////////////* Save data for the next onCreate *//////////////////////////////////
    /* save data for the next onCreate */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d("MainActivity", "Trying to save instance ");
        /* Store active user  and health information's to the savedInstanceState */
        outState.putParcelable("activeUser", activeUser);
        outState.putParcelableArrayList("healtInformation", (ArrayList<? extends Parcelable>) measurementsHistory);

        super.onSaveInstanceState(outState);
    }


    private void doBindBleService() {
        if (!mIsBindBleServivce) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            mIsBindBleServivce = true;
        }
    }

    private void doUnbindBleRService() {
        if (mIsBindBleServivce) {
            unbindService(mServiceConnection);
            mIsBindBleServivce = false;
        }
    }
}