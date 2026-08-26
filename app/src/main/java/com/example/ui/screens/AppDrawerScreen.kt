package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

private enum class AppDrawerFilter(val arabicTitle: String) {
    ALL("جميع التطبيقات"),
    FAVORITES("المفضلة ⭐"),
    HIDDEN("التطبيقات المخفية 👁️")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val installedApps by viewModel.installedApps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppDrawerFilter.ALL) }

    val filteredApps = remember(installedApps, searchQuery, selectedFilter) {
        installedApps.filter { app ->
            val matchesFilter = when (selectedFilter) {
                AppDrawerFilter.ALL -> !app.isHidden
                AppDrawerFilter.FAVORITES -> app.isFavorite && !app.isHidden
                AppDrawerFilter.HIDDEN -> app.isHidden
            }
            val matchesSearch = searchQuery.isBlank() ||
                    app.label.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Top Search & Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input Field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن تطبيق...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = CyanNeon) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface,
                    focusedIndicatorColor = CyanNeon,
                    unfocusedIndicatorColor = CarbonCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("input_search_apps")
            )

            // Category Filter Badges
            AppDrawerFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    color = if (isSelected) CyanNeon else CarbonSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanNeon else CarbonCardBorder),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedFilter = filter }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.arabicTitle,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) CarbonDark else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Apps Grid
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لم يتم العثور على نتائج لـ \"$searchQuery\"" else "لا توجد تطبيقات في هذه الفئة",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppDrawerCard(
                        app = app,
                        onLaunch = { viewModel.launchApp(app.packageName) },
                        onToggleFavorite = { viewModel.toggleAppFavorite(app.packageName) },
                        onToggleHidden = { viewModel.toggleAppHidden(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDrawerCard(
    app: AppItem,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .border(1.dp, if (app.isFavorite) AmberRacing.copy(alpha = 0.5f) else CarbonCardBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onLaunch() }
            .testTag("app_card_${app.packageName}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            // Favorite & Hide Quick Action Buttons
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(24.dp).testTag("btn_fav_${app.packageName}")
                ) {
                    Icon(
                        imageVector = if (app.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "تفضيل",
                        tint = if (app.isFavorite) AmberRacing else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onToggleHidden,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "إخفاء",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Main Content: Icon and Label
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (app.icon != null) {
                    val bitmap = try {
                        app.icon.toBitmap(96, 96).asImageBitmap()
                    } catch (e: Exception) { null }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.label,
                            modifier = Modifier.size(46.dp)
                        )
                    } else {
                        Icon(Icons.Default.Android, contentDescription = app.label, tint = CyanNeon, modifier = Modifier.size(40.dp))
                    }
                } else {
                    Icon(Icons.Default.Android, contentDescription = app.label, tint = CyanNeon, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1
                )
            }
        }
    }
}
