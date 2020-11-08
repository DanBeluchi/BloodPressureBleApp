package com.example.BloodPressureBleApp.Data;

import android.os.Parcel;
import android.os.Parcelable;

public class BloodPressureMeasurement implements Parcelable {
    long mMeasurementId;
    String mSystolic = "";
    String mDiastolic = "";
    String mPulse = "";
    String mTimeStamp = "";
    long mUserId;

    public BloodPressureMeasurement(long measurementId, String systolic, String diastolic, String pulse, String timeStamp, long userID) {
        this.mMeasurementId = measurementId;
        this.mSystolic = systolic;
        this.mDiastolic = diastolic;
        this.mPulse = pulse;
        this.mTimeStamp = timeStamp;
        this.mUserId = userID;
    }


    protected BloodPressureMeasurement(Parcel in) {
        mMeasurementId = in.readLong();
        mSystolic = in.readString();
        mDiastolic = in.readString();
        mPulse = in.readString();
        mTimeStamp = in.readString();
        mUserId = in.readLong();
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

    public long getmMeasurementId() {
        return mMeasurementId;
    }

    public void setmMeasurementId(long mMeasurementId) {
        this.mMeasurementId = mMeasurementId;
    }

    public String getmSystolic() {
        return mSystolic;
    }

    public void setmSystolic(String mSystolic) {
        this.mSystolic = mSystolic;
    }

    public String getmDiastolic() {
        return mDiastolic;
    }

    public void setmDiastolic(String mDiastolic) {
        this.mDiastolic = mDiastolic;
    }

    public String getmPulse() {
        return mPulse;
    }

    public void setmPulse(String mPulse) {
        this.mPulse = mPulse;
    }

    public String getmTimeStamp() {
        return mTimeStamp;
    }

    public void setmTimeStamp(String mTimeStamp) {
        this.mTimeStamp = mTimeStamp;
    }

    public long getmUserId() {
        return mUserId;
    }

    public void setmUserId(long mUserId) {
        this.mUserId = mUserId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mMeasurementId);
        dest.writeString(mSystolic);
        dest.writeString(mDiastolic);
        dest.writeString(mPulse);
        dest.writeString(mTimeStamp);
        dest.writeLong(mUserId);
    }
}
