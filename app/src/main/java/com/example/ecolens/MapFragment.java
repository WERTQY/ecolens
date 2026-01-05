package com.example.ecolens;

import android.Manifest;
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
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private List<RecyclingCenter> allCenters = new ArrayList<>(); // Stores all data from Firebase
    private String currentFilter = "All";
    private String currentSearchQuery = "";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted. Enable the My Location layer.
                    enableMyLocation();
                } else {
                    // Permission was denied. Show a message.
                    Toast.makeText(getContext(), "Location permission denied. Cannot show current location.", Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        setHasOptionsMenu(true);
        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupFilterButtons(view);
        
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                filterAndShowMarkers();
                searchView.clearFocus(); // Hide keyboard
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterAndShowMarkers();
                return true;
            }
        });

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 1. Always set a default starting position to prevent a blank map.
        LatLng defaultLocation = new LatLng(3.1390, 101.6869);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

        // 2. Check for permission to enable the blue dot and the "My Location" button.
        checkLocationPermissionAndEnableMap();

        // 3. Load the recycling center data.
        fetchRecyclingCenters();

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
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            // Permission is not granted. Request it.
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableMyLocation() {
        // This is the safe and standard way.
        // It ONLY enables the blue dot and the "My Location" button.
        // It does NOT move the camera automatically.
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                filterAndShowMarkers();
                searchView.clearFocus(); // Hide keyboard
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

    private void fetchRecyclingCenters() {
        db.collection("recycling_centers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allCenters.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
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

        // This part is for the search zoom. It is correct.
        if (matchingCenters.size() == 1 && !currentSearchQuery.isEmpty()) {
            RecyclingCenter singleResult = matchingCenters.get(0);
            LatLng position = new LatLng(singleResult.getLatitude(), singleResult.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        }
    }

    private void setupFilterButtons(View view) {
        Button btnAll = view.findViewById(R.id.btnFilterAll);
        Button btnEwaste = view.findViewById(R.id.btnFilterEwaste);
        Button btnPaper = view.findViewById(R.id.btnFilterPaper);
        Button btnFabric = view.findViewById(R.id.btnFilterFabric);
        Button btnGlass = view.findViewById(R.id.btnFilterGlass);

        btnAll.setOnClickListener(v -> {
            currentFilter = "All";
            filterAndShowMarkers();
        });
        btnEwaste.setOnClickListener(v -> {
            currentFilter = "E-Waste";
            filterAndShowMarkers();
        });
        btnPaper.setOnClickListener(v -> {
            currentFilter = "Paper";
            filterAndShowMarkers();
        });
        btnFabric.setOnClickListener(v -> {
            currentFilter = "Fabric";
            filterAndShowMarkers();
        });
        btnGlass.setOnClickListener(v -> {
            currentFilter = "Glass";
            filterAndShowMarkers();
        });
    }
}
