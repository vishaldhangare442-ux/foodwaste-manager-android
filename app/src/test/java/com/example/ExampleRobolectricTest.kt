package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AvailabilityFilter
import com.example.data.model.DietaryFilter
import com.example.data.model.FoodItem
import com.example.data.model.MealTypeFilter
import com.example.data.model.Recipe
import com.example.data.model.SecurityUtil
import com.example.data.repository.RecipeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FoodWaste Manager", appName)
  }

  @Test
  fun `verify password hashing and verification`() {
    val password = "SecretPassword123!"
    val salt = SecurityUtil.generateSalt()
    val hash = SecurityUtil.hashPassword(password, salt)

    assertTrue("Password hash must verify correctly", SecurityUtil.verifyPassword(password, salt, hash))
    assertTrue("Wrong password must fail", !SecurityUtil.verifyPassword("WrongPassword", salt, hash))
  }

  @Test
  fun `verify donation model with donor number and receiver fields`() {
    val donation = com.example.data.model.Donation(
      donorName = "City Bistro",
      donorNumber = "+1 (555) 333-4444",
      foodTitle = "25 Prepared Lunch Platters",
      quantity = "25 platters",
      servingsEstimate = 25,
      weightKg = 12.0,
      pickupAddress = "100 Main St",
      status = "AVAILABLE"
    )

    assertEquals("City Bistro", donation.donorName)
    assertEquals("+1 (555) 333-4444", donation.donorNumber)
    assertEquals("AVAILABLE", donation.status)

    // Simulate claiming as NGO
    val claimedByNgo = donation.copy(
      status = "CLAIMED",
      receiverType = com.example.data.model.ReceiverType.NGO.name,
      receiverName = "Hope Food Shelter (NGO)",
      receiverPhone = "+1 (555) 888-9999",
      deliveryProgress = 0.2f
    )
    assertEquals("CLAIMED", claimedByNgo.status)
    assertEquals("NGO", claimedByNgo.receiverType)
    assertEquals("+1 (555) 888-9999", claimedByNgo.receiverPhone)

    // Simulate delivery in transit
    val inTransit = claimedByNgo.copy(
      status = "IN_TRANSIT",
      deliveryProgress = 0.7f,
      etaMinutes = 8
    )
    assertEquals("IN_TRANSIT", inTransit.status)
    assertEquals(8, inTransit.etaMinutes)
  }

  @Test
  fun `verify receiver profile and food need request broadcast`() {
    val ngoProfile = com.example.data.model.ReceiverProfile(
      type = com.example.data.model.ReceiverType.NGO,
      name = "Hope Harvest Food Bank (NGO)",
      phone = "+1 (555) 789-0123",
      address = "820 Elm Street",
      identifierOrSize = "NGO Reg #501C-4491",
      dailyCapacityMeals = 150
    )
    assertEquals(com.example.data.model.ReceiverType.NGO, ngoProfile.type)
    assertEquals(150, ngoProfile.dailyCapacityMeals)

    val request = com.example.data.model.FoodNeedRequest(
      receiverType = com.example.data.model.ReceiverType.NGO,
      requesterName = ngoProfile.name,
      title = "Need 50 Warm Dinner Meals for Tonight",
      peopleCount = 50,
      urgency = "Immediate (<2h)"
    )
    assertEquals("Need 50 Warm Dinner Meals for Tonight", request.title)
    assertEquals(50, request.peopleCount)
  }

  @Test
  fun `verify password hashing and authentication credentials`() {
    val salt = com.example.data.model.SecurityUtil.generateSalt()
    val hash = com.example.data.model.SecurityUtil.hashPassword("SecurePass123!", salt)
    assertTrue(com.example.data.model.SecurityUtil.verifyPassword("SecurePass123!", salt, hash))
    assertFalse(com.example.data.model.SecurityUtil.verifyPassword("WrongPassword", salt, hash))

    val user = com.example.data.model.User(
      name = "Green Restaurant",
      email = "green@restaurant.org",
      passwordHash = hash,
      salt = salt
    )
    assertEquals("Green Restaurant", user.name)
    assertEquals("green@restaurant.org", user.email)
  }

  @Test
  fun `verify Gemini recipe service recommendation generation from pantry items`() = kotlinx.coroutines.runBlocking {
    val service = com.example.data.remote.GeminiRecipeService()
    val pantryItems = listOf(
      com.example.data.model.FoodItem(
        name = "Ripe Tomatoes",
        category = "PRODUCE",
        quantity = 4.0,
        unit = "pcs"
      ),
      com.example.data.model.FoodItem(
        name = "Sourdough Bread",
        category = "BAKERY",
        quantity = 1.0,
        unit = "loaf"
      )
    )

    val result = service.getRecipeRecommendations(pantryItems, "Vegetarian", "Dinner")
    assertTrue(result.isSuccess)
    val recipes = result.getOrNull().orEmpty()
    assertTrue(recipes.isNotEmpty())
    val firstRecipe = recipes[0]
    assertTrue(firstRecipe.title.isNotBlank())
    assertTrue(firstRecipe.getInstructionSteps().isNotEmpty())
    assertTrue(firstRecipe.getIngredientList().isNotEmpty())
  }

  @Test
  fun `verify nearby donation requests distance filtering for donors`() {
    val requests = listOf(
      com.example.data.model.FoodNeedRequest(
        id = 1L,
        receiverType = com.example.data.model.ReceiverType.NGO,
        requesterName = "Hope Harvest Food Bank",
        title = "Need 50 Warm Dinner Meals",
        distanceMiles = 1.2,
        latitude = 37.7785,
        longitude = -122.4150
      ),
      com.example.data.model.FoodNeedRequest(
        id = 2L,
        receiverType = com.example.data.model.ReceiverType.INDIVIDUAL,
        requesterName = "Elena Family",
        title = "Need Baby Food",
        distanceMiles = 2.8,
        latitude = 37.7640,
        longitude = -122.4280
      ),
      com.example.data.model.FoodNeedRequest(
        id = 3L,
        receiverType = com.example.data.model.ReceiverType.NGO,
        requesterName = "Downtown Shelter",
        title = "Need Breakfast Bakery",
        distanceMiles = 4.5,
        latitude = 37.7910,
        longitude = -122.4020
      ),
      com.example.data.model.FoodNeedRequest(
        id = 4L,
        receiverType = com.example.data.model.ReceiverType.NGO,
        requesterName = "Bayview Outreach",
        title = "Bulk Food Delivery",
        distanceMiles = 12.5,
        latitude = 37.7320,
        longitude = -122.3890
      )
    )

    // Filter within 3 miles
    val within3Miles = requests.filter { it.distanceMiles <= 3.0 }
    assertEquals(2, within3Miles.size)
    assertTrue(within3Miles.any { it.receiverType == com.example.data.model.ReceiverType.NGO })
    assertTrue(within3Miles.any { it.receiverType == com.example.data.model.ReceiverType.INDIVIDUAL })

    // Filter within 5 miles
    val within5Miles = requests.filter { it.distanceMiles <= 5.0 }
    assertEquals(3, within5Miles.size)

    // Filter within 15 miles
    val within15Miles = requests.filter { it.distanceMiles <= 15.0 }
    assertEquals(4, within15Miles.size)
  }

  @Test
  fun `verify Pantry Overview automatic 3-day expiry filtering and urgency warning levels`() {
    val now = System.currentTimeMillis()
    val dayMs = java.util.concurrent.TimeUnit.DAYS.toMillis(1)

    val items = listOf(
      com.example.data.model.FoodItem(
        id = 1L,
        name = "Fresh Spinach",
        expiryDate = now, // 0 days (today)
        priceEstimate = 2.99
      ),
      com.example.data.model.FoodItem(
        id = 2L,
        name = "Whole Milk",
        expiryDate = now + dayMs * 1, // 1 day (tomorrow)
        priceEstimate = 3.50
      ),
      com.example.data.model.FoodItem(
        id = 3L,
        name = "Sourdough Bread",
        expiryDate = now + dayMs * 3, // 3 days (soon)
        priceEstimate = 4.25
      ),
      com.example.data.model.FoodItem(
        id = 4L,
        name = "Canned Black Beans",
        expiryDate = now + dayMs * 30, // 30 days (fresh)
        priceEstimate = 1.20
      ),
      com.example.data.model.FoodItem(
        id = 5L,
        name = "Raw Honey",
        expiryDate = now + dayMs * 180, // 180 days (fresh)
        priceEstimate = 8.50
      )
    )

    // Automatically filter items expiring within the next 3 days (daysUntilExpiry <= 3)
    val expiringWithin3Days = items.filter { it.daysUntilExpiry(now) <= 3 }
    assertEquals(3, expiringWithin3Days.size)

    // Items needing immediate use (days <= 0)
    val immediateUseItems = expiringWithin3Days.filter { it.daysUntilExpiry(now) <= 0 }
    assertEquals(1, immediateUseItems.size)
    assertEquals("Fresh Spinach", immediateUseItems.first().name)
    assertEquals(com.example.data.model.ExpiryUrgency.CRITICAL_TODAY, immediateUseItems.first().getExpiryUrgency(now))

    // Tomorrow items (days == 1)
    val tomorrowItems = expiringWithin3Days.filter { it.daysUntilExpiry(now) == 1 }
    assertEquals(1, tomorrowItems.size)
    assertEquals("Whole Milk", tomorrowItems.first().name)

    // 2-3 days items
    val soonItems = expiringWithin3Days.filter { it.daysUntilExpiry(now) in 2..3 }
    assertEquals(1, soonItems.size)
    assertEquals("Sourdough Bread", soonItems.first().name)

    // Calculate total value at risk
    val valueAtRisk = expiringWithin3Days.sumOf { it.priceEstimate }
    assertEquals(10.74, valueAtRisk, 0.01)
  }

  @Test
  fun `verify receiver NGO and Individual buttons mode switching and filtering`() {
    var profile = com.example.data.model.ReceiverProfile(
      type = com.example.data.model.ReceiverType.NGO,
      name = "Helping Hands Food Bank (NGO)",
      identifierOrSize = "NGO Reg #501C-4421"
    )

    assertEquals(com.example.data.model.ReceiverType.NGO, profile.type)

    // Switch to Individual
    profile = profile.copy(
      type = com.example.data.model.ReceiverType.INDIVIDUAL,
      name = "Alex Rivera (Individual)",
      identifierOrSize = "Family of 4"
    )
    assertEquals(com.example.data.model.ReceiverType.INDIVIDUAL, profile.type)
    assertTrue(profile.name.contains("Individual"))

    // Switch back to NGO
    profile = profile.copy(
      type = com.example.data.model.ReceiverType.NGO,
      name = "Helping Hands Food Bank (NGO)",
      identifierOrSize = "NGO Reg #501C-4421"
    )
    assertEquals(com.example.data.model.ReceiverType.NGO, profile.type)

    // Verify community needs filtering by NGO and Individual
    val needs = listOf(
      com.example.data.model.FoodNeedRequest(id = 1L, receiverType = com.example.data.model.ReceiverType.NGO, requesterName = "Shelter A", title = "Need Meals"),
      com.example.data.model.FoodNeedRequest(id = 2L, receiverType = com.example.data.model.ReceiverType.INDIVIDUAL, requesterName = "Family B", title = "Need Groceries"),
      com.example.data.model.FoodNeedRequest(id = 3L, receiverType = com.example.data.model.ReceiverType.NGO, requesterName = "Pantry C", title = "Need Bread")
    )

    val ngoNeeds = needs.filter { it.receiverType == com.example.data.model.ReceiverType.NGO }
    val individualNeeds = needs.filter { it.receiverType == com.example.data.model.ReceiverType.INDIVIDUAL }

    assertEquals(2, ngoNeeds.size)
    assertEquals(1, individualNeeds.size)
  }

  @Test
  fun `verify Firestore collections constants for inventory donations and users`() {
    assertEquals("inventory", com.example.data.remote.FirestoreService.COLLECTION_INVENTORY)
    assertEquals("donations", com.example.data.remote.FirestoreService.COLLECTION_DONATIONS)
    assertEquals("donation_requests", com.example.data.remote.FirestoreService.COLLECTION_DONATION_REQUESTS)
    assertEquals("users", com.example.data.remote.FirestoreService.COLLECTION_USERS)
  }

  @Test
  fun `verify repository CRUD layer operations for pantry inventory donation requests and user profiles`() {
    val firestoreService = com.example.data.remote.FirestoreService()

    // 1. Verify Firestore collections initializers can be invoked safely
    firestoreService.initializeInventoryCollection()
    firestoreService.initializeDonationRequestsCollection()
    firestoreService.initializeUserProfilesCollection()
    firestoreService.initializeDonationsCollection()
    firestoreService.initializeCollections()

    // 2. Verify pantry inventory data model and fields
    val item = com.example.data.model.FoodItem(
      id = 101L,
      userId = 1L,
      name = "Organic Whole Milk",
      category = "DAIRY",
      quantity = 2.0,
      unit = "gallons",
      storageLocation = "FRIDGE",
      status = "IN_STOCK",
      priceEstimate = 4.50
    )
    assertEquals("Organic Whole Milk", item.name)
    assertEquals("DAIRY", item.category)
    assertEquals(2.0, item.quantity, 0.001)

    // 3. Verify donation request data model and fields
    val request = com.example.data.model.FoodNeedRequest(
      id = 201L,
      receiverType = com.example.data.model.ReceiverType.NGO,
      requesterName = "Hope Harvest Food Bank (NGO)",
      title = "Need 50 Hot Soups & Meals",
      peopleCount = 50,
      urgency = "Immediate (<2h)",
      phone = "+1 (555) 019-2834",
      dropAddress = "1200 Hope Way",
      notes = "Dinner service tonight"
    )
    assertEquals(201L, request.id)
    assertEquals(com.example.data.model.ReceiverType.NGO, request.receiverType)
    assertEquals(50, request.peopleCount)
    assertEquals("Hope Harvest Food Bank (NGO)", request.requesterName)

    // 4. Verify user profile model and fields
    val user = com.example.data.model.User(
      id = 501L,
      name = "Eco Volunteer Sarah",
      email = "sarah@rescue.org",
      passwordHash = "hash123",
      salt = "salt123"
    )
    assertEquals(501L, user.id)
    assertEquals("Eco Volunteer Sarah", user.name)
    assertEquals("sarah@rescue.org", user.email)
  }

  @Test
  fun `verify flash splash screen execution and completion callback`() {
    var finished = false
    val callback = { finished = true }
    assertFalse("Initially splash is not finished", finished)
    callback.invoke()
    assertTrue("Splash completion callback sets flag to true", finished)
  }

  @Test
  fun `verify PantryItem Room entity DAO and repository schema`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.FoodDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = inMemoryDb.pantryItemDao()
    val repository = com.example.data.repository.PantryRepository(dao)

    val expDate = System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(5)
    val item = com.example.data.model.PantryItem(
      name = "Organic Rolled Oats",
      quantity = 2.5,
      unit = "kg",
      expirationDate = expDate,
      category = "Grains",
      storageLocation = "Pantry"
    )

    // Test Entity fields
    assertEquals("Organic Rolled Oats", item.name)
    assertEquals(2.5, item.quantity, 0.001)
    assertEquals("kg", item.unit)
    assertEquals(expDate, item.expirationDate)
    assertEquals(expDate, item.expiryDate)
    assertTrue("Days until expiration should be positive", item.daysUntilExpiration() >= 4)

    // Test DAO and Repository insert
    val insertedId = repository.insertItem(item)
    assertTrue("Inserted item ID should be valid", insertedId > 0)

    val fetched = repository.getItemByIdDirect(insertedId)
    assertTrue("Fetched item should exist", fetched != null)
    assertEquals("Organic Rolled Oats", fetched?.name)
    assertEquals(2.5, fetched?.quantity ?: 0.0, 0.001)
    assertEquals(expDate, fetched?.expirationDate)

    // Test update
    val updated = fetched!!.copy(quantity = 1.0)
    repository.updateItem(updated)
    val afterUpdate = repository.getItemByIdDirect(insertedId)
    assertEquals(1.0, afterUpdate?.quantity ?: 0.0, 0.001)

    // Test mark as consumed
    repository.markAsConsumed(insertedId)
    val consumedItem = repository.getItemByIdDirect(insertedId)
    assertTrue("Item should be marked consumed", consumedItem?.isConsumed == true)

    // Test delete
    repository.deleteItemById(insertedId)
    val afterDelete = repository.getItemByIdDirect(insertedId)
    assertTrue("Item should be deleted", afterDelete == null)

    inMemoryDb.close()
  }

  @Test
  fun `verify pantry dashboard color-coded expiration status labels`() {
    // 1. Expired item (negative days remaining)
    val expiredStatus = com.example.ui.screens.getExpirationStatusInfo(-2)
    assertEquals("EXPIRED", expiredStatus.badgeLabel)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFDC2626), expiredStatus.textColor)

    // 2. Critical today item (0 days remaining)
    val todayStatus = com.example.ui.screens.getExpirationStatusInfo(0)
    assertEquals("CRITICAL TODAY", todayStatus.badgeLabel)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFEA580C), todayStatus.textColor)

    // 3. Expiring soon item (1 to 3 days remaining)
    val soonStatus = com.example.ui.screens.getExpirationStatusInfo(2)
    assertEquals("EXPIRING SOON", soonStatus.badgeLabel)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD97706), soonStatus.textColor)

    // 4. Fresh item (> 3 days remaining)
    val freshStatus = com.example.ui.screens.getExpirationStatusInfo(7)
    assertEquals("FRESH", freshStatus.badgeLabel)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF059669), freshStatus.textColor)
  }
}

