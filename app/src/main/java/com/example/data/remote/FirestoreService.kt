package com.example.data.remote

import android.util.Log
import com.example.data.model.Donation
import com.example.data.model.FoodItem
import com.example.data.model.FoodNeedRequest
import com.example.data.model.ReceiverType
import com.example.data.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service to manage Firestore collections:
 * 1. "inventory" -> Pantry Inventory Management
 * 2. "donations" -> Food Donations & Deliveries
 * 3. "donation_requests" -> Community Food Needs & NGO Requests
 * 4. "users" -> User Profiles & Authentication Sync
 */
class FirestoreService {
    private val tag = "FirestoreService"

    companion object {
        const val COLLECTION_INVENTORY = "inventory"
        const val COLLECTION_DONATIONS = "donations"
        const val COLLECTION_DONATION_REQUESTS = "donation_requests"
        const val COLLECTION_USERS = "users"
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Firestore not initialized (likely running without Google Services config): ${e.message}")
            null
        }
    }

    val isAvailable: Boolean
        get() = firestore != null

    // Safe extension to await Google Play Tasks without extra libraries
    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }

    /**
     * Initializes all Firestore collections and seeds baseline documents if empty
     */
    fun initializeCollections() {
        initializeInventoryCollection()
        initializeDonationsCollection()
        initializeDonationRequestsCollection()
        initializeUserProfilesCollection()
    }

    /**
     * Initialize "inventory" collection with baseline document if empty
     */
    fun initializeInventoryCollection() {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_INVENTORY).limit(1).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        Log.d(tag, "Initializing $COLLECTION_INVENTORY collection with baseline item")
                        val baselineItem = hashMapOf(
                            "id" to 1L,
                            "userId" to 1L,
                            "name" to "Organic Whole Milk",
                            "category" to "DAIRY",
                            "quantity" to 1.0,
                            "unit" to "gallon",
                            "storageLocation" to "FRIDGE",
                            "status" to "IN_STOCK",
                            "priceEstimate" to 4.29,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection(COLLECTION_INVENTORY).document("1").set(baselineItem)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Inventory collection initialization notice: ${e.message}")
        }
    }

    /**
     * Initialize "donations" collection with baseline document if empty
     */
    fun initializeDonationsCollection() {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_DONATIONS).limit(1).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        Log.d(tag, "Initializing $COLLECTION_DONATIONS collection with baseline donation")
                        val baselineDonation = hashMapOf(
                            "id" to 101L,
                            "userId" to 1L,
                            "donorName" to "Golden Gate Bistro & Bakery",
                            "donorNumber" to "+1 (555) 234-5678",
                            "foodTitle" to "50 Fresh Artisan Pastries & Bread",
                            "category" to "BAKERY",
                            "quantity" to "50 servings",
                            "servingsEstimate" to 50,
                            "status" to "AVAILABLE",
                            "pickupAddress" to "450 Mission St, San Francisco, CA",
                            "dateLogged" to System.currentTimeMillis()
                        )
                        db.collection(COLLECTION_DONATIONS).document("101").set(baselineDonation)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Donations collection initialization notice: ${e.message}")
        }
    }

    /**
     * Initialize "donation_requests" collection with baseline document if empty
     */
    fun initializeDonationRequestsCollection() {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_DONATION_REQUESTS).limit(1).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        Log.d(tag, "Initializing $COLLECTION_DONATION_REQUESTS collection with baseline request")
                        val baselineRequest = hashMapOf(
                            "id" to 201L,
                            "receiverType" to "NGO",
                            "requesterName" to "Hope Harvest Food Shelter (NGO)",
                            "title" to "Need 40 Warm Meals for Evening Soup Kitchen",
                            "peopleCount" to 40,
                            "urgency" to "Immediate (<2h)",
                            "phone" to "+1 (555) 987-6543",
                            "dropAddress" to "1200 Hope Way, Suite 4",
                            "notes" to "Hot cooked meals or soups greatly appreciated.",
                            "status" to "PENDING",
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection(COLLECTION_DONATION_REQUESTS).document("201").set(baselineRequest)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Donation requests collection initialization notice: ${e.message}")
        }
    }

    /**
     * Initialize "users" collection with baseline document if empty
     */
    fun initializeUserProfilesCollection() {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_USERS).limit(1).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) {
                        Log.d(tag, "Initializing $COLLECTION_USERS collection with baseline user")
                        val baselineUser = hashMapOf(
                            "id" to 1L,
                            "name" to "Eco Chef Alex",
                            "email" to "alex@kitchen.org",
                            "role" to "DONOR",
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection(COLLECTION_USERS).document("1").set(baselineUser)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "User profiles collection initialization notice: ${e.message}")
        }
    }

    // =========================================================================
    // 1. PANTRY INVENTORY CRUD OPERATIONS ("inventory")
    // =========================================================================

    /**
     * CREATE / SYNC: Add or replace pantry item in Firestore
     */
    suspend fun createInventoryItem(item: FoodItem): Boolean {
        val db = firestore ?: return false
        return try {
            val docId = if (item.id > 0) item.id.toString() else System.currentTimeMillis().toString()
            val data = hashMapOf(
                "id" to item.id,
                "userId" to item.userId,
                "name" to item.name,
                "category" to item.category,
                "quantity" to item.quantity,
                "unit" to item.unit,
                "storageLocation" to item.storageLocation,
                "purchaseDate" to item.purchaseDate,
                "expiryDate" to item.expiryDate,
                "status" to item.status,
                "priceEstimate" to item.priceEstimate,
                "notes" to item.notes,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_INVENTORY).document(docId).set(data, SetOptions.merge()).awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error creating inventory item in Firestore: ${e.message}")
            false
        }
    }

    /**
     * READ: Observe real-time inventory stream from Firestore
     */
    fun observeInventory(userId: Long? = null): Flow<List<FoodItem>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            val query = if (userId != null && userId > 0) {
                db.collection(COLLECTION_INVENTORY).whereEqualTo("userId", userId)
            } else {
                db.collection(COLLECTION_INVENTORY)
            }

            registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing inventory: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            FoodItem(
                                id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L),
                                userId = doc.getLong("userId") ?: 1L,
                                name = doc.getString("name") ?: "Unnamed Food",
                                category = doc.getString("category") ?: "PRODUCE",
                                quantity = doc.getDouble("quantity") ?: 1.0,
                                unit = doc.getString("unit") ?: "pcs",
                                storageLocation = doc.getString("storageLocation") ?: "FRIDGE",
                                purchaseDate = doc.getLong("purchaseDate") ?: System.currentTimeMillis(),
                                expiryDate = doc.getLong("expiryDate") ?: (System.currentTimeMillis() + 86400000L * 4),
                                status = doc.getString("status") ?: "IN_STOCK",
                                priceEstimate = doc.getDouble("priceEstimate") ?: 3.50,
                                notes = doc.getString("notes") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(items)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to register inventory snapshot listener: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * READ: Get a single inventory item by ID
     */
    suspend fun getInventoryItem(id: Long): FoodItem? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection(COLLECTION_INVENTORY).document(id.toString()).get().awaitTask()
            if (doc != null && doc.exists()) {
                FoodItem(
                    id = doc.getLong("id") ?: id,
                    userId = doc.getLong("userId") ?: 1L,
                    name = doc.getString("name") ?: "",
                    category = doc.getString("category") ?: "PRODUCE",
                    quantity = doc.getDouble("quantity") ?: 1.0,
                    unit = doc.getString("unit") ?: "pcs",
                    storageLocation = doc.getString("storageLocation") ?: "FRIDGE",
                    purchaseDate = doc.getLong("purchaseDate") ?: System.currentTimeMillis(),
                    expiryDate = doc.getLong("expiryDate") ?: System.currentTimeMillis(),
                    status = doc.getString("status") ?: "IN_STOCK",
                    priceEstimate = doc.getDouble("priceEstimate") ?: 3.50,
                    notes = doc.getString("notes") ?: ""
                )
            } else null
        } catch (e: Exception) {
            Log.w(tag, "Error fetching inventory item #$id: ${e.message}")
            null
        }
    }

    /**
     * UPDATE: Update pantry item in Firestore
     */
    suspend fun updateInventoryItem(item: FoodItem): Boolean {
        return createInventoryItem(item)
    }

    /**
     * DELETE: Remove pantry item from Firestore
     */
    suspend fun deleteInventoryItem(id: Long): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection(COLLECTION_INVENTORY).document(id.toString()).delete().awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error deleting inventory item #$id from Firestore: ${e.message}")
            false
        }
    }

    fun syncInventoryItem(item: FoodItem) {
        val db = firestore ?: return
        try {
            val docId = if (item.id > 0) item.id.toString() else System.currentTimeMillis().toString()
            val data = hashMapOf(
                "id" to item.id,
                "userId" to item.userId,
                "name" to item.name,
                "category" to item.category,
                "quantity" to item.quantity,
                "unit" to item.unit,
                "storageLocation" to item.storageLocation,
                "purchaseDate" to item.purchaseDate,
                "expiryDate" to item.expiryDate,
                "status" to item.status,
                "priceEstimate" to item.priceEstimate,
                "notes" to item.notes,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_INVENTORY).document(docId).set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(tag, "Async syncInventoryItem error: ${e.message}")
        }
    }

    // =========================================================================
    // 2. DONATIONS & DONATION REQUESTS CRUD OPERATIONS ("donations" & "donation_requests")
    // =========================================================================

    /**
     * CREATE / SYNC: Create or update surplus food donation
     */
    suspend fun createDonation(donation: Donation): Boolean {
        val db = firestore ?: return false
        return try {
            val docId = if (donation.id > 0) donation.id.toString() else System.currentTimeMillis().toString()
            val data = mapDonationToFirestore(donation)
            db.collection(COLLECTION_DONATIONS).document(docId).set(data, SetOptions.merge()).awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error creating donation in Firestore: ${e.message}")
            false
        }
    }

    /**
     * READ: Real-time listener for all surplus food donations
     */
    fun observeDonations(): Flow<List<Donation>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection(COLLECTION_DONATIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Error observing donations: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val donations = snapshot.documents.mapNotNull { doc ->
                            try {
                                Donation(
                                    id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L),
                                    userId = doc.getLong("userId") ?: 1L,
                                    donorName = doc.getString("donorName") ?: "Donor",
                                    donorNumber = doc.getString("donorNumber") ?: "",
                                    foodTitle = doc.getString("foodTitle") ?: "Surplus Food",
                                    category = doc.getString("category") ?: "COOKED_MEALS",
                                    quantity = doc.getString("quantity") ?: "1",
                                    servingsEstimate = doc.getLong("servingsEstimate")?.toInt() ?: 10,
                                    weightKg = doc.getDouble("weightKg") ?: 5.0,
                                    foodPhotoUri = doc.getString("foodPhotoUri") ?: "",
                                    foodPresetName = doc.getString("foodPresetName") ?: "COOKED_MEAL",
                                    pickupAddress = doc.getString("pickupAddress") ?: "",
                                    dropAddress = doc.getString("dropAddress") ?: "",
                                    status = doc.getString("status") ?: "AVAILABLE",
                                    receiverType = doc.getString("receiverType") ?: "",
                                    receiverName = doc.getString("receiverName") ?: "",
                                    receiverPhone = doc.getString("receiverPhone") ?: "",
                                    receiverDetails = doc.getString("receiverDetails") ?: "",
                                    deliveryMethod = doc.getString("deliveryMethod") ?: "VOLUNTEER_DELIVERY",
                                    courierName = doc.getString("courierName") ?: "Eco Courier",
                                    courierPhone = doc.getString("courierPhone") ?: "",
                                    etaMinutes = doc.getLong("etaMinutes")?.toInt() ?: 15,
                                    deliveryProgress = doc.getDouble("deliveryProgress")?.toFloat() ?: 0.0f,
                                    specialInstructions = doc.getString("specialInstructions") ?: "",
                                    dateLogged = doc.getLong("dateLogged") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(donations)
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to register donation snapshot listener: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * READ: Get a single donation by ID
     */
    suspend fun getDonation(id: Long): Donation? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection(COLLECTION_DONATIONS).document(id.toString()).get().awaitTask()
            if (doc != null && doc.exists()) {
                Donation(
                    id = doc.getLong("id") ?: id,
                    userId = doc.getLong("userId") ?: 1L,
                    donorName = doc.getString("donorName") ?: "Donor",
                    donorNumber = doc.getString("donorNumber") ?: "",
                    foodTitle = doc.getString("foodTitle") ?: "Surplus Food",
                    category = doc.getString("category") ?: "COOKED_MEALS",
                    quantity = doc.getString("quantity") ?: "1",
                    servingsEstimate = doc.getLong("servingsEstimate")?.toInt() ?: 10,
                    pickupAddress = doc.getString("pickupAddress") ?: "",
                    status = doc.getString("status") ?: "AVAILABLE"
                )
            } else null
        } catch (e: Exception) {
            Log.w(tag, "Error fetching donation #$id: ${e.message}")
            null
        }
    }

    /**
     * UPDATE: Update donation details / claim state
     */
    suspend fun updateDonation(donation: Donation): Boolean {
        return createDonation(donation)
    }

    /**
     * DELETE: Remove donation from Firestore
     */
    suspend fun deleteDonation(id: Long): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection(COLLECTION_DONATIONS).document(id.toString()).delete().awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error deleting donation #$id from Firestore: ${e.message}")
            false
        }
    }

    fun syncDonation(donation: Donation) {
        val db = firestore ?: return
        try {
            val docId = if (donation.id > 0) donation.id.toString() else System.currentTimeMillis().toString()
            val data = mapDonationToFirestore(donation)
            db.collection(COLLECTION_DONATIONS).document(docId).set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(tag, "Exception during Firestore donation sync: ${e.message}")
        }
    }

    // --- Donation Requests (Food Need Broadcasts by NGOs / Individuals) ---

    /**
     * CREATE / SYNC: Create or update food need request in Firestore
     */
    suspend fun createDonationRequest(request: FoodNeedRequest): Boolean {
        val db = firestore ?: return false
        return try {
            val docId = if (request.id > 0) request.id.toString() else System.currentTimeMillis().toString()
            val data = hashMapOf(
                "id" to request.id,
                "receiverType" to request.receiverType.name,
                "requesterName" to request.requesterName,
                "title" to request.title,
                "peopleCount" to request.peopleCount,
                "urgency" to request.urgency,
                "phone" to request.phone,
                "dropAddress" to request.dropAddress,
                "notes" to request.notes,
                "latitude" to request.latitude,
                "longitude" to request.longitude,
                "distanceMiles" to request.distanceMiles,
                "category" to request.category,
                "timestamp" to request.timestamp,
                "status" to "PENDING",
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_DONATION_REQUESTS).document(docId).set(data, SetOptions.merge()).awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error creating donation request in Firestore: ${e.message}")
            false
        }
    }

    /**
     * READ: Observe real-time donation requests / community needs stream
     */
    fun observeDonationRequests(): Flow<List<FoodNeedRequest>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection(COLLECTION_DONATION_REQUESTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Error observing donation requests: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val requests = snapshot.documents.mapNotNull { doc ->
                            try {
                                val typeStr = doc.getString("receiverType") ?: "NGO"
                                val type = if (typeStr.equals("INDIVIDUAL", ignoreCase = true)) ReceiverType.INDIVIDUAL else ReceiverType.NGO
                                FoodNeedRequest(
                                    id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L),
                                    receiverType = type,
                                    requesterName = doc.getString("requesterName") ?: "Community Requester",
                                    title = doc.getString("title") ?: "Food Need",
                                    peopleCount = doc.getLong("peopleCount")?.toInt() ?: 20,
                                    urgency = doc.getString("urgency") ?: "Today",
                                    phone = doc.getString("phone") ?: "",
                                    dropAddress = doc.getString("dropAddress") ?: "",
                                    notes = doc.getString("notes") ?: "",
                                    latitude = doc.getDouble("latitude") ?: 37.7749,
                                    longitude = doc.getDouble("longitude") ?: -122.4194,
                                    distanceMiles = doc.getDouble("distanceMiles") ?: 1.0,
                                    category = doc.getString("category") ?: "Cooked Meals",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(requests)
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to register donation request listener: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * READ: Get a single donation request by ID from Firestore
     */
    suspend fun getDonationRequest(id: Long): FoodNeedRequest? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection(COLLECTION_DONATION_REQUESTS).document(id.toString()).get().awaitTask()
            if (doc != null && doc.exists()) {
                val typeStr = doc.getString("receiverType") ?: "NGO"
                val type = if (typeStr.equals("INDIVIDUAL", ignoreCase = true)) ReceiverType.INDIVIDUAL else ReceiverType.NGO
                FoodNeedRequest(
                    id = doc.getLong("id") ?: id,
                    receiverType = type,
                    requesterName = doc.getString("requesterName") ?: "Community Requester",
                    title = doc.getString("title") ?: "Food Need",
                    peopleCount = doc.getLong("peopleCount")?.toInt() ?: 20,
                    urgency = doc.getString("urgency") ?: "Today",
                    phone = doc.getString("phone") ?: "",
                    dropAddress = doc.getString("dropAddress") ?: "",
                    notes = doc.getString("notes") ?: "",
                    latitude = doc.getDouble("latitude") ?: 37.7749,
                    longitude = doc.getDouble("longitude") ?: -122.4194,
                    distanceMiles = doc.getDouble("distanceMiles") ?: 1.0,
                    category = doc.getString("category") ?: "Cooked Meals",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            Log.w(tag, "Error fetching donation request #$id from Firestore: ${e.message}")
            null
        }
    }

    /**
     * UPDATE: Update donation request
     */
    suspend fun updateDonationRequest(request: FoodNeedRequest): Boolean {
        return createDonationRequest(request)
    }

    /**
     * DELETE: Delete donation request from Firestore
     */
    suspend fun deleteDonationRequest(id: Long): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection(COLLECTION_DONATION_REQUESTS).document(id.toString()).delete().awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error deleting donation request #$id from Firestore: ${e.message}")
            false
        }
    }

    // =========================================================================
    // 3. USER PROFILES CRUD OPERATIONS ("users")
    // =========================================================================

    /**
     * CREATE / SYNC: Create or sync user profile in Firestore
     */
    suspend fun createUserProfile(user: User, role: String = "DONOR", organization: String = ""): Boolean {
        val db = firestore ?: return false
        return try {
            val docId = if (user.id > 0) user.id.toString() else user.email.replace(".", "_")
            val data = hashMapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "role" to role,
                "organization" to organization,
                "securityQuestion" to user.securityQuestion,
                "createdAt" to user.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_USERS).document(docId).set(data, SetOptions.merge()).awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error creating user profile in Firestore: ${e.message}")
            false
        }
    }

    /**
     * READ: Get user profile by user ID
     */
    suspend fun getUserProfile(userId: Long): Map<String, Any?>? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection(COLLECTION_USERS).document(userId.toString()).get().awaitTask()
            if (doc != null && doc.exists()) {
                doc.data
            } else null
        } catch (e: Exception) {
            Log.w(tag, "Error fetching user profile #$userId: ${e.message}")
            null
        }
    }

    /**
     * READ: Observe user profile in real-time
     */
    fun observeUserProfile(userId: Long): Flow<Map<String, Any?>?> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection(COLLECTION_USERS).document(userId.toString())
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Error observing user profile: ${error.message}")
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.data)
                }
        } catch (e: Exception) {
            Log.e(tag, "Failed to register user profile listener: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * UPDATE: Update user profile fields (name, phone, organization, etc.)
     */
    suspend fun updateUserProfile(userId: Long, updates: Map<String, Any?>): Boolean {
        val db = firestore ?: return false
        return try {
            val dataWithTimestamp = updates.toMutableMap()
            dataWithTimestamp["updatedAt"] = System.currentTimeMillis()
            db.collection(COLLECTION_USERS).document(userId.toString()).set(dataWithTimestamp, SetOptions.merge()).awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error updating user profile #$userId: ${e.message}")
            false
        }
    }

    /**
     * DELETE: Delete user profile from Firestore
     */
    suspend fun deleteUserProfile(userId: Long): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection(COLLECTION_USERS).document(userId.toString()).delete().awaitTask()
            true
        } catch (e: Exception) {
            Log.w(tag, "Error deleting user profile #$userId: ${e.message}")
            false
        }
    }

    // --- Helper Mappers ---
    private fun mapDonationToFirestore(donation: Donation): HashMap<String, Any?> {
        return hashMapOf(
            "id" to donation.id,
            "userId" to donation.userId,
            "donorName" to donation.donorName,
            "donorNumber" to donation.donorNumber,
            "foodTitle" to donation.foodTitle,
            "category" to donation.category,
            "quantity" to donation.quantity,
            "servingsEstimate" to donation.servingsEstimate,
            "weightKg" to donation.weightKg,
            "foodPhotoUri" to donation.foodPhotoUri,
            "foodPresetName" to donation.foodPresetName,
            "pickupAddress" to donation.pickupAddress,
            "dropAddress" to donation.dropAddress,
            "status" to donation.status,
            "receiverType" to donation.receiverType,
            "receiverName" to donation.receiverName,
            "receiverPhone" to donation.receiverPhone,
            "receiverDetails" to donation.receiverDetails,
            "deliveryMethod" to donation.deliveryMethod,
            "courierName" to donation.courierName,
            "courierPhone" to donation.courierPhone,
            "etaMinutes" to donation.etaMinutes,
            "deliveryProgress" to donation.deliveryProgress,
            "specialInstructions" to donation.specialInstructions,
            "dateLogged" to donation.dateLogged,
            "updatedAt" to System.currentTimeMillis()
        )
    }
}
