package com.example.ecolens;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class UserFragment extends Fragment {

    private static final String TAG = "UserFragment";

    // UI Components
    private TextView tvUsername, tvEmail, tvJoinedDate, tvImpactScore;
    private ImageView ivProfilePicture, imgBadgeBronze, imgBadgeSilver, imgBadgeGold;
    private TextView btnHistoryLog, btnChangePassword, btnDeleteAccount;
    private Button btnLogout, btnEditProfile;
    private FloatingActionButton fabChangePic;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private StorageReference storageReference;

    private Uri imageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) {
            storageReference = FirebaseStorage.getInstance().getReference("profile_pictures/").child(auth.getCurrentUser().getUid());
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        imageUri = result.getData().getData();
                        ivProfilePicture.setImageURI(imageUri);
                        uploadImageToFirebase();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        tvUsername = view.findViewById(R.id.tvProfileUsername);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        tvJoinedDate = view.findViewById(R.id.tvJoinedDate);
        tvImpactScore = view.findViewById(R.id.tvImpactScore);
        imgBadgeBronze = view.findViewById(R.id.imgBadgeBronze);
        imgBadgeSilver = view.findViewById(R.id.imgBadgeSilver);
        imgBadgeGold = view.findViewById(R.id.imgBadgeGold);
        btnHistoryLog = view.findViewById(R.id.btnHistoryLog);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        fabChangePic = view.findViewById(R.id.fabChangeProfilePic);

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

        fabChangePic.setOnClickListener(v -> openFileChooser());

        // ... other listeners
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToFirebase() {
        if (imageUri != null && storageReference != null) {
            Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();
            storageReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        Map<String, Object> data = new HashMap<>();
                        data.put("profileImageUrl", imageUrl);

                        db.collection("users").document(auth.getCurrentUser().getUid())
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                                    if (getContext() != null) {
                                        Glide.with(getContext()).load(imageUrl).into(ivProfilePicture);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to save URL to database.", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Firestore update failed: ", e);
                                });
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Upload failed: ", e);
                    });
        }
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                tvUsername.setText(document.getString("name"));
                tvEmail.setText(document.getString("email"));
                // Format and set joined date

                if (document.contains("profileImageUrl") && getContext() != null) {
                    Glide.with(getContext()).load(document.getString("profileImageUrl")).into(ivProfilePicture);
                }
            }
        });
        // ... load other data (impact score, badges)
    }
}
