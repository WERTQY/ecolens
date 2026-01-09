package com.example.ecolens;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
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
        this.drawable = ContextCompat.getDrawable(context, R.drawable.ic_leaf_streak);
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
    }
}