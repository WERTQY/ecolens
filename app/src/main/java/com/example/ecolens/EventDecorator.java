package com.example.ecolens;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.util.Collection;
import java.util.HashSet;

public class EventDecorator implements DayViewDecorator {
    private final HashSet<CalendarDay> dates;
    private final Drawable drawable;

    public EventDecorator(Context context, Collection<CalendarDay> dates) {
        this.dates = new HashSet<>(dates);
        // Load the leaf icon
        Drawable original = ContextCompat.getDrawable(context, R.drawable.ic_leaf_streak);

        if (original != null) {
            // Wrap it so we can tint it safely
            this.drawable = DrawableCompat.wrap(original).mutate();
            // Set Color to Dark Green (#1B5E20 is a nice deep eco green)
            DrawableCompat.setTint(this.drawable, Color.parseColor("#1B5E20"));
        } else {
            this.drawable = null;
        }
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        if (drawable != null) {
            // This places the leaf as a background for the date
            view.setBackgroundDrawable(drawable);
        }
        view.addSpan(new ForegroundColorSpan(Color.WHITE));
    }
}