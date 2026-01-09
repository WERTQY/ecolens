package com.example.ecolens;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class DiaryFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private TextView tvSelectedDate, tvDayTotal;

    // Stores the total for each day
    private final Map<CalendarDay, Double> dailyTotals = new HashMap<>();

    // To handle the "toggle" logic
    private CalendarDay lastSelectedDate = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //appbar back button
        View backButton = view.findViewById(R.id.btn_back_manual);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Navigation.findNavController(view).navigateUp();
            });
        }

        AndroidThreeTen.init(requireContext());

        calendarView = view.findViewById(R.id.calendarView);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvDayTotal = view.findViewById(R.id.tvDayTotal);

        // 1. Handle Date Clicks (Toggle Logic)
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                // If clicking the same date again -> Unselect it
                if (lastSelectedDate != null && lastSelectedDate.equals(date)) {
                    widget.clearSelection();
                    lastSelectedDate = null;

                    // Show Month Total instead
                    updateMonthlyTotal(widget.getCurrentDate());
                } else {
                    // New date selected
                    lastSelectedDate = date;
                    widget.setDateSelected(date, true);

                    // Show Daily Total
                    String dateText = date.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                    tvSelectedDate.setText(dateText);

                    Double total = dailyTotals.get(date);
                    tvDayTotal.setText(String.format("Daily Saved: %.2f kg CO₂e", (total != null ? total : 0.0)));
                }
            }
        });

        // 2. Handle Swiping to a New Month
        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                // If no specific day is selected, show the new month's total
                if (widget.getSelectedDates().isEmpty()) {
                    updateMonthlyTotal(date);
                }
            }
        });

        loadHistory();
    }

    // --- NEW HELPER FUNCTION ---
    private void updateMonthlyTotal(CalendarDay month) {
        double monthTotal = 0.0;

        // Loop through all data to find matches for this month/year
        for (Map.Entry<CalendarDay, Double> entry : dailyTotals.entrySet()) {
            CalendarDay dataDate = entry.getKey();
            if (dataDate.getYear() == month.getYear() && dataDate.getMonth() == month.getMonth()) {
                monthTotal += entry.getValue();
            }
        }

        // Format: "January 2026"
        String monthText = month.getDate().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        tvSelectedDate.setText(monthText);
        tvDayTotal.setText(String.format("Total Saved: %.2f kg CO₂e", monthTotal));
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
                    java.util.Calendar cal = java.util.Calendar.getInstance();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        if (doc.contains("timestamp") && doc.contains("amount")) {
                            Timestamp ts = doc.getTimestamp("timestamp");
                            Double amount = doc.getDouble("amount");

                            if (ts != null && amount != null) {
                                Date date = ts.toDate();
                                cal.setTime(date);

                                int year = cal.get(java.util.Calendar.YEAR);
                                int month = cal.get(java.util.Calendar.MONTH) + 1;
                                int day = cal.get(java.util.Calendar.DAY_OF_MONTH);

                                CalendarDay calendarDay = CalendarDay.from(year, month, day);
                                activeDays.add(calendarDay);

                                double currentTotal = dailyTotals.getOrDefault(calendarDay, 0.0);
                                dailyTotals.put(calendarDay, currentTotal + amount);
                            }
                        }
                    }

                    if (getContext() != null) {
                        calendarView.addDecorator(new EventDecorator(getContext(), activeDays));
                    }

                    // Show current month total immediately after loading
                    updateMonthlyTotal(calendarView.getCurrentDate());

                })
                .addOnFailureListener(e -> Log.e("DiaryFragment", "Error loading history", e));
    }
}