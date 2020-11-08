package com.example.BloodPressureBleApp.Data;


import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    private long mId;
    private String mName;
    private String mPassword;
    private int mAge;

    public User(String name, String password, int age) {
        this.mName = name;
        this.mPassword = password;
        this.mAge = age;
    }

    public User() {
    }


    protected User(Parcel in) {
        mId = in.readLong();
        mName = in.readString();
        mPassword = in.readString();
        mAge = in.readInt();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public String toString() {
        return mName;
    }

    public long getmId() {
        return mId;
    }

    public void setmId(long mId) {
        this.mId = mId;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmPassword() {
        return mPassword;
    }

    public void setmPassword(String mPassword) {
        this.mPassword = mPassword;
    }

    public int getmAge() {
        return mAge;
    }

    public void setmAge(int mAge) {
        this.mAge = mAge;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mName);
        dest.writeString(mPassword);
        dest.writeInt(mAge);
    }
}
