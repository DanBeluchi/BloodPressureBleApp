package com.example.BloodPressureBleApp.Ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.example.BloodPressureBleApp.Ble.ADGattUUID.BloodPressureMeasurement;
import static com.example.BloodPressureBleApp.Ble.ADGattUUID.BloodPressureService;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private static BluetoothLeService bleService;
    private String mBluetoothDeviceAddress;
    public String operation = "pair";
    private Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    private long indicationDelay = Long.MIN_VALUE;

    private int mConnectionState = STATE_DISCONNECTED;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";


    public static final String ACTION_BLE_SERVICE = ".BLE_SERVICE";
    public static final String ACTION_BLE_DATA_RECEIVED = ".BLE_DATA_RECEIVED";

    public BluetoothGatt getGatt() {
        return mBluetoothGatt;
    }

    public static BluetoothLeService getInstance() {
        return bleService;
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            Log.d("Bluetooth Scan", "Init Bluetooth");
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }


        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;

                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothDevice device = gatt.getDevice();
            Log.d(TAG, "onServicesDiscovered()" + device.getAddress() + ", " + device.getName() + ", status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //when we first connect to the device we set the time and date
                //Log.d("AD","Case of pair hence set time");
                setupDateTime(gatt);
                //if we connect to get the measurement setnotification
                setNotification(gatt, true);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }

        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged()" + "characteristic=" + characteristic.getUuid().toString());
           // byte[] values = characteristic.getValue();
            //ByteBuffer bufS = ByteBuffer.wrap(new byte[]{values[2], values[1]});
            //short sys = bufS.getShort();
            parseCharacteristicValue(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BluetoothDevice device = gatt.getDevice();
            Log.d(TAG, "onCharacteristicWrite()" + device.getAddress() + ", " + device.getName() + " characteristic=" + characteristic.getUuid().toString());
            String serviceUuidString = characteristic.getService().getUuid().toString();
            String characteristicUuidString = characteristic.getUuid().toString();

            if (operation.equalsIgnoreCase("pair")) {
                Log.d("AD", "OnCharacteristic write for pairing aftrer time is set");
                // disconnect();
            } else if (operation.equalsIgnoreCase("data")) {
                Log.d("AD", "entering the condition of getting data");
                if (serviceUuidString.equals(ADGattUUID.CurrentTimeService.toString())
                        || characteristicUuidString.equals(ADGattUUID.DateTime.toString())) {

                    uiThreadHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothGatt gatt = getGatt();
                            Log.d(TAG, "enabling the setIndication");
                            boolean writeResult = setNotification(gatt, true);
                            if (writeResult == false) {
                                Log.d(TAG, "Write Error");
                            }
                        }
                    }, indicationDelay);

                }
            }

        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }


    private final IBinder mBinder = new LocalBinder();

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {

        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean setupDateTime(BluetoothGatt gatt) {
        boolean isSuccess = false;
        if (gatt != null) {
            isSuccess = setDateTimeSetting(gatt, Calendar.getInstance());
        }
        return isSuccess;
    }

    protected boolean setDateTimeSetting(BluetoothGatt gatt, Calendar cal) {
        boolean isSuccess = false;
        BluetoothGattService gattService = gatt.getService(BloodPressureService);

        if (gattService != null) {
            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(ADGattUUID.DateTime);
            if (characteristic != null) {
                Log.d(TAG, "calling the set date time write characteristic");
                characteristic = datewriteCharacteristic(characteristic, cal);
                isSuccess = gatt.writeCharacteristic(characteristic);
            }
        }
        Log.d("SN", "setDateTimeSetting " + cal.getTime());
        return isSuccess;
    }

    public boolean setNotification(BluetoothGatt gatt, boolean enable) {
        boolean isSuccess = false;

        if (gatt != null) {
            BluetoothGattService service = gatt.getService(BloodPressureService);

            if (service != null) {
                Log.d("AD", "the service for which we are setting notification is " + service.getUuid().toString());
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(BloodPressureMeasurement);
                if (characteristic != null) {

                    isSuccess = gatt.setCharacteristicNotification(characteristic, enable);

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(ADGattUUID.ClientCharacteristicConfiguration);

                    if (enable) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                    gatt.writeDescriptor(descriptor);
                } else {
                    Log.d(TAG, "Characteristic NULL");
                }
            } else {
                Log.d(TAG, "Service NULL");
            }
        }
        return isSuccess;
    }

    public static BluetoothGattCharacteristic datewriteCharacteristic(BluetoothGattCharacteristic characteristic, Calendar calendar) {

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);

        byte[] value = {
                (byte) (year & 0x0FF),    // year 2bit
                (byte) (year >> 8),        //
                (byte) month,            // month
                (byte) day,                // day
                (byte) hour,                // hour
                (byte) min,                // min
                (byte) sec                // sec
        };
        characteristic.setValue(value);

        return characteristic;
    }

    public void parseCharacteristicValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (ADGattUUID.BloodPressureMeasurement.equals(characteristic.getUuid())) {
            Log.d("AD", "reading for BP is received");
            int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            String flagString = Integer.toBinaryString(flag);
            String systolic = "";
            String diastolic = "";
            String pulse = "";
            String systolic_display = "";
            String diastolic_display = "";
            String pulse_display = "";
            String timeStamp = "";

            int offset = 0;
            for (int index = flagString.length(); 0 < index; index--) {
                String key = flagString.substring(index - 1, index);

                if (index == flagString.length()) {
                    if (key.equals("0")) {
                        // mmHg
                        Log.d("SN", "mmHg");

                    } else {
                        // kPa
                        Log.d("SN", "kPa");

                    }
                    // Unit
                    offset += 1;
                    Log.d("SN", "Systolic :" + String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                    systolic = String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                    systolic_display = String.format("%.0f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                    offset += 2;

                    Log.d("SN", "Diastolic :" + String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                    diastolic = String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                    diastolic_display = String.format("%.0f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                    offset += 2;

                    Log.d("SN", "Mean Arterial Pressure :" + String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));

                    offset += 2;
                } else if (index == flagString.length() - 1) {
                    if (key.equals("1")) {
                        // Time Stamp
                        String year = String.format("%04d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset));
                        offset += 2;
                        String month = String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset));
                        offset += 1;
                        String day = String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset));
                        offset += 1;
                        String hour = String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset));
                        offset += 1;
                        String minutes = String.format("%02d", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset));
                        //we dont need the seconds so +2
                        offset += 2;

                        timeStamp = day + "-" + month + "-" + year + " " + hour + ":" + minutes;
                    } else {

                        Calendar calendar = Calendar.getInstance(Locale.getDefault());
                        //Use calendar to get the date and time
                        Calendar c = Calendar.getInstance();
                        System.out.println("Current time => " + c.getTime());

                        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                        timeStamp = df.format(c.getTime());

                    }
                } else if (index == flagString.length() - 2) {
                    if (key.equals("1")) {
                        // Pulse Rate
                        Log.d("SN", "Pulse Rate :" + String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset)));
                        pulse = String.format("%f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                        pulse_display = String.format("%.0f", characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset));
                        offset += 2;
                    }
                } else if (index == flagString.length() - 3) {
                    // UserID
                } else if (index == flagString.length() - 4) {
                    // Measurement Status Flag
                    int statusFalg = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                    String statusFlagString = Integer.toBinaryString(statusFalg);
                    for (int i = statusFlagString.length(); 0 < i; i--) {
                        String status = statusFlagString.substring(i - 1, i);
                        if (i == statusFlagString.length()) {
                        } else if (i == statusFlagString.length() - 1) {
                        } else if (i == statusFlagString.length() - 2) {
                        } else if (i == statusFlagString.length() - 3) {
                            i--;
                            String secondStatus = statusFlagString.substring(i - 1, i);
                            if (status.endsWith("1") && secondStatus.endsWith("0")) {
                                Log.d("AD", "Pulse range detection is 1");

                            } else if (status.endsWith("0") && secondStatus.endsWith("1")) {
                                Log.d("AD", "Pulse range detection is 2");
                            } else if (status.endsWith("1") && secondStatus.endsWith("1")) {
                                Log.d("AD", "Pulse range detection is 3");
                            } else {
                                Log.d("AD", "Pulse range detection is 0");
                            }
                        } else if (i == statusFlagString.length() - 5) {
                            Log.d("AD", "Measurment position detection");
                        }
                    }
                }
            }
            Intent intent = new Intent();
            intent.setAction(ACTION_BLE_DATA_RECEIVED);
            intent.putExtra("Systolic", systolic_display);
            intent.putExtra("Diastolic", diastolic_display);
            intent.putExtra("Pulse", pulse_display);
            intent.putExtra("Timestamp", timeStamp);
            sendBroadcast(intent);
        }
    }
}