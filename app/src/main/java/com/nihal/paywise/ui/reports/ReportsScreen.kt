package com.nihal.paywise.ui.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PieChart
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
import com.nihal.paywise.domain.model.CategoryReportItem
import com.nihal.paywise.domain.model.TrendItem
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.EmptyState
import com.nihal.paywise.ui.components.SoftCard
import com.nihal.paywise.ui.util.CategoryVisuals
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import java.time.YearMonth

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ReportsHeader(
                selectedMonth = uiState.selectedMonth,
                selectedMode = uiState.selectedMode,
                onPrev = { viewModel.prevMonth() },
                onNext = { viewModel.nextMonth() },
                onModeChange = { viewModel.setMode(it) }
            )

            if (uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.PieChart,
                        title = "No data for this period",
                        subtitle = "Add some expenses to see your spending reports."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Total & Breakdown Card
                    item {
                        CategoryBreakdownCard(
                            totalSpent = uiState.totalSpentText,
                            items = uiState.categoryBreakdown
                        )
                    }

                    // 2. Fixed vs Discretionary
                    uiState.fixedVsDiscretionary?.let { report ->
                        item {
                            FixedVsDiscretionaryCard(report)
                        }
                    }

                    // 3. Trend Card
                    item {
                        TrendCard(uiState.trend)
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReportsHeader(
    selectedMonth: YearMonth,
    selectedMode: ReportMode,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onModeChange: (ReportMode) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Mode Selector
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = selectedMode == ReportMode.MONTH,
                    onClick = { onModeChange(ReportMode.MONTH) },
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                    label = { Text("Month", fontSize = 12.sp) }
                )
                SegmentedButton(
                    selected = selectedMode == ReportMode.YEAR,
                    onClick = { onModeChange(ReportMode.YEAR) },
                    shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                    label = { Text("Year", fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Month Navigation
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = CircleShape,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                }
                Text(
                    text = if (selectedMode == ReportMode.MONTH) 
                        DateTimeFormatterUtil.formatYearMonth(selectedMonth)
                    else 
                        "Last 12 Months",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 100.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(totalSpent: String, items: List<CategoryReportItem>) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Spending Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(totalSpent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Donut Chart
                DonutChart(
                    items = items.take(6),
                    modifier = Modifier.size(140.dp)
                )
                
                Spacer(modifier = Modifier.width(24.dp))

                // Legend (Top 4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.take(4).forEach { item ->
                        val visual = CategoryVisuals.getVisual(item.categoryName)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(visual.color, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${item.categoryName} (${(item.percentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Full List
            items.forEach { item ->
                CategoryBreakdownRow(item)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun CategoryBreakdownRow(item: CategoryReportItem) {
    val visual = CategoryVisuals.getVisual(item.categoryName)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).background(visual.containerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(visual.icon, null, modifier = Modifier.size(16.dp), tint = visual.color)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(item.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Text(
                MoneyFormatter.formatPaise(item.amountPaise),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { item.percentage },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = visual.color,
            trackColor = visual.containerColor
        )
    }
}

@Composable
fun FixedVsDiscretionaryCard(report: com.nihal.paywise.domain.model.FixedVsDiscretionaryReport) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Text("Fixed vs Discretionary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth().height(24.dp).clip(CircleShape)) {
            Box(modifier = Modifier.weight(report.fixedPercent.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            Box(modifier = Modifier.weight(report.discretionaryPercent.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LegendItem("Fixed", MoneyFormatter.formatPaise(report.fixedTotal), MaterialTheme.colorScheme.primary)
            LegendItem("Discretionary", MoneyFormatter.formatPaise(report.discretionaryTotal), MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun TrendCard(items: List<TrendItem>) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Text("Monthly Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        BarChart(items = items, modifier = Modifier.fillMaxWidth().height(160.dp))
    }
}

@Composable
fun LegendItem(label: String, amount: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DonutChart(items: List<CategoryReportItem>, modifier: Modifier = Modifier) {
    val total = items.sumOf { it.amountPaise }.toFloat()
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        items.forEach { item ->
            val sweepAngle = (item.amountPaise.toFloat() / total) * 360f
            val visual = CategoryVisuals.getVisual(item.categoryName)
            
            drawArc(
                color = visual.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun BarChart(items: List<TrendItem>, modifier: Modifier = Modifier) {
    val maxVal = (items.maxOfOrNull { it.amountPaise } ?: 1L).toFloat()
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val spacing = size.width / (items.size * 2)
        val barWidth = size.width / (items.size * 1.5f)
        
        items.forEachIndexed { index, item ->
            val x = (index * (barWidth + spacing)) + spacing
            val barHeight = (item.amountPaise.toFloat() / maxVal) * size.height
            
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.8f),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
    
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        items.forEach { item ->
            Text(item.label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}
