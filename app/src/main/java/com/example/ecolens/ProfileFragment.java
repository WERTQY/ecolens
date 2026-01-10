package com.example.ecolens;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class ProfileFragment extends Fragment {

    private TextView tvProfileUsername, tvProfileEmail;
    private ImageView ivProfilePicture;
    private FloatingActionButton fabChangeProfilePic;
    private Button btnLogout;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadProfilePicture(imageUri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        mStorage = FirebaseStorage.getInstance().getReference();

        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        fabChangeProfilePic = view.findViewById(R.id.fabChangeProfilePic);
        tvProfileUsername = view.findViewById(R.id.tvProfileUsername);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        btnLogout = view.findViewById(R.id.btnLogout);

        if (currentUser == null) {
            Navigation.findNavController(view).navigate(R.id.action_profileFragment_to_loginFragment);
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvProfileUsername.setText(user.username);
                    tvProfileEmail.setText(user.email);

                    if (user.profilePictureUrl != null && !user.profilePictureUrl.isEmpty()) {
                        Glide.with(requireContext()).load(user.profilePictureUrl).into(ivProfilePicture);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });

        fabChangeProfilePic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigate(R.id.action_profileFragment_to_loginFragment);
        });
    }

    private void uploadProfilePicture(Uri imageUri) {
        if (mAuth.getCurrentUser() == null) return;

        final String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = mStorage.child("profile_pictures/" + mAuth.getCurrentUser().getUid() + "/" + fileName);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(this::updateUserProfileUrl))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateUserProfileUrl(Uri uri) {
        String downloadUrl = uri.toString();
        mDatabase.child("profilePictureUrl").setValue(downloadUrl)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update database.", Toast.LENGTH_SHORT).show());
    }
}
