package com.example.ecolens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
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
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserFragment extends Fragment {

    private static final String TAG = "UserFragment";

    // UI Components
    private TextView tvUsername, tvEmail, tvJoinedDate, tvImpactScore;
    private ImageView ivProfilePicture, imgBadgeBronze, imgBadgeSilver, imgBadgeGold;
    private TextView btnHistoryLog;
    private Button btnLogout, btnChangePassword, btnDeleteAccount;
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

        btnHistoryLog.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.diaryFragment)
        );

        btnChangePassword.setOnClickListener(v -> {
            if (auth.getCurrentUser() != null) {
                String email = auth.getCurrentUser().getEmail();
                if (email != null && !email.isEmpty()) {
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                                Log.d(TAG, "Password reset email sent to " + email);
                            } else {
                                Toast.makeText(getContext(), "Failed to send password reset email.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error sending password reset email", task.getException());
                            }
                        });
                } else {
                    Toast.makeText(getContext(), "Cannot send reset email. User email is not available.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "You need to be logged in to change your password.", Toast.LENGTH_LONG).show();
            }
        });

        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteUserAccount();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
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

                        Log.d(TAG, "Attempting to update Firestore for user: " + auth.getCurrentUser().getUid() + " with URL: " + imageUrl);
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

    private void deleteUserAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = user.getUid();

        // 1. Delete Firestore data
        db.collection("users").document(uid).delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User data from users collection deleted successfully.");
                // 2. Delete Impact Tracker data
                db.collection("impact_tracker").document(uid).delete()
                    .addOnSuccessListener(aVoid1 -> {
                        Log.d(TAG, "User data from impact_tracker collection deleted successfully.");
                        // 3. Delete Storage data
                        if (storageReference != null) {
                            storageReference.delete()
                                .addOnSuccessListener(aVoid2 -> Log.d(TAG, "User profile image deleted successfully."))
                                .addOnFailureListener(e -> Log.e(TAG, "Error deleting profile image.", e));
                        }
                        // 4. Delete user auth
                        user.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Account deleted successfully.", Toast.LENGTH_LONG).show();
                                    Navigation.findNavController(getView()).navigate(R.id.action_userFragment_to_loginFragment);
                                } else {
                                    if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                        Toast.makeText(getContext(), "This action requires recent authentication. Please log out and log in again to delete your account.", Toast.LENGTH_LONG).show();
                                        Log.w(TAG, "Error deleting user account: requires recent login", task.getException());
                                    } else {
                                        Toast.makeText(getContext(), "Failed to delete account. Please try again.", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Error deleting user account", task.getException());
                                    }
                                }
                            });
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error deleting impact_tracker data.", e));
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error deleting users data.", e));
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();

        // Load user profile data
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                tvUsername.setText(document.getString("name"));
                tvEmail.setText(document.getString("email"));

                if (document.contains("dateJoined") && document.getTimestamp("dateJoined") != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                    tvJoinedDate.setText(sdf.format(document.getTimestamp("dateJoined").toDate()));
                } else {
                    tvJoinedDate.setText("N/A");
                }

                if (document.contains("profileImageUrl") && getContext() != null) {
                    Glide.with(getContext()).load(document.getString("profileImageUrl")).into(ivProfilePicture);
                }
            } else {
                Log.d(TAG, "No such document in users collection");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));

        // Load user impact data
        db.collection("impact_tracker").document(uid).get().addOnSuccessListener(document -> {
            double totalScore = 0.0;
            if (document.exists() && document.contains("gross_footprint") && document.get("gross_footprint")!=null) {
                totalScore = document.getDouble("gross_footprint");

            }else{
                totalScore=0.0;
            }
            tvImpactScore.setText(String.format(Locale.getDefault(), "%.2f kg", totalScore));
            updateBadges(totalScore);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading impact data", e);
            tvImpactScore.setText("0.00 kg");
            updateBadges(0.0);
        });
    }

    private void updateBadges(double score) {
        // Colors
        int colorBronze = 0xFFCD7F32;
        int colorSilver = 0xFFC0C0C0;
        int colorGold   = 0xFFFFD700;
        int colorLocked = 0xFFE0E0E0; // Light Gray

        // Bronze is always unlocked
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
