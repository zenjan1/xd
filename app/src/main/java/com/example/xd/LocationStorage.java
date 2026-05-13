package com.example.xd;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LocationStorage {
    private static final String PREF_NAME = "mock_locations";
    private static final String KEY_LOCATIONS = "saved_locations";
    private static final String KEY_LAST_LAT = "last_latitude";
    private static final String KEY_LAST_LON = "last_longitude";
    private static final String KEY_LAST_ALT = "last_altitude";
    private static final String KEY_LAST_BEARING = "last_bearing";
    private static final String KEY_LAST_SPEED = "last_speed";
    private static final String KEY_LAST_ACCURACY = "last_accuracy";
    private static final String KEY_TRAJECTORY_ENABLED = "trajectory_enabled";
    private static final String KEY_TRAJECTORY_INTERVAL = "trajectory_interval";
    private static final String KEY_TRAJECTORY_SPEED = "trajectory_speed";

    private SharedPreferences prefs;
    private Gson gson;

    public LocationStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveLocation(MockLocation location) {
        List<MockLocation> locations = getAllLocations();
        location.setId(System.currentTimeMillis());
        locations.add(location);
        String json = gson.toJson(locations);
        prefs.edit().putString(KEY_LOCATIONS, json).apply();
    }

    public void saveLocationWithName(String name, double lat, double lon,
                                     double alt, float bearing, float speed, float accuracy) {
        MockLocation location = new MockLocation(name, lat, lon, alt, bearing, speed, accuracy);
        saveLocation(location);
    }

    public List<MockLocation> getAllLocations() {
        String json = prefs.getString(KEY_LOCATIONS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<MockLocation>>(){}.getType();
        List<MockLocation> locations = gson.fromJson(json, type);
        return locations != null ? locations : new ArrayList<>();
    }

    public void deleteLocation(long id) {
        List<MockLocation> locations = getAllLocations();
        locations.removeIf(loc -> loc.getId() == id);
        String json = gson.toJson(locations);
        prefs.edit().putString(KEY_LOCATIONS, json).apply();
    }

    public void updateLocation(MockLocation location) {
        List<MockLocation> locations = getAllLocations();
        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i).getId() == location.getId()) {
                locations.set(i, location);
                break;
            }
        }
        String json = gson.toJson(locations);
        prefs.edit().putString(KEY_LOCATIONS, json).apply();
    }

    public void saveLastUsedLocation(double lat, double lon, double alt,
                                     float bearing, float speed, float accuracy) {
        prefs.edit()
            .putLong(KEY_LAST_LAT, Double.doubleToRawLongBits(lat))
            .putLong(KEY_LAST_LON, Double.doubleToRawLongBits(lon))
            .putLong(KEY_LAST_ALT, Double.doubleToRawLongBits(alt))
            .putFloat(KEY_LAST_BEARING, bearing)
            .putFloat(KEY_LAST_SPEED, speed)
            .putFloat(KEY_LAST_ACCURACY, accuracy)
            .apply();
    }

    public double[] getLastUsedLocation() {
        double[] result = new double[6];
        result[0] = Double.longBitsToDouble(prefs.getLong(KEY_LAST_LAT,
            Double.doubleToRawLongBits(38.56621628)));
        result[1] = Double.longBitsToDouble(prefs.getLong(KEY_LAST_LON,
            Double.doubleToRawLongBits(113.02837638)));
        result[2] = Double.longBitsToDouble(prefs.getLong(KEY_LAST_ALT,
            Double.doubleToRawLongBits(723.70837402)));
        result[3] = prefs.getFloat(KEY_LAST_BEARING, 0.0f);
        result[4] = prefs.getFloat(KEY_LAST_SPEED, 0.0f);
        result[5] = prefs.getFloat(KEY_LAST_ACCURACY, 4.288f);
        return result;
    }

    public void setTrajectoryEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TRAJECTORY_ENABLED, enabled).apply();
    }

    public boolean isTrajectoryEnabled() {
        return prefs.getBoolean(KEY_TRAJECTORY_ENABLED, false);
    }

    public void setTrajectoryInterval(long intervalMs) {
        prefs.edit().putLong(KEY_TRAJECTORY_INTERVAL, intervalMs).apply();
    }

    public long getTrajectoryInterval() {
        return prefs.getLong(KEY_TRAJECTORY_INTERVAL, 2000);
    }

    public void setTrajectorySpeed(float speed) {
        prefs.edit().putFloat(KEY_TRAJECTORY_SPEED, speed).apply();
    }

    public float getTrajectorySpeed() {
        return prefs.getFloat(KEY_TRAJECTORY_SPEED, 10.0f);
    }
}
