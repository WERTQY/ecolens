package com.example.ecolens;

public class RecyclingCenter {
    private String name;
    private double latitude;
    private double longitude;
    private String type;     // e.g., "E-Waste", "Paper", "Glass"
    private String address;

    // 1. Required empty constructor for Firebase
    public RecyclingCenter() {}

    // 2. Normal constructor for your own use
    public RecyclingCenter(String name, double latitude, double longitude, String type, String address) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.address = address;
    }

    // 3. Getters are required for Firebase to read data
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getType() { return type; }
    public String getAddress() { return address; }
}
