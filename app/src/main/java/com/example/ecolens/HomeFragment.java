package com.example.ecolens;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.DecelerateInterpolator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private TextView tvTierName, tvStreak, carbonFootPrintTextView;
    private TextView greetingDisplay, usernameDisplay;
    private ImageView imgTierBadge;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        tvTierName = view.findViewById(R.id.tvTierName);
        tvStreak = view.findViewById(R.id.tvStreak);
        imgTierBadge = view.findViewById(R.id.imgTrophy);
        carbonFootPrintTextView = view.findViewById(R.id.carbon_footprint_value);
        greetingDisplay = view.findViewById(R.id.greetingsDisplay);
        usernameDisplay = view.findViewById(R.id.usernameDisplay);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        //greeting logic
        if(greetingDisplay != null){
            greetingDisplay.setText(getGreeting());
        }

        //profile button
        ImageButton btnProfile = view.findViewById(R.id.profile_button);
        if(btnProfile != null){
            btnProfile.setOnClickListener(v ->{
                Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_userFragment);
            });
        }

        // Diary Button
        Button btnDiary = view.findViewById(R.id.btnDiary);
        if (btnDiary != null) {
            btnDiary.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_diaryFragment)
            );
        }

        // Load Data (Consolidated)
        if (auth.getCurrentUser() != null) {
            loadUserData();
            fetchUserName();
        }
    }

    // username
    private void fetchUserName() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && usernameDisplay != null) {
                            usernameDisplay.setText(name);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("HomeFragment", "Error fetching name", e));
    }

    // greeting message
    private String getGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            return "Good Morning,";
        } else if (hour >= 12 && hour < 17) {
            return "Good Afternoon,";
        } else if (hour >= 17 && hour < 21) {
            return "Good Evening,";
        } else {
            return "Good Night,";
        }
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("impact_tracker").document(uid);

        userRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {

                // 1. GET FOOTPRINT VALUE & ANIMATE
                double totalSaved = 0.0;
                if (document.contains("gross_footprint") && document.get("gross_footprint") != null) {
                    totalSaved = document.getDouble("gross_footprint");
                }
                animateCarbonFootprintValue(totalSaved);

                // 2. UPDATE TIER UI
                updateTierUI(totalSaved);

                // 3. HANDLE STREAK
                handleStreakLogic(document, userRef);

            } else {
                // New user / Empty data defaults
                animateCarbonFootprintValue(0.0);
                updateTierUI(0.0);
                if(tvStreak != null) tvStreak.setText("🔥 0 Day Streak");
            }
        }).addOnFailureListener(e -> Log.e("HomeFragment", "Error loading user data", e));
    }

    private void animateCarbonFootprintValue(double finalValue) {
        if (carbonFootPrintTextView == null) return;

        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) finalValue);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(1500);
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            carbonFootPrintTextView.setText(String.format("%.2f", animatedValue));
        });
        animator.start();
    }

    private void updateTierUI(double totalSaved) {
        if (tvTierName == null || imgTierBadge == null) return;

        if (totalSaved < 10.0) {
            tvTierName.setText("Bronze Recycler");
            imgTierBadge.setColorFilter(0xFFCD7F32); // Bronze
        } else if (totalSaved < 50.0) {
            tvTierName.setText("Silver Recycler");
            imgTierBadge.setColorFilter(0xFFC0C0C0); // Silver
        } else {
            tvTierName.setText("Gold Recycler");
            imgTierBadge.setColorFilter(0xFFFFD700); // Gold
        }
    }

    private void handleStreakLogic(DocumentSnapshot document, DocumentReference userRef) {
        if (tvStreak == null) return;

        long currentStreak = 0;
        String lastDate = "";

        if (document.contains("streak")) {
            currentStreak = document.getLong("streak");
        }
        if (document.contains("last_login_date")) {
            lastDate = document.getString("last_login_date");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        //streak logic
        if (lastDate.equals(todayDate)) {
        } else {
            currentStreak++;

            Map<String, Object> updates = new HashMap<>();
            updates.put("streak", currentStreak);
            updates.put("last_login_date", todayDate);
            userRef.set(updates, SetOptions.merge());
        }

        tvStreak.setText("🔥 " + currentStreak + " Day Streak");
    }
}