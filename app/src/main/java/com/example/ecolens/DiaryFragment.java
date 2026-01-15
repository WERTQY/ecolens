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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Import the inner class
import com.example.ecolens.EventDecorator.HistoryItem;

public class DiaryFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private TextView tvSelectedDate, tvDayTotal;

    // UI for List
    private RecyclerView recyclerView;
    private DiaryAdapter adapter;

    // Data Storage
    private final Map<CalendarDay, Double> dailyTotals = new HashMap<>();
    private final List<HistoryItem> allHistoryItems = new ArrayList<>();
    private final List<HistoryItem> currentDisplayList = new ArrayList<>();

    private CalendarDay lastSelectedDate = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View backButton = view.findViewById(R.id.btn_back_manual);
        if (backButton != null) {
            backButton.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        }

        AndroidThreeTen.init(requireContext());

        calendarView = view.findViewById(R.id.calendarView);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvDayTotal = view.findViewById(R.id.tvDayTotal);

        recyclerView = view.findViewById(R.id.rv_history_log);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new DiaryAdapter(currentDisplayList);
            recyclerView.setAdapter(adapter);
        }

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (lastSelectedDate != null && lastSelectedDate.equals(date)) {
                widget.clearSelection();
                lastSelectedDate = null;
                updateMonthlyTotal(widget.getCurrentDate());
                filterListByDate(null); // Show all or clear
            } else {
                lastSelectedDate = date;
                widget.setDateSelected(date, true);

                String dateText = date.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                tvSelectedDate.setText(dateText);

                String dailyAchievement = findTierUnlockText(date, false);
                Double total = dailyTotals.get(date);
                double safeTotal = (total != null) ? total : 0.0;

                if (dailyAchievement != null) {
                    tvDayTotal.setText(String.format("• Daily: %.2f kg CO₂e\n%s", safeTotal, dailyAchievement));
                } else {
                    tvDayTotal.setText(String.format("• Daily Saved: %.2f kg CO₂e", safeTotal));
                }

                filterListByDate(date);
            }
        });

        calendarView.setOnMonthChangedListener((widget, date) -> {
            if (widget.getSelectedDates().isEmpty()) {
                updateMonthlyTotal(date);
                filterListByDate(null);
            }
        });

        loadHistory();
    }

    private void filterListByDate(@Nullable CalendarDay targetDate) {
        currentDisplayList.clear();
        if (targetDate != null) {
            for (HistoryItem item : allHistoryItems) {
                if (item.getTimestamp() != null) {
                    Date d = item.getTimestamp().toDate();
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(d);

                    int year = cal.get(java.util.Calendar.YEAR);
                    int month = cal.get(java.util.Calendar.MONTH) + 1;
                    int day = cal.get(java.util.Calendar.DAY_OF_MONTH);

                    if (year == targetDate.getYear() && month == targetDate.getMonth() && day == targetDate.getDay()) {
                        currentDisplayList.add(item);
                    }
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private String findTierUnlockText(CalendarDay target, boolean matchMonthOnly) {
        StringBuilder achievements = new StringBuilder();
        for (HistoryItem item : allHistoryItems) {
            if (item.getWasteType() != null && item.getWasteType().contains("Unlocked") && item.getTimestamp() != null) {
                Date d = item.getTimestamp().toDate();
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(d);
                int year = cal.get(java.util.Calendar.YEAR);
                int month = cal.get(java.util.Calendar.MONTH) + 1;
                int day = cal.get(java.util.Calendar.DAY_OF_MONTH);

                boolean isMatch = false;

                if (matchMonthOnly) {
                    if (year == target.getYear() && month == target.getMonth()) {
                        isMatch = true;
                    }
                } else {
                    if (year == target.getYear() && month == target.getMonth() && day == target.getDay()) {
                        isMatch = true;
                    }
                }

                if (isMatch) {
                    if (achievements.length() > 0) {
                        achievements.append("\n• ");
                    } else {
                        achievements.append("• ");
                    }
                    achievements.append(item.getWasteType());
                }
            }
        }
        return achievements.length() > 0 ? achievements.toString() : null;
    }

    private void updateMonthlyTotal(CalendarDay month) {
        double monthTotal = 0.0;
        for (Map.Entry<CalendarDay, Double> entry : dailyTotals.entrySet()) {
            CalendarDay dataDate = entry.getKey();
            if (dataDate.getYear() == month.getYear() && dataDate.getMonth() == month.getMonth()) {
                monthTotal += entry.getValue();
            }
        }

        String monthText = month.getDate().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        tvSelectedDate.setText(monthText);

        String achievementText = findTierUnlockText(month, true); // true = match month

        if (achievementText != null) {
            tvDayTotal.setText(String.format("• Total Saved: %.2f kg CO₂e\n%s", monthTotal, achievementText));
        } else {
            tvDayTotal.setText(String.format("• Total Saved: %.2f kg CO₂e", monthTotal));
        }
    }

    private void loadHistory() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("impact_tracker")
                .document(uid)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    dailyTotals.clear();
                    allHistoryItems.clear();
                    java.util.Calendar cal = java.util.Calendar.getInstance();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        HistoryItem item = doc.toObject(HistoryItem.class);
                        allHistoryItems.add(item);

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
                                double currentTotal = dailyTotals.getOrDefault(calendarDay, 0.0);
                                dailyTotals.put(calendarDay, currentTotal + amount);
                            }
                        }
                    }

                    if (getContext() != null) {
                        calendarView.removeDecorators();
                        calendarView.addDecorator(new EventDecorator(getContext(), allHistoryItems, EventDecorator.TYPE_LEAF));
                        calendarView.addDecorator(new EventDecorator(getContext(), allHistoryItems, EventDecorator.TYPE_TROPHY_BRONZE));
                        calendarView.addDecorator(new EventDecorator(getContext(), allHistoryItems, EventDecorator.TYPE_TROPHY_SILVER));
                        calendarView.addDecorator(new EventDecorator(getContext(), allHistoryItems, EventDecorator.TYPE_TROPHY_GOLD));
                        calendarView.addDecorator(new EventDecorator(getContext(), allHistoryItems, EventDecorator.TYPE_TROPHY_PLATINUM));
                        calendarView.addDecorator(new EventDecorator(getContext(), allHistoryItems, EventDecorator.TYPE_TROPHY_DIAMOND));
                    }

                    updateMonthlyTotal(calendarView.getCurrentDate());

                })
                .addOnFailureListener(e -> Log.e("DiaryFragment", "Error loading history", e));
    }
}