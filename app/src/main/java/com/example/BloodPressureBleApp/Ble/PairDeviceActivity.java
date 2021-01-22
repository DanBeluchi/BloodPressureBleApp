package com.example.BloodPressureBleApp.Ble;

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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.BloodPressureBleApp.BleDevicesListAdapter;
import com.example.BloodPressureBleApp.MainActivity;
import com.example.BloodPressureBleApp.R;
import com.example.BloodPressureBleApp.SettingsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.example.BloodPressureBleApp.Ble.ADGattUUID.BloodPressureService;
import static com.example.BloodPressureBleApp.Ble.BluetoothLeService.PAIR_OPERATION;
import static com.example.BloodPressureBleApp.MainActivity.PAIRED_DEVICE_ADDRESS;
import static com.example.BloodPressureBleApp.SettingsActivity.DEVICE_PAIRED;
import static com.example.BloodPressureBleApp.SettingsActivity.DEVICE_KEY;

public class PairDeviceActivity extends AppCompatActivity {

    private static final String TAG = "PairDeviceActivity";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 5000;


    TextView tvDeviceName;
    TextView tvDeviceAddress;

    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeService mBluetoothLeService;
    private boolean mIsBindBleService = false;
    private boolean mScanning = false;

    List<BluetoothDevice> mBleDeviceList = new ArrayList<>();
    BluetoothDevice mBluetoothDevice;
    Button mScanButton;
    ProgressBar mProgressIndicator;
    private BleDevicesListAdapter mAdapter;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_device);

        tvDeviceName = findViewById(R.id.tv_device_name);
        tvDeviceAddress = findViewById(R.id.tv_device_address);
        mScanButton = findViewById(R.id.btn_start_scan);
        mProgressIndicator = findViewById(R.id.progressBar);
        mScanButton.setVisibility(View.GONE);
        mRecyclerView = findViewById(R.id.rv_ble_list);

        /* Initializes Bluetooth adapter */
        Log.d("Bluetooth Scan", "Init Bluetooth");
        /*Bluetooth */
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        if (mAdapter == null) {
            mAdapter = new BleDevicesListAdapter(mBleDeviceList);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);


        /* Ensures Bluetooth is available on the device and it is enabled. If not,
           displays a dialog requesting user permission to enable Bluetooth.*/
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        /* register BroadCastReceiver to listen for bond state changes while pairing device*/
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(pairingBroadCastReceiver, intentFilter);

        doBindBleService();

        /*Register BroadcastReceiver for BluetoothLeService */
        IntentFilter filter = new IntentFilter(BluetoothLeService.ACTION_BLE_SERVICE);
        filter.addAction(BluetoothLeService.ACTION_BLE_DEVICE_PAIRED);
        registerReceiver(gattUpdateReceiver, filter);

        if (mBluetoothLeScanner != null) {
            mScanButton.setVisibility(View.VISIBLE);
            /* if everything went right we can set the onClickListener to start scan */
            mScanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*stop scanning */
                    if (mScanning) {
                        mProgressIndicator.setVisibility(View.GONE);
                        mScanButton.setText(getResources().getString(R.string.scan_for_device));
                        Log.d(PairDeviceActivity.class.getCanonicalName(), "Stopped Scanning");
                        mScanning = false;
                        mBluetoothLeScanner.stopScan(leScanCallback);
                    } else {
                        /*start scanning*/
                        if (mBluetoothLeScanner != null) {
                            mProgressIndicator.setVisibility(View.VISIBLE);
                            new Thread(new PairDeviceActivity.scanLeDevice()).start();
                        }
                    }
                }
            });
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(MainActivity.class.getCanonicalName(), "Trying to bind service ");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("ServiceConnection", "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }

    };

    /**
     * Scan filter is set to only scan for BLE devices with a Blood Pressure Service.
     */
    class scanLeDevice implements Runnable {

        @Override
        public void run() {
            Handler handler = new Handler(getMainLooper());
            if (!mScanning) {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        /* stop scanning if scan is not yet stopped by user */
                        if (mScanning) {
                            mScanning = false;
                            mProgressIndicator.setVisibility(View.GONE);
                            mScanButton.setText(getResources().getString(R.string.scan_for_device));
                            Log.d(PairDeviceActivity.class.getCanonicalName(), "Stopped Scanning");
                            mBluetoothLeScanner.stopScan(leScanCallback);
                        }
                    }
                }, SCAN_PERIOD);

                mScanning = true;
                mProgressIndicator.setVisibility(View.VISIBLE);
                /*Scan only for BloodPressure Service*/
                ScanFilter bpUuid = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BloodPressureService.toString())).build();
                ArrayList scanFilterList = new ArrayList();
                scanFilterList.add(bpUuid);

                ScanSettings scanSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
                mBluetoothLeScanner.startScan(scanFilterList, scanSettings, leScanCallback);

                Log.d(PairDeviceActivity.class.getCanonicalName(), "Started Scanning");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.clear();
                        mScanButton.setText("Stop");
                    }
                });
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        mProgressIndicator.setVisibility(View.GONE);
                        mScanButton.setText(getResources().getString(R.string.scan_for_device));
                        Log.d(PairDeviceActivity.class.getCanonicalName(), "Stopped Scanning");
                        mBluetoothLeScanner.stopScan(leScanCallback);
                    }
                });
            }
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
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBleDeviceList != null) {
                                if (mBleDeviceList.contains(result.getDevice())) {
                                    // do not add devices that are already in the list
                                } else {
                                    mBleDeviceList.add(result.getDevice());
                                    mAdapter.notifyDataSetChanged();
                                    /* set onClickListener to start pairing with the device that was clicked only if a device was found*/
                                    mAdapter.setOnListItemClickListener(new BleDevicesListAdapter.BleListItemClickListener() {
                                        @Override
                                        public void onListItemClick(BluetoothDevice item) {
                                            Log.d(PairDeviceActivity.class.getCanonicalName(), "Stopped Scanning");
                                            mScanning = false;
                                            mBluetoothLeScanner.stopScan(leScanCallback);
                                            mProgressIndicator.setVisibility(View.GONE);
                                            mScanButton.setText(getResources().getString(R.string.scan_for_device));
                                            mBluetoothDevice = item;
                                            mBluetoothLeService.connect(item.getAddress());
                                        }
                                    });
                                }
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

            if (BluetoothLeService.ACTION_BLE_DEVICE_PAIRED.equals(action)) {
                Log.d("AD", "Device paired ");
                doUnbindBleRService();
                mBluetoothLeService = null;
                Intent returnIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                returnIntent.putExtra(DEVICE_KEY, (Parcelable) mBluetoothDevice);
                returnIntent.putExtra(PAIRED_DEVICE_ADDRESS, mBluetoothDevice.getAddress());
                PairDeviceActivity.this.setResult(DEVICE_PAIRED, returnIntent);
                finish();
            }
        }
    };

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
            mBluetoothLeService = null;
            mIsBindBleService = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (mScanning) {
            Log.d(PairDeviceActivity.class.getCanonicalName(), "Stopped Scanning");
            mBluetoothLeScanner.stopScan(leScanCallback);
        }
        doUnbindBleRService();
        mBluetoothLeService = null;
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        doBindBleService();
        super.onResume();
    }

    /**
     * BroadcastReceiver for bond state change while pairing
     */
    private final BroadcastReceiver pairingBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            /* bond state changed */
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "Bonding... ");
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    /* pairing is done*/
                    Log.d(TAG, "Bonded!!");
                    if (mBluetoothDevice != null && mBluetoothLeService != null) {
                        mBluetoothLeService.setOperation(PAIR_OPERATION);
                        mBluetoothLeService.connect(mBluetoothDevice.getAddress());
                    }
                } else if (state == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "Not Bonded");
                }
            }
        }
    };

}