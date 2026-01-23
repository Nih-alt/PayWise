package com.nihal.paywise.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nihal.paywise.MainActivity

class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuickAddContent()
        }
    }

    @Composable
    private fun QuickAddContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Add",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                QuickAddButton(label = "Chai")
                Spacer(modifier = GlanceModifier.width(8.dp))
                QuickAddButton(label = "Fuel")
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                QuickAddButton(label = "Lunch")
                Spacer(modifier = GlanceModifier.width(8.dp))
                QuickAddButton(label = "Custom")
            }
        }
    }

    @Composable
    private fun QuickAddButton(label: String) {
        Box(
            modifier = GlanceModifier
                .padding(4.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer, 
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            )
        }
    }
}