package com.nihal.paywise.util

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.nihal.paywise.widget.MonthSnapshotWidget
import com.nihal.paywise.widget.QuickAddWidget

object WidgetUpdateController {
    suspend fun updateAllWidgets(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        
        manager.getGlanceIds(QuickAddWidget::class.java).forEach {
            QuickAddWidget().update(context, it)
        }
        
        manager.getGlanceIds(MonthSnapshotWidget::class.java).forEach {
            MonthSnapshotWidget().update(context, it)
        }
    }
}
