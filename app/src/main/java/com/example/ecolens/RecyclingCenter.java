package com.example.ecolens;

// No longer need GeoPoint
// import com.google.firebase.firestore.GeoPoint;

public class RecyclingCenter {
    private String name;
    private String type;
    private String address;
    private double latitude;  // Changed from GeoPoint
    private double longitude; // Changed from GeoPoint

    // IMPORTANT: A public no-argument constructor is required for Firestore deserialization
    public RecyclingCenter() {}

    public RecyclingCenter(String name, String type, String address, double latitude, double longitude) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // --- Getters ---
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
