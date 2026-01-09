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
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.DecelerateInterpolator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private TextView tvTierName, tvStreak, carbonFootPrintTextView; // Merged: Added carbonFootPrintTextView
    private ImageView imgTierBadge;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //initialize firebase
        tvTierName = view.findViewById(R.id.tvTierName);
        tvStreak = view.findViewById(R.id.tvStreak);
        imgTierBadge = view.findViewById(R.id.imgTierBadge);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        carbonFootPrintTextView = view.findViewById(R.id.carbon_footprint_value);
        fetchAndAnimateCarbonFootprint();


        //logout button will be reuse in the profile part
//        Button btnLogout = view.findViewById(R.id.btnLogout);
//        btnLogout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                FirebaseAuth.getInstance().signOut();
//                Navigation.findNavController(view).navigate(R.id.loginFragment);
//            }
//        });

        //diary log button
        Button btnDiary = view.findViewById(R.id.btnDiary);
        if (btnDiary != null) {
            btnDiary.setOnClickListener(v -> {
                // Navigate to the Diary Fragment
                Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_diaryFragment);
            });
        }

        if (auth != null) {
            loadUserData();
        }
    }

    private void loadUserData() {
        DocumentReference userRef = db.collection("impact_tracker").document(Objects.requireNonNull(auth.getUid()));

        userRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {

                // --- A. GET FOOTPRINT VALUE ---
                double totalSaved = 0.0;
                if (document.contains("gross_footprint") && document.get("gross_footprint") != null) {
                    totalSaved = document.getDouble("gross_footprint");
                }

                // --- B. UPDATE UI ---
                animateCarbonFootprintValue(totalSaved); // 1. Animate the Number
                updateTierUI(totalSaved);                // 2. Update the Badge/Tier

                // --- C. HANDLE STREAK ---
                handleStreakLogic(document, userRef);    // 3. Update the Streak

            } else {
                // New user / Empty data
                animateCarbonFootprintValue(0.0);
                updateTierUI(0);
                tvStreak.setText("🔥 0 Day Streak");
            }
        }).addOnFailureListener(e -> Log.e("HomeFragment", "Error loading user data", e));
    }

    private void fetchAndAnimateCarbonFootprint() {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            //fetch reference
            DocumentReference docRef = db.collection("impact_tracker").document(user.getUid());

            // 5. Get the data
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.contains("gross_footprint")) {
                    Double grossTotal = documentSnapshot.getDouble("gross_footprint");

                    if (grossTotal != null) {
                        animateCarbonFootprintValue(grossTotal);
                    } else {
                        animateCarbonFootprintValue(0.0);
                    }
                } else {
                    animateCarbonFootprintValue(0.0);
                }
            }).addOnFailureListener(e -> {
                Log.e("HomeFragment", "Error fetching carbon footprint", e);
                carbonFootPrintTextView.setText("Error fetching data");
            });
        }
    }
    private void animateCarbonFootprintValue(double finalValue) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) finalValue);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(1500);
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            if (carbonFootPrintTextView != null) {
                carbonFootPrintTextView.setText(String.format("%.2f", animatedValue));
            }
        });
        animator.start();
    }

    private void updateTierUI(double totalSaved) {
        if (tvTierName == null || imgTierBadge == null) return;

        if (totalSaved < 10.0) {
            tvTierName.setText("Bronze Recycler");
            imgTierBadge.setColorFilter(0xFFCD7F32); // Bronze Color
        } else if (totalSaved < 50.0) {
            tvTierName.setText("Silver Recycler");
            imgTierBadge.setColorFilter(0xFFC0C0C0); // Silver Color
        } else {
            tvTierName.setText("Gold Recycler");
            imgTierBadge.setColorFilter(0xFFFFD700); // Gold Color
        }
    }
    private void handleStreakLogic(com.google.firebase.firestore.DocumentSnapshot document, DocumentReference userRef) {
        if (tvStreak == null) return;

        long currentStreak = 0;
        String lastDate = "";

        if (document.contains("streak")) {
            currentStreak = document.getLong("streak");
        }
        if (document.contains("last_login_date")) {
            lastDate = document.getString("last_login_date");
        }

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (lastDate.equals(todayDate)) {
            // Already logged in today
        } else {
            // New day
            currentStreak++;
            Map<String, Object> updates = new HashMap<>();
            updates.put("streak", currentStreak);
            updates.put("last_login_date", todayDate);
            userRef.set(updates, SetOptions.merge());
        }

        tvStreak.setText("🔥 " + currentStreak + " Day Streak");
    }
}