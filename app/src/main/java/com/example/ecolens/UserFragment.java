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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserFragment extends Fragment {

    private static final String TAG = "UserFragment";
    private ImageButton btnEditName;
    // UI Components
    private TextView tvUsername, tvEmail, tvJoinedDate, tvImpactScore;
    private ImageView ivProfilePicture, imgBadgeBronze, imgBadgeSilver, imgBadgeGold, imgBadgePlatinum, imgBadgeDiamond;
    private Map<String, com.google.firebase.Timestamp> tierDates = new HashMap<>();
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
        imgBadgePlatinum = view.findViewById(R.id.imgBadgePlatinum);
        imgBadgeDiamond = view.findViewById(R.id.imgBadgeDiamond);
        btnHistoryLog = view.findViewById(R.id.btnHistoryLog);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnLogout = view.findViewById(R.id.btnLogout);
        fabChangePic = view.findViewById(R.id.fabChangeProfilePic);
        btnEditName = view.findViewById(R.id.btnEditName);

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

        btnEditName.setOnClickListener(v -> showEditNameDialog());
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

    private void showEditNameDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null);

        TextInputEditText etName = dialogView.findViewById(R.id.etNewName);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        etName.setText(tvUsername.getText().toString());
        etName.setSelection(etName.getText().length()); // Move cursor to end

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView); // Set the custom view
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateNameInFirebase(newName);
                dialog.dismiss();
            } else {
                etName.setError("Name cannot be empty");
            }
        });

        dialog.show();
    }

    private void updateNameInFirebase(String newName) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User profile updated.");

                db.collection("users").document(user.getUid())
                        .update("name", newName)
                        .addOnSuccessListener(aVoid -> {
                            tvUsername.setText(newName);
                            Toast.makeText(getContext(), "Name updated successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update database.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error updating Firestore", e);
                        });
            } else {
                Toast.makeText(getContext(), "Failed to update profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteUserAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = user.getUid();

        // 1. Delete Firestore User Data
        db.collection("users").document(uid).delete()
                .addOnCompleteListener(task1 -> {
                    Log.d(TAG, "Step 1: User Firestore Data processed.");

                    // 2. Delete Impact Tracker Data (Using OnComplete to continue even if file missing)
                    db.collection("impact_tracker").document(uid).delete()
                            .addOnCompleteListener(task2 -> {
                                Log.d(TAG, "Step 2: Impact Tracker Data processed.");

                                // 3. Attempt to delete Profile Picture
                                if (storageReference != null) {
                                    storageReference.delete().addOnCompleteListener(task3 -> {
                                        // Whether image delete SUCCEEDED or FAILED (e.g. no image),
                                        // we NOW proceed to delete the account.
                                        // This ensures we don't delete the account before trying to delete the image.
                                        performAuthDeletion(user);
                                    });
                                } else {
                                    // No storage reference? Skip straight to Auth delete
                                    performAuthDeletion(user);
                                }
                            });
                });
    }

    // Helper method to keep the code clean
    private void performAuthDeletion(FirebaseUser user) {
        user.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Account deleted successfully.", Toast.LENGTH_LONG).show();
                Navigation.findNavController(getView()).navigate(R.id.action_userFragment_to_loginFragment);
            } else {
                if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                    Toast.makeText(getContext(), "Security: Please Log Out and Log In again to delete account.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Failed to delete account.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting auth", task.getException());
                }
            }
        });
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

        db.collection("impact_tracker").document(uid).get().addOnSuccessListener(document -> {
            double totalScore = 0.0;
            if (document.exists()) {
                if (document.contains("gross_footprint") && document.get("gross_footprint") != null) {
                    totalScore = document.getDouble("gross_footprint");
                }

                if (document.contains("tier_dates") && document.get("tier_dates") != null) {
                    tierDates = (Map<String, com.google.firebase.Timestamp>) document.get("tier_dates");
                }
            }

            tvImpactScore.setText(String.format(Locale.getDefault(), "%.2f kg", totalScore));
            checkAndSaveTiers(uid, totalScore);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading impact data", e);
        });
    }

    private void updateBadgesUI(double score) {
        // 1. Define Colors
        int colorBronze   = 0xFFCD7F32;
        int colorSilver   = 0xFFC0C0C0;
        int colorGold     = 0xFFFFD700;
        int colorPlatinum = 0xFFE5E4E2; // Shiny Gray/White
        int colorDiamond  = 0xFFB9F2FF; // Light Cyan/Blue

        // 2. Update Visuals
        configureBadge(imgBadgeBronze,   score >= 0,   colorBronze,   "bronze",   "Bronze Tier");
        configureBadge(imgBadgeSilver,   score >= 10,  colorSilver,   "silver",   "Silver Tier");
        configureBadge(imgBadgeGold,     score >= 50,  colorGold,     "gold",     "Gold Tier");
        configureBadge(imgBadgePlatinum, score >= 100, colorPlatinum, "platinum", "Platinum Tier");
        configureBadge(imgBadgeDiamond,  score >= 500, colorDiamond,  "diamond",  "Diamond Tier");
    }

    // update badges helper
    private void configureBadge(ImageView badge, boolean isUnlocked, int color, String key, String tierName) {
        if (isUnlocked) {
            // UNLOCKED STATE
            badge.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            badge.setAlpha(1.0f);

            badge.setOnClickListener(v -> {
                if (tierDates.containsKey(key)) {
                    // Show the date
                    com.google.firebase.Timestamp ts = tierDates.get(key);
                    if (ts != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        Toast.makeText(getContext(), tierName + " unlocked on: " + sdf.format(ts.toDate()), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), tierName + " Unlocked!", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            // LOCKED STATE
            badge.setColorFilter(0xFFE0E0E0, PorterDuff.Mode.SRC_IN); // Gray
            badge.setAlpha(0.4f); // Faded

            badge.setOnClickListener(v ->
                    Toast.makeText(getContext(), tierName + " is locked.", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void checkAndSaveTiers(String uid, double score) {
        Map<String, Object> updates = new HashMap<>();
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        if (!tierDates.containsKey("bronze")) {
            updates.put("tier_dates.bronze", now);
            tierDates.put("bronze", now);
            logTierHistory(uid, "Bronze");
        }

        if (score >= 10.0 && !tierDates.containsKey("silver")) {
            updates.put("tier_dates.silver", now);
            tierDates.put("silver", now);
            logTierHistory(uid, "Silver");
        }

        if (score >= 50.0 && !tierDates.containsKey("gold")) {
            updates.put("tier_dates.gold", now);
            tierDates.put("gold", now);
            logTierHistory(uid, "Gold");
        }

        if (score >= 100.0 && !tierDates.containsKey("platinum")) {
            updates.put("tier_dates.platinum", now);
            tierDates.put("platinum", now);
            logTierHistory(uid, "Platinum");
        }

        if (score >= 500.0 && !tierDates.containsKey("diamond")) {
            updates.put("tier_dates.diamond", now);
            tierDates.put("diamond", now);
            logTierHistory(uid, "Diamond");
        }

        if (!updates.isEmpty()) {
            db.collection("impact_tracker").document(uid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "New tier dates saved!"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save tier dates", e));
        }

        updateBadgesUI(score);
    }

    private void logTierHistory(String uid, String tierName) {
        Map<String, Object> historyData = new HashMap<>();

        // history list field
        historyData.put("timestamp", com.google.firebase.Timestamp.now());
        historyData.put("amount", 0.0);

        // Display history list
        historyData.put("wasteType", "Unlocked " + tierName + " Tier! 🏆");

        db.collection("impact_tracker").document(uid)
                .collection("history")
                .add(historyData)
                .addOnSuccessListener(ref -> Log.d(TAG, "Tier unlock logged to history: " + tierName))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to log tier history", e));
    }
}
