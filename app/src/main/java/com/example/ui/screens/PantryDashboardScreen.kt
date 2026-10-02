package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodCategory
import com.example.data.model.FoodItem
import com.example.data.model.StorageLocation
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ExpirationFilter(val label: String) {
    ALL("All Items"),
    EXPIRING_SOON("Expiring Soon ⚠️"),
    FRESH("Fresh 🌱"),
    EXPIRED("Expired ❌")
}

/**
 * Pantry Dashboard Screen displaying items stored in the local Room database,
 * highlighting color-coded status badges for items nearing expiration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToDonor: (() -> Unit)? = null
) {
    val inStockItems by viewModel.inStockItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ExpirationFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Statistics counts
    val now = System.currentTimeMillis()
    val expiredCount = remember(inStockItems) {
        inStockItems.count { it.daysUntilExpiry(now) < 0 }
    }
    val expiringSoonCount = remember(inStockItems) {
        inStockItems.count { it.daysUntilExpiry(now) in 0..3 }
    }
    val freshCount = remember(inStockItems) {
        inStockItems.count { it.daysUntilExpiry(now) > 3 }
    }

    // Filter items based on user selection and search query
    val filteredItems = remember(inStockItems, searchQuery, selectedFilter) {
        inStockItems.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            val days = item.daysUntilExpiry(now)
            val matchesFilter = when (selectedFilter) {
                ExpirationFilter.ALL -> true
                ExpirationFilter.EXPIRING_SOON -> days in 0..3
                ExpirationFilter.FRESH -> days > 3
                ExpirationFilter.EXPIRED -> days < 0
            }
            matchesSearch && matchesFilter
        }.sortedBy { it.expiryDate }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("pantry_dashboard_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Dashboard Header Banner
            item {
                PantryDashboardHeader(
                    totalCount = inStockItems.size,
                    expiringSoonCount = expiringSoonCount,
                    freshCount = freshCount,
                    expiredCount = expiredCount,
                    onAddNewItem = { showAddDialog = true }
                )
            }

            // Search Bar & Filter Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_pantry_input"),
                        placeholder = { Text("Search pantry items (e.g. Milk, Spinach)...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = ForestGreenPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Expiration Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ExpirationFilter.values()) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForestGreenPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = ForestGreenPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Results count label
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inventory Items (${filteredItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sorted by Expiry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Empty State
            if (filteredItems.isEmpty()) {
                item {
                    PantryEmptyState(
                        filter = selectedFilter,
                        onResetFilter = {
                            selectedFilter = ExpirationFilter.ALL
                            searchQuery = ""
                        },
                        onAddItem = { showAddDialog = true }
                    )
                }
            } else {
                // Item Cards
                items(filteredItems, key = { it.id }) { item ->
                    PantryItemCard(
                        item = item,
                        now = now,
                        onMarkConsumed = { viewModel.markItemConsumed(item) },
                        onDelete = { viewModel.deleteFoodItem(item) },
                        onDonate = onNavigateToDonor
                    )
                }
            }
        }

        // Floating Action Button to Add Pantry Item
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ForestGreenPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("btn_add_pantry_item")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Pantry Item",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Add Item Dialog
    if (showAddDialog) {
        AddPantryItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, quantity, unit, expiryDays, category, location ->
                val expiryMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(expiryDays.toLong())
                viewModel.addFoodItem(
                    FoodItem(
                        userId = 1L,
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        expiryDate = expiryMillis,
                        category = category,
                        storageLocation = location
                    )
                )
                showAddDialog = false
            }
        )
    }
}

/**
 * Top Summary Dashboard Card with KPIs
 */
@Composable
private fun PantryDashboardHeader(
    totalCount: Int,
    expiringSoonCount: Int,
    freshCount: Int,
    expiredCount: Int,
    onAddNewItem: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ForestGreenPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Kitchen,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Pantry Inventory",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track shelf-life & prevent household waste",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                Button(
                    onClick = onAddNewItem,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Expiration KPI Stat Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expiring Soon (Amber/Orange)
                ExpirationStatBox(
                    modifier = Modifier.weight(1f),
                    count = expiringSoonCount,
                    title = "Expiring",
                    color = Color(0xFFD97706),
                    bgColor = Color(0xFFFEF3C7),
                    icon = Icons.Default.Alarm
                )

                // Fresh (Green)
                ExpirationStatBox(
                    modifier = Modifier.weight(1f),
                    count = freshCount,
                    title = "Fresh",
                    color = Color(0xFF059669),
                    bgColor = Color(0xFFD1FAE5),
                    icon = Icons.Default.CheckCircle
                )

                // Expired (Red)
                ExpirationStatBox(
                    modifier = Modifier.weight(1f),
                    count = expiredCount,
                    title = "Expired",
                    color = Color(0xFFDC2626),
                    bgColor = Color(0xFFFEE2E2),
                    icon = Icons.Default.Warning
                )
            }
        }
    }
}

