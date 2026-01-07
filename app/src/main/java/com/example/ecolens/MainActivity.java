package com.example.ecolens;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);
            MaterialCardView bottomNavCard = findViewById(R.id.bottom_nav_card);

            // 1. Link the NavController to the BottomNavigationView
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // 2. Handle visibility logic
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.loginFragment || destination.getId() == R.id.registerFragment) {
                    bottomNavCard.setVisibility(View.GONE);
                } else {
                    bottomNavCard.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}