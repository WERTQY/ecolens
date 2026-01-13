package com.example.ecolens;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserFragment extends Fragment {

    // UI Components
    private TextView tvUsername, tvEmail, tvJoinedDate, tvImpactScore;
    private ImageView imgBadgeBronze, imgBadgeSilver, imgBadgeGold;
    private TextView btnHistoryLog, btnChangePassword, btnDeleteAccount;
    private Button btnLogout, btnEditProfile;
    private FloatingActionButton fabChangePic;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);

        setupListeners(view);

        if (auth.getCurrentUser() != null) {
            loadUserData();
        } else {
            // back to login if no user
            Navigation.findNavController(view).navigate(R.id.action_userFragment_to_loginFragment);
        }
    }

    private void initViews(View view) {
        tvUsername = view.findViewById(R.id.tvProfileUsername);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvJoinedDate = view.findViewById(R.id.tvJoinedDate);
        tvImpactScore = view.findViewById(R.id.tvImpactScore);

        // badge
        imgBadgeBronze = view.findViewById(R.id.imgBadgeBronze);
        imgBadgeSilver = view.findViewById(R.id.imgBadgeSilver);
        imgBadgeGold = view.findViewById(R.id.imgBadgeGold);

        // button
        btnHistoryLog = view.findViewById(R.id.btnHistoryLog);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        fabChangePic = view.findViewById(R.id.fabChangeProfilePic);

        // go back
        View btnBack = view.findViewById(R.id.btn_back_manual);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());
        }
    }

    private void setupListeners(View view) {
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigate(R.id.action_userFragment_to_loginFragment);
        });

        btnHistoryLog.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.diaryFragment)
        );

        // Placeholders for future features
        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(getContext(), "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnChangePassword.setOnClickListener(v ->
                Toast.makeText(getContext(), "Password change feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        fabChangePic.setOnClickListener(v ->
                Toast.makeText(getContext(), "Change Photo feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnDeleteAccount.setOnClickListener(v ->
                Toast.makeText(getContext(), "Please contact admin to delete account.", Toast.LENGTH_LONG).show()
        );
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String email = document.getString("email");

                        if (name != null) tvUsername.setText(name);
                        if (email != null) tvEmail.setText(email);

                        //join date must add
                        tvJoinedDate.setText("Jan 2025");
                    }
                })
                .addOnFailureListener(e -> Log.e("UserFragment", "Error loading user details", e));

        db.collection("impact_tracker").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        double totalScore = 0.0;
                        if (document.contains("gross_footprint") && document.get("gross_footprint") != null) {
                            totalScore = document.getDouble("gross_footprint");
                        }
                        tvImpactScore.setText(String.format("%.2f kg", totalScore));

                        updateBadges(totalScore);
                    } else {
                        tvImpactScore.setText("0.00 kg");
                        updateBadges(0.0);
                    }
                })
                .addOnFailureListener(e -> Log.e("UserFragment", "Error loading impact stats", e));
    }

    private void updateBadges(double score) {
        // Colors
        int colorBronze = 0xFFCD7F32;
        int colorSilver = 0xFFC0C0C0;
        int colorGold   = 0xFFFFD700;
        int colorLocked = 0xFFE0E0E0; // Light Gray

        //
        imgBadgeBronze.setColorFilter(colorBronze, PorterDuff.Mode.SRC_IN);
        imgBadgeBronze.setAlpha(1.0f);

        // 2. Silver (Unlock at 10kg)
        if (score >= 10.0) {
            imgBadgeSilver.setColorFilter(colorSilver, PorterDuff.Mode.SRC_IN);
            imgBadgeSilver.setAlpha(1.0f);
        } else {
            imgBadgeSilver.setColorFilter(colorLocked, PorterDuff.Mode.SRC_IN);
            imgBadgeSilver.setAlpha(0.5f); // Make it look faded
        }

        // 3. Gold (Unlock at 50kg)
        if (score >= 50.0) {
            imgBadgeGold.setColorFilter(colorGold, PorterDuff.Mode.SRC_IN);
            imgBadgeGold.setAlpha(1.0f);
        } else {
            imgBadgeGold.setColorFilter(colorLocked, PorterDuff.Mode.SRC_IN);
            imgBadgeGold.setAlpha(0.5f); // Make it look faded
        }
    }
}