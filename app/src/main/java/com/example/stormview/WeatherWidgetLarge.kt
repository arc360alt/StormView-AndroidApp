package com.example.stormview

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class WeatherWidgetLarge : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            WeatherWidget.updateWidget(context, appWidgetManager, id, R.layout.widget_large)
        }
    }
}