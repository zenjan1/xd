package com.example.xd;

import java.io.Serializable;

public class MockLocation implements Serializable {
    private static final long serialVersionUID = 1L;
    private long id;
    private String name;
    private double latitude;
    private double longitude;
    private double altitude;
    private float bearing;
    private float speed;
    private float accuracy;
    private long createTime;
    private String description;

    public MockLocation() {
        this.createTime = System.currentTimeMillis();
    }

    public MockLocation(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = 0;
        this.bearing = 0;
        this.speed = 0;
        this.accuracy = 5.0f;
        this.createTime = System.currentTimeMillis();
    }

    public MockLocation(String name, double latitude, double longitude, double altitude,
                       float bearing, float speed, float accuracy) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.bearing = bearing;
        this.speed = speed;
        this.accuracy = accuracy;
        this.createTime = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public float getBearing() { return bearing; }
    public void setBearing(float bearing) { this.bearing = bearing; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return name != null ? name : String.format("%.6f, %.6f", latitude, longitude);
    }

    public String getFormattedLocation() {
        return String.format("%.8f, %.8f", latitude, longitude);
    }
}
