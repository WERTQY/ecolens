package com.example.ecolens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback, MenuProvider {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private final List<RecyclingCenter> allCenters = new ArrayList<>();
    private String currentFilter = "All";
    private String currentSearchQuery = "";

    // Permission Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableMyLocation();
                } else {
                    Toast.makeText(getContext(), "Location permission needed to show your position", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        db = FirebaseFirestore.getInstance();

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupFilterButtons(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Setup Search Menu
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 1. Default Location (KL)
        LatLng defaultLocation = new LatLng(3.1390, 101.6869);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

        // 2. Enable "My Location" Blue Dot
        checkLocationPermissionAndEnableMap();

        // 3. Load Data
        fetchRecyclingCenters();

        // 4. Click Marker -> Open Google Maps App (Navigation)
        mMap.setOnInfoWindowClickListener(marker -> {
            double lat = marker.getPosition().latitude;
            double lng = marker.getPosition().longitude;
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(getContext(), "Google Maps app not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkLocationPermissionAndEnableMap() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    // --- MENU & SEARCH LOGIC ---
    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentSearchQuery = query;
                    filterAndShowMarkers();
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentSearchQuery = newText;
                    filterAndShowMarkers();
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

    // --- DATA LOGIC ---
    private void fetchRecyclingCenters() {
        db.collection("recycling_centers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allCenters.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Ensure your RecyclingCenter.java has an empty constructor!
                            RecyclingCenter center = document.toObject(RecyclingCenter.class);
                            allCenters.add(center);
                        }
                        filterAndShowMarkers();
                    } else {
                        Log.e("EcoLens", "Error getting documents: ", task.getException());
                        Toast.makeText(getContext(), "Failed to load map data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterAndShowMarkers() {
        if (mMap == null) return;
        mMap.clear();

        List<RecyclingCenter> matchingCenters = new ArrayList<>();

        for (RecyclingCenter center : allCenters) {
            boolean matchesFilter = currentFilter.equals("All") || center.getType().equalsIgnoreCase(currentFilter);
            boolean matchesSearch = currentSearchQuery.isEmpty() || center.getName().toLowerCase(Locale.ROOT).contains(currentSearchQuery.toLowerCase(Locale.ROOT));

            if (matchesFilter && matchesSearch) {
                matchingCenters.add(center);
            }
        }

        for (RecyclingCenter center : matchingCenters) {
            LatLng position = new LatLng(center.getLatitude(), center.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(center.getName())
                    .snippet(center.getAddress())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_recycle_marker)));
        }

        // If searching and found exactly 1 result, zoom in on it
        if (matchingCenters.size() == 1 && !currentSearchQuery.isEmpty()) {
            LatLng position = new LatLng(matchingCenters.get(0).getLatitude(), matchingCenters.get(0).getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        }
    }

    private void setupFilterButtons(View view) {
        // You MUST have these IDs in your XML file
        Button btnAll = view.findViewById(R.id.btnFilterAll);
        Button btnEwaste = view.findViewById(R.id.btnFilterEwaste);
        Button btnPaper = view.findViewById(R.id.btnFilterPaper);
        Button btnFabric = view.findViewById(R.id.btnFilterFabric);
        Button btnGlass = view.findViewById(R.id.btnFilterGlass);

        btnAll.setOnClickListener(v -> { currentFilter = "All"; filterAndShowMarkers(); });
        btnEwaste.setOnClickListener(v -> { currentFilter = "E-Waste"; filterAndShowMarkers(); });
        btnPaper.setOnClickListener(v -> { currentFilter = "Paper"; filterAndShowMarkers(); });
        btnFabric.setOnClickListener(v -> { currentFilter = "Fabric"; filterAndShowMarkers(); });
        btnGlass.setOnClickListener(v -> { currentFilter = "Glass"; filterAndShowMarkers(); });
    }
}
