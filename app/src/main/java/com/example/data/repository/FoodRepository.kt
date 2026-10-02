package com.example.data.repository

import com.example.data.local.DonationDao
import com.example.data.local.FoodItemDao
import com.example.data.local.WasteLogDao
import com.example.data.model.Donation
import com.example.data.model.FoodItem
import com.example.data.model.FoodStatus
import com.example.data.model.Recipe
import com.example.data.model.WasteLog
import com.example.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

class FoodRepository(
    private val foodItemDao: FoodItemDao,
    private val wasteLogDao: WasteLogDao,
    private val donationDao: DonationDao,
    val firestoreService: FirestoreService = FirestoreService()
) {
    fun getInStockItems(userId: Long): Flow<List<FoodItem>> = foodItemDao.getInStockItems(userId)
    fun getAllItems(userId: Long): Flow<List<FoodItem>> = foodItemDao.getAllItems(userId)

    // =========================================================================
    // Firestore Collection Initialization & Direct CRUD for Pantry Inventory
    // =========================================================================

    /**
     * Initializes the Firestore "inventory" collection with baseline data if empty
     */
    fun initializeInventoryCollection() {
        firestoreService.initializeInventoryCollection()
    }

    /**
     * CREATE: Add or replace pantry item directly in Firestore "inventory" collection
     */
    suspend fun createFirestoreInventoryItem(item: FoodItem): Boolean =
        firestoreService.createInventoryItem(item)

    /**
     * READ: Observe real-time pantry inventory stream from Firestore
     */
    fun observeFirestoreInventory(userId: Long? = null): Flow<List<FoodItem>> =
        firestoreService.observeInventory(userId)

    /**
     * READ: Fetch single pantry item by ID from Firestore
     */
    suspend fun getFirestoreInventoryItem(id: Long): FoodItem? =
        firestoreService.getInventoryItem(id)

    /**
     * UPDATE: Update pantry inventory item in Firestore
     */
    suspend fun updateFirestoreInventoryItem(item: FoodItem): Boolean =
        firestoreService.updateInventoryItem(item)

    /**
     * DELETE: Delete pantry inventory item by ID from Firestore
     */
    suspend fun deleteFirestoreInventoryItem(id: Long): Boolean =
        firestoreService.deleteInventoryItem(id)

    // =========================================================================
    // Local Room + Bi-Directional Cloud Sync Operations
    // =========================================================================

    suspend fun insertItem(item: FoodItem): Long {
        val id = foodItemDao.insertItem(item)
        val saved = if (item.id == 0L) item.copy(id = id) else item
        firestoreService.createInventoryItem(saved)
        return id
    }

    suspend fun updateItem(item: FoodItem) {
        foodItemDao.updateItem(item)
        firestoreService.updateInventoryItem(item)
    }

    suspend fun deleteItem(item: FoodItem) {
        foodItemDao.deleteItem(item)
        firestoreService.deleteInventoryItem(item.id)
    }

    suspend fun syncInventoryFromFirestore(userId: Long) {
        if (!firestoreService.isAvailable) return
        try {
            firestoreService.observeInventory(userId).collect { remoteItems ->
                for (item in remoteItems) {
                    foodItemDao.insertItem(item)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun markAsConsumed(item: FoodItem, reason: String = "Cooked / Consumed") {
        val updated = item.copy(status = FoodStatus.CONSUMED.name)
        foodItemDao.updateItem(updated)
        firestoreService.updateInventoryItem(updated)
        val co2 = calculateCo2Savings(item.quantity, item.unit)
        wasteLogDao.insertWasteLog(
            WasteLog(
                userId = item.userId,
                foodName = item.name,
                category = item.category,
                quantityWithUnit = "${item.quantity} ${item.unit}",
                actionType = "RESCUED",
                costAmount = item.priceEstimate,
                co2SavedKg = co2,
                reason = reason
            )
        )
    }

    suspend fun markAsWasted(item: FoodItem, reason: String) {
        val updated = item.copy(status = FoodStatus.WASTED.name)
        foodItemDao.updateItem(updated)
        firestoreService.updateInventoryItem(updated)
        wasteLogDao.insertWasteLog(
            WasteLog(
                userId = item.userId,
                foodName = item.name,
                category = item.category,
                quantityWithUnit = "${item.quantity} ${item.unit}",
                actionType = "WASTED",
                costAmount = item.priceEstimate,
                co2SavedKg = 0.0,
                reason = reason.ifBlank { "Expired before use" }
            )
        )
    }

    suspend fun markAsDonated(item: FoodItem, destination: String, notes: String = "") {
        val updated = item.copy(status = FoodStatus.DONATED.name)
        foodItemDao.updateItem(updated)
        firestoreService.updateInventoryItem(updated)
        val donation = Donation(
            userId = item.userId,
            foodTitle = item.name,
            quantity = "${item.quantity} ${item.unit}",
            destination = destination.ifBlank { "Community Food Drop-off" },
            status = "Completed",
            notes = notes
        )
        val donationId = donationDao.insertDonation(donation)
        firestoreService.createDonation(donation.copy(id = donationId))

        val co2 = calculateCo2Savings(item.quantity, item.unit)
        wasteLogDao.insertWasteLog(
            WasteLog(
                userId = item.userId,
                foodName = item.name,
                category = item.category,
                quantityWithUnit = "${item.quantity} ${item.unit}",
                actionType = "RESCUED",
                costAmount = item.priceEstimate,
                co2SavedKg = co2,
                reason = "Donated to $destination"
            )
        )
    }

    suspend fun cookRecipeAndDeduct(recipe: Recipe, inStockItems: List<FoodItem>): Int {
        var itemsRescuedCount = 0
        var totalSavings = 0.0
        val recipeIngredients = recipe.getIngredientList()

        for (recipeIngredient in recipeIngredients) {
            val matchedItem = inStockItems.firstOrNull { inv ->
                isIngredientMatch(inv.name, recipeIngredient)
            }
            if (matchedItem != null) {
                itemsRescuedCount++
                totalSavings += matchedItem.priceEstimate
                if (matchedItem.quantity <= 1.0) {
                    foodItemDao.updateItem(matchedItem.copy(status = FoodStatus.CONSUMED.name))
                } else {
                    foodItemDao.updateItem(matchedItem.copy(quantity = matchedItem.quantity - 1.0))
                }
            }
        }

        wasteLogDao.insertWasteLog(
            WasteLog(
                userId = inStockItems.firstOrNull()?.userId ?: 1L,
                foodName = recipe.title,
                category = "PREPARED",
                quantityWithUnit = "$itemsRescuedCount items used",
                actionType = "RESCUED",
                costAmount = totalSavings,
                co2SavedKg = itemsRescuedCount * 0.85,
                reason = "Cooked recipe: ${recipe.title}"
            )
        )

        return itemsRescuedCount
    }

    private fun isIngredientMatch(inventoryName: String, recipeIngredient: String): Boolean {
        val inv = inventoryName.trim().lowercase()
        val rec = recipeIngredient.trim().lowercase()
        return inv.contains(rec) || rec.contains(inv) ||
                inv.removeSuffix("s").contains(rec.removeSuffix("s")) ||
                rec.removeSuffix("s").contains(inv.removeSuffix("s"))
    }

    private fun calculateCo2Savings(quantity: Double, unit: String): Double {
        // Average carbon footprint factor ~2.5 kg CO2 per kg food rescued
        val weightInKg = when (unit.lowercase()) {
            "kg" -> quantity
            "g" -> quantity / 1000.0
            "l" -> quantity * 1.0
            "ml" -> quantity / 1000.0
            "slices" -> quantity * 0.04
            else -> quantity * 0.2 // default approx 200g per piece/unit
        }
        return (weightInKg * 2.5).coerceAtLeast(0.1)
    }
}
