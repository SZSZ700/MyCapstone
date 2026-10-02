package com.example.myfinaltopapplication

// This is the client-side User model
// It is used ONLY on Android side to send/receive data via REST
@Suppress("unused")
class User {
    // Username field
    var userName: String? = null
    // Password field
    var password: String? = null
    // Age field
    var age: Int = 0
    // Full name field
    var fullName: String? = null

    // Constructor with all fields
    constructor(userName: String?, password: String?, age: Int, fullName: String?) {
        this.userName = userName
        this.password = password
        this.age = age
        this.fullName = fullName
    }

    // Constructor used when receiving user data from the server
    // without receiving the password field.
    constructor(userName: String?, age: Int, fullName: String?) {
        this.userName = userName
        this.age = age
        this.fullName = fullName
    }

    // Constructor with username + password only
    constructor(userName: String?, password: String?) {
        this.userName = userName
        this.password = password
    }

    // For debugging / logging
    override fun toString(): String {
        return "User{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", age=" + age +
                ", fullName='" + fullName + '\'' +
                '}'
    }
}