package com.nihal.paywise.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.EmptyState
import com.nihal.paywise.ui.components.GlassCard
import com.nihal.paywise.ui.util.CategoryVisuals
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onRangeSelected = { start, end ->
                viewModel.setCustomRange(start, end)
                showDatePicker = false
            }
        )
    }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ReportsHeader(
                uiState = uiState,
                onTabSelected = { viewModel.setTab(it) },
                onPresetSelected = { viewModel.setPreset(it) },
                onPrevMonth = { viewModel.prevMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onCustomClick = { showDatePicker = true }
            )

            AnimatedContent(
                targetState = uiState.selectedTab,
                label = "ReportTabAnimation"
            ) { tab ->
                if (uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Default.PieChart,
                            title = "No expenses found",
                            subtitle = "Try selecting a different period or add new expenses."
                        )
                    }
                } else {
                    when (tab) {
                        ReportTab.BREAKDOWN -> BreakdownTab(uiState)
                        ReportTab.TREND -> TrendTab(uiState)
                        ReportTab.FIXED_VS_DISCRETIONARY -> FixedVsDiscTab(uiState)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsHeader(
    uiState: ReportsUiState,
    onTabSelected: (ReportTab) -> Unit,
    onPresetSelected: (ReportRangePreset) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCustomClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Range Presets
        ScrollableTabRow(
            selectedTabIndex = uiState.rangePreset.ordinal,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = {}
        ) {
            ReportRangePreset.entries.forEach { preset ->
                val selected = uiState.rangePreset == preset
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { 
                            if (preset == ReportRangePreset.CUSTOM) onCustomClick()
                            else onPresetSelected(preset)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.name.replace("_", " ").lowercase().capitalize(),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Month Selector for THIS_MONTH preset
        if (uiState.rangePreset == ReportRangePreset.THIS_MONTH) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                }
                Text(
                    text = DateTimeFormatterUtil.formatYearMonth(uiState.selectedMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 120.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
        } else {
            Text(
                text = uiState.dateRangeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Selector
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            ReportTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = when(tab) {
                                ReportTab.BREAKDOWN -> "Breakdown"
                                ReportTab.TREND -> "Trend"
                                ReportTab.FIXED_VS_DISCRETIONARY -> "Groups"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun BreakdownTab(uiState: ReportsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Spent", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(uiState.totalSpentText, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    DonutChart(
                        items = uiState.categoryBreakdown,
                        modifier = Modifier.size(200.dp)
                    )
                }
            }
        }

        items(uiState.categoryBreakdown) { item ->
            CategoryRow(item)
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun CategoryRow(item: CategoryBreakdownRow) {
    val visual = CategoryVisuals.getVisual(item.categoryName)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(visual.containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(MoneyFormatter.formatPaise(item.totalAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { item.percentage },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = visual.color,
                    trackColor = visual.containerColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("${(item.percentage * 100).toInt()}% of total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TrendTab(uiState: ReportsUiState) {
    val avg = if (uiState.trend.isEmpty()) 0L else uiState.trend.sumOf { it.totalAmount } / uiState.trend.size
    val max = uiState.trend.maxOfOrNull { it.totalAmount } ?: 0L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Text("Last 12 Months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                TrendChart(
                    items = uiState.trend,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("Average", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MoneyFormatter.formatPaise(avg), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("Highest", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MoneyFormatter.formatPaise(max), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun FixedVsDiscTab(uiState: ReportsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val fixed = uiState.fixedVsDiscretionary.find { it.spendingGroup == SpendingGroup.FIXED }
                val disc = uiState.fixedVsDiscretionary.find { it.spendingGroup == SpendingGroup.DISCRETIONARY }

                SpendingGroupCard(
                    title = "Fixed",
                    amount = fixed?.totalAmount ?: 0L,
                    percent = fixed?.percentage ?: 0f,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    helper = "Rent, EMI, Utilities, etc."
                )
                SpendingGroupCard(
                    title = "Discretionary",
                    amount = disc?.totalAmount ?: 0L,
                    percent = disc?.percentage ?: 0f,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    helper = "Dining, Fun, Shopping, etc."
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SpendingGroupCard(
    title: String,
    amount: Long,
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
    helper: String
) {
    GlassCard(modifier = modifier.heightIn(min = 180.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(MoneyFormatter.formatPaise(amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
            Text("${(percent * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(helper, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DonutChart(items: List<CategoryBreakdownRow>, modifier: Modifier = Modifier) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animationProgress.animateTo(1f, animationSpec = tween(1000))
    }

    Canvas(modifier = modifier) {
        var startAngle = -90f
        items.forEach { item ->
            val sweepAngle = item.percentage * 360f * animationProgress.value
            if (sweepAngle > 0) {
                val visual = CategoryVisuals.getVisual(item.categoryName)
                drawArc(
                    color = visual.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 40f, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun TrendChart(items: List<MonthlyTrendRow>, modifier: Modifier = Modifier) {
    val maxVal = (items.maxOfOrNull { it.totalAmount } ?: 1L).toFloat()
    val primaryColor = MaterialTheme.colorScheme.primary
    
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animationProgress.animateTo(1f, animationSpec = tween(1000))
    }

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val barCount = items.size
            if (barCount == 0) return@Canvas
            
            val spacing = size.width / (barCount * 3)
            val barWidth = (size.width - (spacing * (barCount + 1))) / barCount
            
            items.forEachIndexed { index, item ->
                val barHeight = (item.totalAmount.toFloat() / maxVal) * size.height * animationProgress.value
                val x = spacing + index * (barWidth + spacing)
                
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            items.forEach { item ->
                Text(
                    text = item.yearMonth.month.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(30.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangePickerDialog(
    onDismiss: () -> Unit,
    onRangeSelected: (Instant, Instant) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis?.let { Instant.ofEpochMilli(it) }
                    val end = dateRangePickerState.selectedEndDateMillis?.let { Instant.ofEpochMilli(it) }
                    if (start != null && end != null) {
                        onRangeSelected(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.weight(1f),
            title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) }
        )
    }
}

private fun String.capitalize() = this.replaceFirstChar { it.uppercase() }