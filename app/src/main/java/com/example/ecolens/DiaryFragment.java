package com.example.ecolens;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// --- CRITICAL IMPORTS FOR VERSION 2.0.1 ---
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import org.threeten.bp.format.DateTimeFormatter;
// ------------------------------------------

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class DiaryFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private TextView tvSelectedDate, tvDayTotal;

    // Stores the total for each day
    private Map<CalendarDay, Double> dailyTotals = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize the Date Library (REQUIRED for v2.0.1)
        AndroidThreeTen.init(requireContext());

        // 2. Bind Views
        calendarView = view.findViewById(R.id.calendarView);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvDayTotal = view.findViewById(R.id.tvDayTotal);

        // 3. Set Default Date Text (Using new Formatter)
        tvSelectedDate.setText("Select a date");

        // 4. Handle Date Clicks
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                // Formatting the date using ThreeTen formatter
                String dateText = date.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                tvSelectedDate.setText(dateText);

                // Show total
                Double total = dailyTotals.get(date);
                if (total != null) {
                    tvDayTotal.setText(String.format("Total Saved: %.2f kg", total));
                } else {
                    tvDayTotal.setText("Total Saved: 0.00 kg");
                }
            }
        });

        // 5. Load Data
        loadHistory();
    }

    private void loadHistory() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("impact_tracker")
                .document(uid)
                .collection("history")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    dailyTotals.clear();
                    HashSet<CalendarDay> activeDays = new HashSet<>();

                    // Helper calendar to extract day/month/year from Firebase Date
                    java.util.Calendar cal = java.util.Calendar.getInstance();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        if (doc.contains("timestamp") && doc.contains("amount")) {
                            Timestamp ts = doc.getTimestamp("timestamp");
                            Double amount = doc.getDouble("amount");

                            if (ts != null && amount != null) {
                                // Convert Timestamp to Java Date
                                Date date = ts.toDate();
                                cal.setTime(date);

                                // Extract parts
                                int year = cal.get(java.util.Calendar.YEAR);
                                int month = cal.get(java.util.Calendar.MONTH) + 1; // Fix: Add 1 because Library expects 1-12
                                int day = cal.get(java.util.Calendar.DAY_OF_MONTH);

                                // Create CalendarDay
                                CalendarDay calendarDay = CalendarDay.from(year, month, day);
                                activeDays.add(calendarDay);

                                // Add to total
                                double currentTotal = dailyTotals.containsKey(calendarDay) ? dailyTotals.get(calendarDay) : 0.0;
                                dailyTotals.put(calendarDay, currentTotal + amount);
                            }
                        }
                    }

                    // Add the Leaf Icons
                    if (getContext() != null) {
                        calendarView.addDecorator(new EventDecorator(getContext(), activeDays));
                    }
                })
                .addOnFailureListener(e -> Log.e("DiaryFragment", "Error loading history", e));
    }
}