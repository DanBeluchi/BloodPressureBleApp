package com.example.BloodPressureBleApp.ble;

/**
 * Created by sbhattacharya on 3/29/18.
 */

import java.util.ArrayList;
import java.util.List;
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
    public static final UUID CurrentTime = uuidFromShortString("2a2b");
    public static final UUID DateTime = uuidFromShortString("2a08");
    public static final UUID FirmwareRevisionString = uuidFromShortString("2a26");


    public static List<UUID> ServicesUUIDs = new ArrayList<UUID>();
    public static List<UUID> MeasuCharacUUIDs = new ArrayList<UUID>();

    static {
        ServicesUUIDs.add(BloodPressureService);
        MeasuCharacUUIDs.add(BloodPressureMeasurement);
    }

    public static UUID uuidFromShortString(String uuid) {
        return UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", uuid));
    }
}
