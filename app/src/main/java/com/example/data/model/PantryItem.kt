package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

/**
 * Room Entity representing a pantry inventory item.
 * Stores name, quantity, unit, and expiration date as required by the schema.
 */
@Entity(tableName = "pantry_inventory")
data class PantryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val expirationDate: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7),
    val category: String = "Pantry",
    val storageLocation: String = "Pantry",
    val isConsumed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Alias for expiration date to support interchangeable naming
     */
    val expiryDate: Long get() = expirationDate

    /**
     * Calculates the remaining shelf-life days until expiration date
     */
    fun daysUntilExpiration(now: Long = System.currentTimeMillis()): Int {
        val diff = expirationDate - now
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}
