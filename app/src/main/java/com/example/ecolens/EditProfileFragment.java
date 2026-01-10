package com.example.ecolens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        EditText etUsername = view.findViewById(R.id.etUsername);
        EditText etMunicipality = view.findViewById(R.id.etMunicipality);
        Button btnSave = view.findViewById(R.id.btnSave);

        if (currentUser == null) {
            // Should not happen, but as a safeguard.
            return;
        }

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        // Load existing data into the EditText fields
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    etUsername.setText(user.username);
                    etMunicipality.setText(user.municipality);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up the save button listener
        btnSave.setOnClickListener(v -> {
            String newUsername = etUsername.getText().toString().trim();
            String newMunicipality = etMunicipality.getText().toString().trim();

            if (newUsername.isEmpty()) {
                etUsername.setError("Username cannot be empty");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("username", newUsername);
            updates.put("municipality", newMunicipality);

            mDatabase.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        // Go back to the profile screen
                        Navigation.findNavController(requireView()).popBackStack();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update profile.", Toast.LENGTH_SHORT).show());
        });
    }
}
