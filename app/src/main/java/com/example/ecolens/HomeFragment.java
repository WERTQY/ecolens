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
import android.widget.TextView;
import android.view.animation.DecelerateInterpolator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    private TextView carbonFootPrintTextView;
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
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        carbonFootPrintTextView = view.findViewById(R.id.carbon_footprint_value);
        fetchAndAnimateCarbonFootprint();


        //logout button
        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Navigation.findNavController(view).navigate(R.id.loginFragment);
            }
        });

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
}