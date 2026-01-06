package com.example.ecolens;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    // To store all fetched recycling centers and their markers
    private final List<RecyclingCenter> allCenters = new ArrayList<>();
    private final List<Marker> allMarkers = new ArrayList<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    // If permission is denied, still fetch and show the centers
                    fetchRecyclingCenters();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSearchView(view);
        setupFilterChips(view); // NEW: Call method to set up chip listeners

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true); // Optional: Add zoom buttons
        checkLocationPermissionAndGetLocation();
    }

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f)); // Zoom out a bit to see more area
            } else {
                Toast.makeText(getContext(), "Could not get current location. Enable location services.", Toast.LENGTH_LONG).show();
            }
            // Fetch centers AFTER we have the user's location
            fetchRecyclingCenters();
        });
    }

    // --- NEW: Fetch all center data from Firestore ---
    private void fetchRecyclingCenters() {
        db.collection("recycling_centers") // Make sure your collection is named this!
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mMap.clear(); // Clear old markers before adding new ones
                        allCenters.clear();
                        allMarkers.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Convert Firestore document to our RecyclingCenter object
                            RecyclingCenter center = document.toObject(RecyclingCenter.class);
                            allCenters.add(center);

                            // Create a marker for each center
                            LatLng position = new LatLng(center.getLatitude(), center.getLongitude());
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(center.getName())
                                    .snippet("Type: " + center.getType())
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))); // Red marker
                            marker.setTag(center.getType()); // IMPORTANT: Tag the marker with its type for filtering
                            allMarkers.add(marker);
                        }
                        Log.d(TAG, "Fetched " + allCenters.size() + " recycling centers.");
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(getContext(), "Failed to fetch recycling centers.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- NEW: Set up listeners for the filter chips ---
    private void setupFilterChips(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.filter_chip_group);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) {
                filterMarkers("All");
            } else if (checkedId == R.id.chip_ewaste) {
                filterMarkers("E-Waste");
            } else if (checkedId == R.id.chip_paper) {
                filterMarkers("Paper");
            } else if (checkedId == R.id.chip_fabric) {
                filterMarkers("Fabric");
            } else if (checkedId == R.id.chip_glass) {
                filterMarkers("Glass");
            } else if (checkedId == R.id.chip_cooking_oil) {
                filterMarkers("Cooking Oil");
            }
        });
    }

    // --- NEW: Logic to show/hide markers based on filter ---
    private void filterMarkers(String type) {
        for (Marker marker : allMarkers) {
            if (type.equals("All")) {
                marker.setVisible(true); // Show all markers
            } else {
                // Get the type we stored in the marker's tag
                String markerType = (String) marker.getTag();
                // Show marker only if its type matches the filter
                marker.setVisible(type.equalsIgnoreCase(markerType));
            }
        }
    }
    
    // --- SEARCH BAR LOGIC (no changes needed here, just ensure it's present) ---
    private void setupSearchView(View view) {
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                searchView.clearFocus();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });
    }
    
    private void searchLocation(String locationName) {
        if (locationName == null || locationName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a location name", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                // When searching, we don't clear the recycling center markers
                // We just move the camera
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f));
            } else {
                Toast.makeText(getContext(), "Location not found: " + locationName, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoding failed", e);
            Toast.makeText(getContext(), "Service not available", Toast.LENGTH_SHORT).show();
        }
    }
}
