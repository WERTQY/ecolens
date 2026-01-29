package com.example.ecolens;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.health.connect.datatypes.ExerciseRoute;
import android.location.Location;
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
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private List<RecyclingCenter> allCenters = new ArrayList<>(); // Stores all data from Firebase
    private String currentFilter = "All";
    private String currentSearchQuery = "";
    private ImageButton btnBackMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FloatingActionButton btnMyLocation;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // enable location
                    enableMyLocation();
                    getDeviceLocation();
                } else {
                    // location denied
                    Toast.makeText(getContext(), "Location permission denied. Cannot show current location.", Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        setHasOptionsMenu(true);
        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnBackMap = view.findViewById(R.id.btn_back_map);
        btnBackMap.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).navigate(R.id.homeFragment);
            }
        });

        btnMyLocation = view.findViewById(R.id.btn_my_location);
        btnMyLocation.setOnClickListener(v -> {
            getDeviceLocation();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupChipFilters(view);
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        int topPadding = (int)(150 * getResources().getDisplayMetrics().density);
        mMap.setPadding(0, topPadding, 0, 0);
        mMap.setPadding(0, topPadding,0,0);
        // default location
        LatLng defaultLocation = new LatLng(3.1390, 101.6869);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        checkLocationPermissionAndEnableMap();

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

    private void getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                fusedLocationProviderClient.getLastLocation()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location location = task.getResult();
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                            } else {
                                Toast.makeText(getContext(), "Unable to find current location", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } catch (SecurityException e) {
            Log.e("EcoLens", "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }
    private void checkLocationPermissionAndEnableMap() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            getDeviceLocation();
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
        BitmapDescriptor markerIcon = bitmapDescriptorFromVector(requireContext(), R.drawable.ic_recycle_marker);
        for (RecyclingCenter center : matchingCenters) {
            LatLng position = new LatLng(center.getLatitude(), center.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(center.getName())
                    .snippet(center.getAddress())
                    .icon(markerIcon));
        }

        // This part is for the search zoom. It is correct.
        if (matchingCenters.size() == 1 && !currentSearchQuery.isEmpty()) {
            RecyclingCenter singleResult = matchingCenters.get(0);
            LatLng position = new LatLng(singleResult.getLatitude(), singleResult.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        }
    }
    //update filter
    private void setupChipFilters(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.filterChipGroup);

        // select through different chip
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.chip_all) {
                currentFilter = "All";
            } else if (checkedId == R.id.chip_ewaste) {
                currentFilter = "E-Waste";
            } else if (checkedId == R.id.chip_paper) {
                currentFilter = "Paper";
            } else if (checkedId == R.id.chip_fabric) {
                currentFilter = "Fabric";
            } else if (checkedId == R.id.chip_glass) {
                currentFilter = "Glass";
            }

            filterAndShowMarkers();
        });
    }

    //helper method to fix map problem
    private com.google.android.gms.maps.model.BitmapDescriptor bitmapDescriptorFromVector(android.content.Context context, int vectorResId) {
        android.graphics.drawable.Drawable vectorDrawable = androidx.core.content.ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable == null) return null;

        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);

        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        vectorDrawable.draw(canvas);

        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
