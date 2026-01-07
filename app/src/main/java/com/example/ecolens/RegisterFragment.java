package com.example.ecolens;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class RegisterFragment extends Fragment {

    TextInputEditText etEmail, etPassword, etConfirmPassword, etUsername;
    Button btnRegister;
    TextView tvLogin;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etUsername = view.findViewById(R.id.etUsername);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        tvLogin = view.findViewById(R.id.tvLogin);


        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_loginFragment);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username, email, password, confirmPassword;
                username = String.valueOf(etUsername.getText());
                email = String.valueOf(etEmail.getText());
                password = String.valueOf(etPassword.getText());
                confirmPassword = String.valueOf(etConfirmPassword.getText());

                if(TextUtils.isEmpty(username)) {
                    Toast.makeText(view.getContext(), "Enter Username", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(TextUtils.isEmpty(email)) {
                    Toast.makeText(view.getContext(), "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(TextUtils.isEmpty(password)) {
                    Toast.makeText(view.getContext(), "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!TextUtils.equals(password, confirmPassword)) {
                    Toast.makeText(view.getContext(), "Password Not The Same", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                    String userId = firebaseUser.getUid();

                                    User newUser = new User(username, email, 0, 0, new ArrayList<>());

                                    mDatabase.child("users").child(userId).setValue(newUser)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> dbTask) {
                                                    if (dbTask.isSuccessful()) {
                                                        Toast.makeText(view.getContext(), "Account created.",
                                                                Toast.LENGTH_SHORT).show();
                                                        Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_homeFragment);
                                                    } else {
                                                        Toast.makeText(view.getContext(), "Database error: " + dbTask.getException().getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(view.getContext(), "Authentication failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });


    }
}
