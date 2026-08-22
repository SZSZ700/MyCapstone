package org.example.CapstoneProject.model;
import java.util.List;
import java.util.Map;

// This class represents a User model
// It matches the structure expected in Spring Boot and Android side
@SuppressWarnings({"unused", "DataFlowIssue"})
public class User {

    // Username field
    private String userName;

    // Password field
    private String password;

    // Age field
    private int age;

    // Full name field
    private String fullName;

    // BMI field (can be updated separately)
    private double bmi;

    // Water log field → per-day water data
    // Key = date (yyyy-MM-dd), Value = list of 13 numbers [sum, cups...]
    private Map<String, List<Long>> waterLog;

    // Total daily calories field (simple key-value, like bmi)
    // This represents the total calories the user has eaten (for the current day / last update)
    private int calories;

    // Daily hydration goal in milliliters.
    private int goalMl = 3000;

    // Constructor with all fields
    public User(String userName, String password, int age, String fullName) {
        this.userName = userName;
        this.password = password;
        this.age = age;
        this.fullName = fullName;
        this.calories = 0;
        this.goalMl = 3000;
    }

    // Constructor with username and password only
    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
        this.goalMl = 3000;
    }

    // ------------------------------
    // Mandatory no-args constructor
    // Firebase uses this to deserialize
    // ------------------------------
    public User() {
        // Required for Firebase
    }

    // Getter for userName
    public String getUserName() {
        return userName;
    }

    // Setter for userName
    public void setUserName(String userName) {
        this.userName = userName;
    }

    // Getter for password
    public String getPassword() {
        return password;
    }

    // Setter for password
    public void setPassword(String password) {
        this.password = password;
    }

    // Getter for age
    public int getAge() {
        return age;
    }

    // Setter for age
    public void setAge(int age) {
        this.age = age;
    }

    // Getter for fullName
    public String getFullName() {
        return fullName;
    }

    // Setter for fullName
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // Getter for bmi
    public double getBmi() {
        return bmi;
    }

    // Setter for bmi
    public void setBmi(double bmi) {
        this.bmi = bmi;
    }

    // Getter for waterLog
    public Map<String, List<Long>> getWaterLog() {
        return waterLog;
    }

    // Setter for waterLog
    public void setWaterLog(Map<String, List<Long>> waterLog) {
        this.waterLog = waterLog;
    }

    // Getter for calories
    public int getCalories() {
        return calories;
    }

    // Setter for calories
    public void setCalories(int calories) {
        this.calories = calories;
    }

    // Getter for goalMl
    public int getGoalMl() {
        return goalMl;
    }

    // Setter for goalMl
    public void setGoalMl(int goalMl) {
        this.goalMl = goalMl;
    }

    // toString for debugging
    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", password='" + "*********" + '\'' +
                ", age=" + age +
                ", fullName='" + fullName + '\'' +
                ", bmi=" + bmi +
                ", calories=" + calories +
                ", goalMl=" + goalMl +
                ", waterLog=" + (waterLog != null ? waterLog.toString() : "null") +
                '}';
    }
}