@Composable
private fun ExpirationStatBox(
    modifier: Modifier = Modifier,
    count: Int,
    title: String,
    color: Color,
    bgColor: Color,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$count",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Pantry Item Card with prominent color-coded expiration label
 */
@Composable
fun PantryItemCard(
    item: FoodItem,
    now: Long,
    onMarkConsumed: () -> Unit,
    onDelete: () -> Unit,
    onDonate: (() -> Unit)? = null
) {
    val daysRemaining = item.daysUntilExpiry(now)
    val statusInfo = getExpirationStatusInfo(daysRemaining)
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = remember(item.expiryDate) { dateFormat.format(Date(item.expiryDate)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("pantry_item_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Row: Item Name, Category, and Color-coded Expiration Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusInfo.bgColor,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = statusInfo.icon,
                                contentDescription = null,
                                tint = statusInfo.textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.quantity} ${item.unit} • ${item.category} • ${item.storageLocation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                // COLOR-CODED EXPIRATION LABEL
                ExpirationBadge(statusInfo = statusInfo)
            }

            // Expiration Date Details Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expires on: $formattedDate",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = statusInfo.timeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusInfo.textColor
                )
            }

            // Actions Row: Consume, Donate, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mark Consumed Button
                Button(
                    onClick = onMarkConsumed,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_consume_${item.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreenPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Used / Consumed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Quick Donate Button
                if (onDonate != null && daysRemaining >= 0) {
                    OutlinedButton(
                        onClick = onDonate,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("btn_donate_${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Donate", fontSize = 12.sp, color = ForestGreenPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_delete_${item.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Color-coded Expiration Badge pill
 */
@Composable
fun ExpirationBadge(statusInfo: ExpirationStatusInfo) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = statusInfo.bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusInfo.textColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = statusInfo.icon,
                contentDescription = null,
                tint = statusInfo.textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = statusInfo.badgeLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusInfo.textColor
            )
        }
    }
}

data class ExpirationStatusInfo(
    val badgeLabel: String,
    val timeText: String,
    val textColor: Color,
    val bgColor: Color,
    val icon: ImageVector
)

fun getExpirationStatusInfo(daysRemaining: Int): ExpirationStatusInfo {
    return when {
        daysRemaining < 0 -> {
            val pastDays = kotlin.math.abs(daysRemaining)
            ExpirationStatusInfo(
                badgeLabel = "EXPIRED",
                timeText = if (pastDays == 1) "Expired yesterday" else "Expired $pastDays days ago",
                textColor = Color(0xFFDC2626), // Deep Crimson Red
                bgColor = Color(0xFFFEE2E2),   // Soft Red Container
                icon = Icons.Default.Warning
            )
        }
        daysRemaining == 0 -> {
            ExpirationStatusInfo(
                badgeLabel = "CRITICAL TODAY",
                timeText = "Expires today!",
                textColor = Color(0xFFEA580C), // Vivid Alert Orange
                bgColor = Color(0xFFFFEDD5),   // Warm Peach Container
                icon = Icons.Default.PriorityHigh
            )
        }
        daysRemaining in 1..3 -> {
            ExpirationStatusInfo(
                badgeLabel = "EXPIRING SOON",
                timeText = if (daysRemaining == 1) "Expires tomorrow" else "Expires in $daysRemaining days",
                textColor = Color(0xFFD97706), // Amber Warning
                bgColor = Color(0xFFFEF3C7),   // Soft Amber Container
                icon = Icons.Default.Alarm
            )
        }
        else -> {
            ExpirationStatusInfo(
                badgeLabel = "FRESH",
                timeText = "Fresh ($daysRemaining days left)",
                textColor = Color(0xFF059669), // Emerald Safe Green
                bgColor = Color(0xFFD1FAE5),   // Pale Mint Container
                icon = Icons.Default.CheckCircle
            )
        }
    }
}

/**
 * Empty State view for filtered or empty pantry
 */
@Composable
private fun PantryEmptyState(
    filter: ExpirationFilter,
    onResetFilter: () -> Unit,
    onAddItem: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ForestGreenPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = when (filter) {
                    ExpirationFilter.ALL -> "Your pantry is empty"
                    ExpirationFilter.EXPIRING_SOON -> "No items expiring soon! 🎉"
                    ExpirationFilter.EXPIRED -> "No expired items! Great job! 🌱"
                    ExpirationFilter.FRESH -> "No fresh items found"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Log groceries or pantry items to receive smart shelf-life alerts and save meals from waste.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filter != ExpirationFilter.ALL) {
                    OutlinedButton(onClick = onResetFilter) {
                        Text("Show All")
                    }
                }
                Button(
                    onClick = onAddItem,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("+ Add First Item")
                }
            }
        }
    }
}

/**
 * Dialog to add an item to the Room Database
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPantryItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, quantity: Double, unit: String, expiryDays: Int, category: String, location: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1.0") }
    var unit by remember { mutableStateOf("pcs") }
    var expiryDays by remember { mutableIntStateOf(5) }
    var category by remember { mutableStateOf(FoodCategory.PRODUCE.label) }
    var location by remember { mutableStateOf(StorageLocation.PANTRY.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Pantry Item",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g. Sliced Bread, Greek Yogurt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Expiration Presets
                Text(
                    text = "Shelf-Life Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        1 to "1 Day ⚠️",
                        3 to "3 Days ⏱️",
                        7 to "1 Week 🌱",
                        14 to "2 Wks 📦"
                    ).forEach { (days, label) ->
                        FilterChip(
                            selected = expiryDays == days,
                            onClick = { expiryDays = days },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreenPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = ForestGreenPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        onConfirm(name.trim(), qty, unit.trim(), expiryDays, category, location)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
            ) {
                Text("Add to Pantry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
