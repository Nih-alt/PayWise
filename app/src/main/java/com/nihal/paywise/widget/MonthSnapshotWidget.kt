package com.nihal.paywise.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class MonthSnapshotWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            SnapshotContent()
        }
    }

    @Composable
    private fun SnapshotContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "This Month",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant, 
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            )
            
            Text(
                text = "₹12,450", 
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // Progress bar simulation
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(GlanceTheme.colors.secondaryContainer)
            ) {
                // In Glance we can use Box with fixed width or weight for progress
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(GlanceTheme.colors.primary)
                    ) {}
                    Spacer(modifier = GlanceModifier.defaultWeight()) // Just for visual ratio
                }
            }
            
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            Text(
                text = "70% of budget used",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant, 
                    fontSize = 10.sp
                )
            )
        }
    }
}