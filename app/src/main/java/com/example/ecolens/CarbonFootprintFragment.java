package com.example.ecolens;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class CarbonFootprintFragment extends Fragment {

    // --- Views ---
    private EditText editTextPlastic, editTextPaper, editTextAluminium, editTextGlass, editTextOrganic;
    private Button buttonCalculate;
    // We now correctly reference both TextViews separately
    private TextView textViewResult, textViewGrossTotal;
    private ImageView imageViewFootprint;
    private Group groupInputs;

    // --- Firebase (Firestore) ---
    private DocumentReference userImpactDocRef;
    private FirebaseUser currentUser;

    // --- Data ---
    private double grossTotal = 0.0;

    // --- Constants ---
    private static final double EMISSION_FACTOR_PLASTIC = 1.3;
    private static final double EMISSION_FACTOR_PAPER = 0.9;
    private static final double EMISSION_FACTOR_ALUMINIUM = 8.1;
    private static final double EMISSION_FACTOR_GLASS = 0.3;
    private static final double EMISSION_FACTOR_ORGANIC = 0.1;

    private static final float MAX_SCALE = 3.0f;
    private static final double MAX_CARBON_FOR_SCALING = 100.0;

    private static final String TAG = "CarbonFootprintFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_carbon_footprint, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userImpactDocRef = FirebaseFirestore.getInstance()
                    .collection("impact_tracker")
                    .document(currentUser.getUid());
        }

        initializeViews(view);
        setupTextWatchers();
        fetchGrossTotal();

        buttonCalculate.setOnClickListener(v -> calculateCarbonFootprint());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View backButton = view.findViewById(R.id.btn_back_manual);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Navigation.findNavController(view).navigateUp();
            });
        }
    }

    private void initializeViews(View view) {
        editTextPlastic = view.findViewById(R.id.editTextPlastic);
        editTextPaper = view.findViewById(R.id.editTextPaper);
        editTextAluminium = view.findViewById(R.id.editTextAluminium);
        editTextGlass = view.findViewById(R.id.editTextGlass);
        editTextOrganic = view.findViewById(R.id.editTextOrganic);
        buttonCalculate = view.findViewById(R.id.buttonCalculate);
        textViewGrossTotal = view.findViewById(R.id.textViewGrossTotal);
        imageViewFootprint = view.findViewById(R.id.imageViewFootprint);
        groupInputs = view.findViewById(R.id.groupInputs);
        textViewResult = view.findViewById(R.id.textViewResult);
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {}
        };
        editTextPlastic.addTextChangedListener(textWatcher);
        editTextPaper.addTextChangedListener(textWatcher);
        editTextAluminium.addTextChangedListener(textWatcher);
        editTextGlass.addTextChangedListener(textWatcher);
        editTextOrganic.addTextChangedListener(textWatcher);
    }

    private void fetchGrossTotal() {
        if (userImpactDocRef != null) {
            userImpactDocRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    grossTotal = 0.0;
                    updateGrossTotalText();
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    if (snapshot.contains("gross_footprint") && snapshot.get("gross_footprint") != null) {
                        grossTotal = snapshot.getDouble("gross_footprint");
                    } else {
                        grossTotal = 0.0;
                    }
                } else {
                    Log.d(TAG, "Current data: null. New user or first-time use.");
                    grossTotal = 0.0;
                }
                updateGrossTotalText();
            });
        }
    }

    // This method ONLY updates the gross total TextView.
    private void updateGrossTotalText() {
        if (textViewGrossTotal != null) {
            textViewGrossTotal.setText(String.format("Gross Total Saved: %.2f kg CO2e", grossTotal));
        }
    }

    // In CarbonFootprintFragment.java

    // In C:/Users/User/AndroidStudioProjects/ecolens/app/src/main/java/com/example/ecolens/CarbonFootprintFragment.java

