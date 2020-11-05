package com.example.BloodPressureBleApp.Database;

import com.example.BloodPressureBleApp.Model.User;

import java.util.List;

public interface IUserDAO {
    public User fetchUserById(int userId);

    public User fetchUserByName(String name);

    public List<User> fetchAllUsers();

    // add user
    public long addUser(User user);

    // add users in bulk
    public boolean deleteAllUsers();
}

