package io.github.bx_xd.velotrack.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val weightKg: Double = 75.0,
    val bikeWeightKg: Double = 9.0,
    val cda: Double = 0.36,      // aero drag coefficient × frontal area
    val crr: Double = 0.004,     // rolling resistance coefficient
    val efficiency: Double = 0.97 // drivetrain efficiency
) {
    val totalMassKg get() = weightKg + bikeWeightKg
    val isConfigured get() = weightKg > 30.0
}