// In C:/Users/User/AndroidStudioProjects/ecolens/app/src/main/java/com/example/ecolens/CarbonFootprintFragment.java

    // In C:/Users/User/AndroidStudioProjects/ecolens/app/src/main/java/com/example/ecolens/CarbonFootprintFragment.java

    private void calculateCarbonFootprint() {
        double sessionTotal = 0;
        sessionTotal += getWeight(editTextPlastic) * EMISSION_FACTOR_PLASTIC;
        sessionTotal += getWeight(editTextPaper) * EMISSION_FACTOR_PAPER;
        sessionTotal += getWeight(editTextAluminium) * EMISSION_FACTOR_ALUMINIUM;
        sessionTotal += getWeight(editTextGlass) * EMISSION_FACTOR_GLASS;
        sessionTotal += getWeight(editTextOrganic) * EMISSION_FACTOR_ORGANIC;

        textViewResult.setText(String.format("This Time: %.2f kg CO2e", sessionTotal));

        // This is required for the lambda expression.
        final double finalSessionTotal = sessionTotal;
        Log.w(TAG, "userimpactdocref:"+userImpactDocRef);

        // --- Definitive Get-then-Set Solution ---
        if (userImpactDocRef != null && currentUser != null) {

            // 1. Explicitly get the document first.
            userImpactDocRef.get().addOnSuccessListener(documentSnapshot -> {
                double currentGrossTotal = 0.0;

                // 2. Safely get the current total if the document exists.
                if (documentSnapshot.exists() && documentSnapshot.contains("gross_footprint")) {
                    currentGrossTotal = documentSnapshot.getDouble("gross_footprint");
                }

                // 3. Calculate the new total on the app side.
                double newGrossTotal = currentGrossTotal + finalSessionTotal;

                // 4. Create the complete data map required by your security rules.
                Map<String, Object> data = new HashMap<>();
                data.put("gross_footprint", newGrossTotal);
                data.put("user_id", currentUser.getUid());

                // 5. Use set(merge) to CREATE or UPDATE the document.
                // This is the key change to handle new users correctly.
                userImpactDocRef.set(data, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Set with merge successful!"))
                        .addOnFailureListener(e -> {
                            // If an error still occurs here, it's a fundamental rules mismatch,
                            // but this logic is now sound for create/update.
                            Log.w(TAG, "Set with merge failed: ", e);
                        });

            }).addOnFailureListener(e -> {
                // This would fail if the user doesn't even have read permission.
                Log.w(TAG, "Failed to get document before setting data: ", e);
            });
        }else {
            // --- ADDED CONDITION ---
            // Log an error if the user is not authenticated or the doc ref is null
            Log.e(TAG, "Cannot update gross total: User is not authenticated or userImpactDocRef is null.");
        }

        groupInputs.setVisibility(View.GONE);
        imageViewFootprint.setVisibility(View.VISIBLE);
        textViewResult.setVisibility(View.VISIBLE);

        animateResultText(sessionTotal);
        animateFootprint(sessionTotal);
    }




    // This animation method now ONLY updates the session result TextView.
    private void animateResultText(double finalValue) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) finalValue);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            // This now correctly updates ONLY the session TextView, leaving the gross total alone.
            textViewResult.setText(String.format("This Time: %.2f kg CO2e", animatedValue));
        });
        animator.start();
    }

    private void animateFootprint(double totalCarbonFootprint) {
        double normalizedFootprint = Math.min(totalCarbonFootprint, MAX_CARBON_FOR_SCALING);
        double logValue = Math.log1p(normalizedFootprint);
        double maxLogValue = Math.log1p(MAX_CARBON_FOR_SCALING);
        float scale = 1.0f + (float) ((logValue / maxLogValue) * (MAX_SCALE - 1.0f));

        ScaleAnimation scaleAnimation = new ScaleAnimation(0.0f, scale, 0.0f, scale, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(1000);
        scaleAnimation.setFillAfter(true);
        imageViewFootprint.startAnimation(scaleAnimation);
    }

    private double getWeight(EditText editText) {
        String text = editText.getText().toString();
        return text.isEmpty() ? 0 : Double.parseDouble(text);
    }
}
