@file:Suppress("PackageName")
package org.example.CapstoneProject.model

// -------------------------------------------------------------------------
// Represents a user in the application.
//
// The model keeps the same fields and constructor behavior as the
// original Java class.
// -------------------------------------------------------------------------
@Suppress("unused")
class User {
    var userName: String? = null // Username field.
    var password: String? = null // Password field.
    var age: Int = 0 // Age field.
    var fullName: String? = null // Full name field.
    var bmi: Double = 0.0 // BMI field that can be updated separately.

    // Water log field containing per-day water data.
    // Key = date in yyyy-MM-dd format, Value = list of 13 numbers containing the total and cup values.
    var waterLog: Map<String, List<Long>>? = null
    var calories: Int = 0 // Total daily calories value.
    var goalMl: Int = 3000 // Daily hydration goal in milliliters.

    // ---------------------------------------------------------------------
    // Mandatory no-argument constructor.
    // ---------------------------------------------------------------------
    constructor()

    // ---------------------------------------------------------------------
    // Creates a user with username and password only.
    // ---------------------------------------------------------------------
    constructor(userName: String, password: String) {
        this.userName = userName
        this.password = password
        this.goalMl = 3000
    }

    // ---------------------------------------------------------------------
    // Creates a user with the main registration fields.
    // ---------------------------------------------------------------------
    constructor(userName: String, password: String, age: Int, fullName: String) {
        this.userName = userName
        this.password = password
        this.age = age
        this.fullName = fullName
        this.calories = 0
        this.goalMl = 3000
    }

    // ---------------------------------------------------------------------
    // Returns a readable representation of the user for debugging.
    //
    // The real password is intentionally never included.
    // ---------------------------------------------------------------------
    override fun toString(): String {
        return "User{" +
                "userName='$userName'" + ", password='*********'" + ", age=$age" +
                ", fullName='$fullName'" + ", bmi=$bmi" + ", calories=$calories" + ", goalMl=$goalMl" +
                ", waterLog=${waterLog?.toString() ?: "null"}" +
                '}'
    }
}