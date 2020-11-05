package com.example.BloodPressureBleApp.Model;

import android.os.Parcel;
import android.os.Parcelable;

public class BloodPressureMeasurement implements Parcelable {
    long measurementID;
    String systolic = "";
    String diastolic = "";
    String pulse = "";
    String timeStamp = "";
    long userID;

    public BloodPressureMeasurement(long measurementID, String systolic, String diastolic, String pulse, String timeStamp, long userID) {
        this.measurementID = measurementID;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.pulse = pulse;
        this.timeStamp = timeStamp;
        this.userID = userID;
    }

    protected BloodPressureMeasurement(Parcel in) {
        systolic = in.readString();
        diastolic = in.readString();
        pulse = in.readString();
        userID = in.readInt();
    }

    public static final Creator<BloodPressureMeasurement> CREATOR = new Creator<BloodPressureMeasurement>() {
        @Override
        public BloodPressureMeasurement createFromParcel(Parcel in) {
            return new BloodPressureMeasurement(in);
        }

        @Override
        public BloodPressureMeasurement[] newArray(int size) {
            return new BloodPressureMeasurement[size];
        }
    };

    public long getMeasurementID() {
        return measurementID;
    }

    public void setMeasurementID(long measurementID) {
        this.measurementID = measurementID;
    }

    public String getSystolic() {
        return systolic;
    }

    public void setSystolic(String systolic) {
        this.systolic = systolic;
    }

    public String getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(String diastolic) {
        this.diastolic = diastolic;
    }

    public String getPulse() {
        return pulse;
    }

    public void setPulse(String pulse) {
        this.pulse = pulse;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(systolic);
        dest.writeString(diastolic);
        dest.writeString(pulse);
        dest.writeLong(userID);
    }
}
