package com.nihal.paywise.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickAddWidget()
}

class MonthSnapshotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MonthSnapshotWidget()
}
