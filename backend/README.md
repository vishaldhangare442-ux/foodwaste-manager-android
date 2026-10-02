# 🍲 FoodWaste Rescue - Complete Backend Architecture

This project includes a dual-backend architecture:
1. **Cloud / Server Backend (`/backend`)**: Node.js & Express REST API with real-time Socket.io delivery tracking, JWT authentication, and Gemini AI Zero-Waste Chef endpoints.
2. **Android In-App Local & Cloud Sync Backend (`/app/src/main/java/com/example/data`)**: Room SQLite Local Database, reactive Kotlin Coroutines/Flow Repositories, Firebase Firestore remote synchronization, and Gemini AI API client.

---

## 🌐 1. Server Backend (`/backend/server.js`)

### ⚡ Quick Start

```bash
cd backend
npm install
npm run dev   # Starts with nodemon on http://localhost:8080
# or
npm start     # Starts in production mode
```

Or with **Docker Compose**:
```bash
docker compose up -d
```

---

### 📡 API Endpoints Reference

#### 🍽️ 1. Food Donation Coordination & Donor Details

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/donations` | List surplus donations (filters: `status`, `category`, `receiverType`, `donorId`, `search`) |
| `GET` | `/api/donations/:id` | Get single donation with complete donor contact details & delivery timeline |
| `POST` | `/api/donations` | Publish food donation with full donor info, food specs, and storage condition |
| `PUT` | `/api/donations/:id` | Full update of food donation details |
| `PATCH` | `/api/donations/:id/donor-details` | **Update donor details** (phone, contact person, pickup address, instructions, availability window) |
| `POST` | `/api/donations/:id/claim` | **Claim surplus food donation** as **NGO** (shelter) or **Individual** with logistics details |
| `PATCH` | `/api/donations/:id/status` | **Update donation coordination status**: `AVAILABLE` → `CLAIMED` → `IN_TRANSIT` → `DELIVERED` / `CANCELLED` |
| `PATCH` | `/api/donations/:id/advance` | Step forward delivery workflow |
| `DELETE`| `/api/donations/:id` | Cancel/remove donation |

#### 🏢 2. NGO Requests & Status Updates Coordination

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/needs` / `/api/ngo-requests` | List food requests with filters (`type=NGO\|INDIVIDUAL`, `status`, `urgency`) |
| `GET` | `/api/needs/:id` | Get NGO request by ID with organization details and matched donation |
| `POST` | `/api/needs` / `/api/ngo-requests` | Broadcast new food need from NGO shelter or individual family |
| `PUT` | `/api/needs/:id` | Update food need parameters |
| `PATCH` | `/api/needs/:id/status` | **Update NGO request status**: `PENDING` → `MATCHED` → `IN_FULFILLMENT` → `FULFILLED` / `CANCELLED` |
| `POST` | `/api/needs/:id/match-donation` | **Directly coordinate matching** an NGO request with a specific donor's surplus food donation |
| `DELETE`| `/api/needs/:id` | Cancel/remove NGO request |

