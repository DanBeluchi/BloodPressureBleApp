package com.example.BloodPressureBleApp.Ble;

/**
 * Created by sbhattacharya on 3/29/18.
 */

import java.util.UUID;


public class ADGattUUID {
    public static final UUID ClientCharacteristicConfiguration = uuidFromShortString("2902");
    /*
     * Services
     */
    public static final UUID BloodPressureService = uuidFromShortString("1810");
    public static final UUID CurrentTimeService = uuidFromShortString("1805");

    /*
     * Characteristics
     */
    public static final UUID BloodPressureMeasurement = uuidFromShortString("2a35");
    public static final UUID DateTime = uuidFromShortString("2a08");

    /**
     * Reconstruct the full 128-bit UUID from the shortened version.
     * Insert the short value into the Bluetooth Base UUID:
     *
     * @param uuid UUID of a service
     * @return 128-Bit UUID as a string
     */
    public static UUID uuidFromShortString(String uuid) {
        return UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", uuid));
    }
}
