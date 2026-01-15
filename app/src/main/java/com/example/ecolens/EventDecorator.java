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

import com.google.firebase.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class EventDecorator implements DayViewDecorator {

    // Define Types
    public static final int TYPE_LEAF = 0;
    public static final int TYPE_TROPHY_BRONZE = 1;
    public static final int TYPE_TROPHY_SILVER = 2;
    public static final int TYPE_TROPHY_GOLD = 3;
    public static final int TYPE_TROPHY_PLATINUM = 4;
    public static final int TYPE_TROPHY_DIAMOND = 5;

    private final HashSet<CalendarDay> dates = new HashSet<>();
    private final Drawable drawable;

    public EventDecorator(Context context, List<HistoryItem> historyItems, int type) {

        for (HistoryItem item : historyItems) {
            if (item.getTimestamp() == null) continue;

            String title = (item.getWasteType() != null) ? item.getWasteType() : "";
            boolean isAchievement = title.contains("Unlocked");
            boolean shouldAdd = false;

            // --- FILTER LOGIC ---
            if (type == TYPE_LEAF && !isAchievement) {
                shouldAdd = true;
            }
            else if (isAchievement) {
                // Check specific tiers for Trophies
                if (type == TYPE_TROPHY_BRONZE && title.contains("Bronze")) shouldAdd = true;
                else if (type == TYPE_TROPHY_SILVER && title.contains("Silver")) shouldAdd = true;
                else if (type == TYPE_TROPHY_GOLD && title.contains("Gold")) shouldAdd = true;
                else if (type == TYPE_TROPHY_PLATINUM && title.contains("Platinum")) shouldAdd = true;
                else if (type == TYPE_TROPHY_DIAMOND && title.contains("Diamond")) shouldAdd = true;
            }

            if (shouldAdd) {
                Date d = item.getTimestamp().toDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);

                CalendarDay day = CalendarDay.from(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                );
                dates.add(day);
            }
        }

        // --- COLOR LOGIC ---
        int resId;
        int color;

        if (type == TYPE_LEAF) {
            resId = R.drawable.ic_leaf_streak;
            color = Color.parseColor("#1B5E20"); // Dark Green
        } else {
            // It is a Trophy
            resId = R.drawable.ic_trophy;

            if (type == TYPE_TROPHY_BRONZE) color = 0xFFCD7F32;      // Bronze
            else if (type == TYPE_TROPHY_SILVER) color = 0xFF9E9E9E; // Silver
            else if (type == TYPE_TROPHY_GOLD) color = 0xFFFFD700;   // Gold
            else if (type == TYPE_TROPHY_PLATINUM) color = 0xFF455A64; // Platinum
            else if (type == TYPE_TROPHY_DIAMOND) color = 0xFF00B0FF;  // Diamond
            else color = 0xFFFFD700; // Default
        }

        Drawable original = ContextCompat.getDrawable(context, resId);
        if (original != null) {
            this.drawable = DrawableCompat.wrap(original).mutate();
            DrawableCompat.setTint(this.drawable, color);
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
            view.setBackgroundDrawable(drawable);
        }
        view.addSpan(new ForegroundColorSpan(Color.WHITE));
    }

    public static class HistoryItem {
        private String wasteType;
        private double amount;
        private Timestamp timestamp;

        public HistoryItem() {}

        public HistoryItem(String wasteType, double amount, Timestamp timestamp) {
            this.wasteType = wasteType;
            this.amount = amount;
            this.timestamp = timestamp;
        }

        public String getWasteType() { return wasteType; }
        public double getAmount() { return amount; }
        public Timestamp getTimestamp() { return timestamp; }
    }
}