package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PantryItem
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for local Room operations on pantry inventory items.
 * Supports reactive Flow observations, expiration date sorting, and CRUD operations.
 */
@Dao
interface PantryItemDao {

    @Query("SELECT * FROM pantry_inventory ORDER BY expirationDate ASC")
    fun getAllPantryItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_inventory WHERE isConsumed = 0 ORDER BY expirationDate ASC")
    fun getActivePantryItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_inventory WHERE id = :id LIMIT 1")
    fun getPantryItemById(id: Long): Flow<PantryItem?>

    @Query("SELECT * FROM pantry_inventory WHERE id = :id LIMIT 1")
    suspend fun getPantryItemByIdDirect(id: Long): PantryItem?

    @Query("SELECT * FROM pantry_inventory WHERE isConsumed = 0 AND expirationDate <= :thresholdDate ORDER BY expirationDate ASC")
    fun getItemsExpiringBefore(thresholdDate: Long): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItems(items: List<PantryItem>)

    @Update
    suspend fun updatePantryItem(item: PantryItem)

    @Delete
    suspend fun deletePantryItem(item: PantryItem)

    @Query("DELETE FROM pantry_inventory WHERE id = :id")
    suspend fun deletePantryItemById(id: Long)

    @Query("UPDATE pantry_inventory SET isConsumed = 1 WHERE id = :id")
    suspend fun markAsConsumed(id: Long)

    @Query("SELECT COUNT(*) FROM pantry_inventory WHERE isConsumed = 0")
    fun getActiveCount(): Flow<Int>

    @Query("DELETE FROM pantry_inventory")
    suspend fun clearAll()
}
