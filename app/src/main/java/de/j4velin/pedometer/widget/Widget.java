/*
 * Copyright 2014 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.j4velin.pedometer.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Color;
import android.widget.RemoteViews;

import de.j4velin.pedometer.R;

public class Widget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WidgetUpdateService.enqueueUpdate(context);
    }

    static RemoteViews updateWidget(final Context context, final int steps) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextColor(R.id.widget_steps, Color.WHITE);
        views.setTextViewText(R.id.widget_steps, String.valueOf(steps));
        views.setInt(R.id.widget, "setBackgroundColor", Color.TRANSPARENT);
        return views;
    }
}
