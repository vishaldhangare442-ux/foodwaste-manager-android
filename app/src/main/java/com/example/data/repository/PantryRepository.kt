package com.example.data.repository

import com.example.data.local.PantryItemDao
import com.example.data.model.PantryItem
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Repository class implementing the Repository Pattern to manage pantry inventory items.
 * Provides a clean abstraction between the local Room PantryItemDao and ViewModels.
 */
class PantryRepository(private val pantryItemDao: PantryItemDao) {

    /**
     * Reactive stream of all pantry inventory items sorted by expiration date.
     */
    val allPantryItems: Flow<List<PantryItem>> = pantryItemDao.getAllPantryItems()

    /**
     * Reactive stream of active (unconsumed) pantry inventory items.
     */
    val activePantryItems: Flow<List<PantryItem>> = pantryItemDao.getActivePantryItems()

    /**
     * Observe a single pantry inventory item by its unique ID.
     */
    fun getItemById(id: Long): Flow<PantryItem?> = pantryItemDao.getPantryItemById(id)

    /**
     * Fetch a single pantry inventory item directly.
     */
    suspend fun getItemByIdDirect(id: Long): PantryItem? = pantryItemDao.getPantryItemByIdDirect(id)

    /**
     * Retrieve pantry inventory items expiring within the specified number of days.
     */
    fun getItemsExpiringSoon(daysAhead: Int = 3): Flow<List<PantryItem>> {
        val thresholdDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysAhead.toLong())
        return pantryItemDao.getItemsExpiringBefore(thresholdDate)
    }

    /**
     * Insert a new pantry inventory item (stores name, quantity, unit, and expiration date).
     */
    suspend fun insertItem(item: PantryItem): Long = pantryItemDao.insertPantryItem(item)

    /**
     * Insert multiple pantry inventory items in a single transaction.
     */
    suspend fun insertItems(items: List<PantryItem>) = pantryItemDao.insertPantryItems(items)

    /**
     * Update an existing pantry inventory item.
     */
    suspend fun updateItem(item: PantryItem) = pantryItemDao.updatePantryItem(item)

    /**
     * Delete a pantry inventory item.
     */
    suspend fun deleteItem(item: PantryItem) = pantryItemDao.deletePantryItem(item)

    /**
     * Delete a pantry inventory item by ID.
     */
    suspend fun deleteItemById(id: Long) = pantryItemDao.deletePantryItemById(id)

    /**
     * Mark an item as consumed in the pantry.
     */
    suspend fun markAsConsumed(id: Long) = pantryItemDao.markAsConsumed(id)

    /**
     * Clear all items from the local pantry inventory table.
     */
    suspend fun clearAll() = pantryItemDao.clearAll()
}
