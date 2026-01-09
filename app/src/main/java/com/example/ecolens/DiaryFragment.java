package com.example.ecolens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiaryFragment extends Fragment {

    private RecyclerView recyclerView;
    private DiaryAdapter adapter;
    private List<DiaryEntry> diaryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.calendarView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup Adapter
        adapter = new DiaryAdapter(diaryList);
        recyclerView.setAdapter(adapter);

        // Load Data
        loadHistory();
    }

    private void loadHistory() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("impact_tracker")
                .document(uid)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Show newest first
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    diaryList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Manual mapping to ensure safety
                        if (doc.contains("amount") && doc.contains("timestamp")) {
                            double amount = doc.getDouble("amount");
                            Timestamp ts = doc.getTimestamp("timestamp");
                            diaryList.add(new DiaryEntry(amount, ts));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // --- Simple Data Class ---
    public static class DiaryEntry {
        double amount;
        Timestamp timestamp;

        public DiaryEntry(double amount, Timestamp timestamp) {
            this.amount = amount;
            this.timestamp = timestamp;
        }
    }

    // --- Simple Adapter ---
    public static class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.ViewHolder> {
        private List<DiaryEntry> list;

        public DiaryAdapter(List<DiaryEntry> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Using simple android layout to save time, or you can create a custom row xml
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DiaryEntry entry = list.get(position);

            // Format Date
            String dateStr = "Unknown Date";
            if (entry.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                dateStr = sdf.format(entry.timestamp.toDate());
            }

            holder.text1.setText(String.format("Saved: %.2f kg CO2e", entry.amount));
            holder.text2.setText(dateStr);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}