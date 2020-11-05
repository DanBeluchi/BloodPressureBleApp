package com.example.BloodPressureBleApp.Database;

import com.example.BloodPressureBleApp.Model.BloodPressureMeasurement;

import java.util.List;

public interface IMeasurementResultsDAO {
    public List<BloodPressureMeasurement> fetchAllMeasurementsFromUserByID(long id);

    // add measurement
    public BloodPressureMeasurement addMeasurementResult(String systolic, String diastolic, String pulse, String timeStamp, long userID);

    // delete measurement
    public int deleteMeasurementById(long measurementID);
}