#### 👨‍🍳 3. Donor Directory & Profiles

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/donors` | List registered and verified food donors |
| `GET` | `/api/donors/:id` | Get donor profile, rating, total meals donated, and active listings |
| `GET` | `/api/donors/:id/donations` | List all donations posted by a specific donor |
| `PATCH` | `/api/donors/:id` | Update donor profile, notification phone, and operating hours |

#### 📦 4. Pantry & Zero-Waste AI

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Server health check and entity counts |
| `POST` | `/api/auth/register` | Register user (`DONOR`, `RECEIVER_NGO`, or `INDIVIDUAL`) |
| `POST` | `/api/auth/login` | Login and receive JWT access token |
| `GET` | `/api/inventory` | List pantry inventory (filter by `location` & `search`) |
| `GET` | `/api/inventory/expiring-soon`| Automatic 3-Day Expiry Radar with total value-at-risk & CO2 impact |
| `POST` | `/api/inventory` | Add item to pantry inventory |
| `DELETE`| `/api/inventory/:id` | Remove consumed/expired food item |
| `GET` | `/api/analytics/impact` | Community impact metrics (meals rescued, kg CO2 diverted, $ saved) |
| `POST` | `/api/recipes/gemini-generate`| AI Zero-Waste recipe generation based on expiring food items |

---

### 📋 Request & Response Examples

#### 1. Update Donor Contact & Pickup Details
`PATCH /api/donations/201/donor-details`
```json
{
  "donorName": "Golden Gate Bistro & Bakery",
  "donorNumber": "+1 (555) 999-1234",
  "contactPerson": "Chef Alex Rivera",
  "pickupAddress": "450 Mission St, Dock #3, San Francisco, CA",
  "pickupInstructions": "Ring kitchen buzzer at loading dock #3. Ask for Alex.",
  "availableUntil": "2026-09-30T18:00:00Z"
}
```

#### 2. Claim Donation as NGO
`POST /api/donations/201/claim`
```json
{
  "receiverType": "NGO",
  "receiverName": "Hope Harvest Food Shelter (NGO)",
  "receiverPhone": "+1 (555) 987-6543",
  "ngoRegistrationId": "NGO-501C-4421",
  "dropAddress": "1200 Hope Way, Suite 4, San Francisco, CA",
  "deliveryMethod": "VOLUNTEER_DELIVERY",
  "volunteerName": "Marcus Chen",
  "estimatedPickupTime": "Within 30 minutes"
}
```

#### 3. Update NGO Request Status
`PATCH /api/needs/301/status`
```json
{
  "status": "FULFILLED",
  "matchedDonationId": 201,
  "actualPeopleFed": 65,
  "fulfillmentNotes": "Successfully picked up and served 65 warm meals for evening soup kitchen.",
  "updatedBy": "St. Vincent Shelter Manager"
}
```

#### 4. Match NGO Request With a Donation
`POST /api/needs/301/match-donation`
```json
{
  "donationId": 201,
  "volunteerName": "Sarah Jenkins",
  "deliveryNotes": "Picking up at 3:30 PM with insulated food transport van."
}
```

---

### 🛰️ Real-Time Delivery Tracking (Socket.io)

- **Event `courier:location_update`**: Courier sends GPS updates `{ donationId, latitude, longitude, heading, speed }`.
- **Event `delivery:{id}:location`**: Receivers and donors listen for live map updates on the streaming delivery route.
- **Event `donation:claimed`**: Broadcasts when an NGO or Individual claims a batch of surplus meals.

---

## 📱 2. Android Client Data Layer (`/app/src/main/java/com/example/data`)

The Android application implements Clean MVVM Architecture with offline-first data persistence:

### 🗄️ Local Persistence (Room Database)
- **`FoodDatabase.kt`**: Central Room SQLite database with destructive migration fallback.
- **`FoodItemDao.kt`**: Pantry item CRUD, in-stock queries, expiry sorting.
- **`DonationAndWasteDao.kt`**: Surplus donations cache, active courier deliveries, and food waste logs.
- **`UserDao.kt`**: Offline user profile and authentication session.
- **`RecipeDao.kt`**: Zero-waste recipe recommendations cache.

### 🔄 Repository Pattern
- **`DonationRepository.kt`**: Coordinates between Room local cache and Firebase Firestore remote collection.
- **`FoodRepository.kt`**: Manages pantry items, days-until-expiry calculations, and consumption metrics.
- **`AuthRepository.kt`**: User sign-in, registration, demo accounts, and guest sessions.
- **`RecipeRepository.kt`**: Recipe storage and bookmarking.

### ☁️ Cloud & AI Integration
- **`FirestoreService.kt`**: Real-time snapshot listener for community food donations and delivery coordinates.
- **`GeminiRecipeService.kt`**: Directly calls the Google Gemini AI API to transform expiring pantry ingredients into chef-quality recipes.
