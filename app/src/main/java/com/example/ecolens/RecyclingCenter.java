package com.example.ecolens;

public class RecyclingCenter {
    private String name;
    private double latitude;
    private double longitude;
    private String type;     // e.g., "E-Waste", "Paper", "Glass"
    private String address;

    public RecyclingCenter() {}

    public RecyclingCenter(String name, double latitude, double longitude, String type, String address) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.address = address;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getType() { return type; }
    public String getAddress() { return address; }
}
