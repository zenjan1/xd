package com.example.xd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final String TAG = "MockLocationApp";
    private LocationStorage locationStorage;
    private TrajectorySimulator trajectorySimulator;
    private Handler trajectoryHandler;

    private TextView tvSystemMockPositionStatus;
    private Button btnStartMock;
    private Button btnStopMock;
    private Button btnSaveLoc;
    private Button btnManageLocations;
    private Button btnTrajectory;
    private TextView tvProvider;
    private TextView tvTime;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAltitude;
    private TextView tvBearing;
    private TextView tvSpeed;
    private TextView tvAccuracy;
    private EditText inputLatitude;
    private EditText inputLongitude;
    private TextView tvTrajectoryStatus;
    private TextView tvPointCount;

    private double iLongitude = 113.02837638;
    private double iLatitude = 38.56621628;
    private double iAltitude = 723.70837402;
    private float iBearing = 0.0f;
    private float iSpeed = 0.0f;
    private float iAccuracy = 4.288f;

    private LocationManager locationManager;
    private List mockProviders;
    private boolean hasAddTestProvider = true;
    private boolean bRun = false;
    private boolean trajectoryMode = false;
    private long trajectoryInterval = 2000;
    private float trajectorySpeed = 10.0f;
    private float trajectoryProgress = 0f;

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public List getMockProviders() {
        return mockProviders;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationStorage = new LocationStorage(this);
        trajectorySimulator = new TrajectorySimulator();
        trajectoryHandler = new Handler();

        initViews();
        loadLastLocation();
        setupListeners();
        initService(this);
        initPermissions(this);
        new Thread(new RunnableMockLocation()).start();
    }

    private void initViews() {
        tvSystemMockPositionStatus = findViewById(R.id.tv_system_mock_position_status);
        btnStartMock = findViewById(R.id.btn_start_mock);
        btnStopMock = findViewById(R.id.btn_stop_mock);
        btnSaveLoc = findViewById(R.id.btn_SaveLoc);
        btnManageLocations = findViewById(R.id.btn_manage_locations);
        btnTrajectory = findViewById(R.id.btn_trajectory);
        tvProvider = findViewById(R.id.tv_provider);
        tvTime = findViewById(R.id.tv_time);
        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
        tvAltitude = findViewById(R.id.tv_altitude);
        tvBearing = findViewById(R.id.tv_bearing);
        tvSpeed = findViewById(R.id.tv_speed);
        tvAccuracy = findViewById(R.id.tv_accuracy);
        inputLatitude = findViewById(R.id.input_latitude);
        inputLongitude = findViewById(R.id.input_longitude);
        tvTrajectoryStatus = findViewById(R.id.tv_trajectory_status);
        tvPointCount = findViewById(R.id.tv_point_count);
    }

    private void loadLastLocation() {
        double[] lastLoc = locationStorage.getLastUsedLocation();
        iLatitude = lastLoc[0];
        iLongitude = lastLoc[1];
        iAltitude = lastLoc[2];
        iBearing = (float) lastLoc[3];
        iSpeed = (float) lastLoc[4];
        iAccuracy = (float) lastLoc[5];
        inputLatitude.setText(String.valueOf(iLatitude));
        inputLongitude.setText(String.valueOf(iLongitude));
    }

    private void saveCurrentLocation() {
        locationStorage.saveLastUsedLocation(iLatitude, iLongitude, iAltitude, iBearing, iSpeed, iAccuracy);
    }

    @SuppressLint("SetTextI18n")
    private void setupListeners() {
        btnStartMock.setOnClickListener(v -> {
            if (getUseMockPosition()) {
                parseInputLocation();
                bRun = true;
                trajectoryMode = false;
                saveCurrentLocation();
                btnStartMock.setEnabled(false);
                btnStopMock.setEnabled(true);
                tvTrajectoryStatus.setText(R.string.single_point_mode);
            }
        });

        btnStopMock.setOnClickListener(v -> {
            bRun = false;
            trajectoryMode = false;
            stopMockLocation();
            btnStartMock.setEnabled(true);
            btnStopMock.setEnabled(false);
            tvTrajectoryStatus.setText(R.string.status_stopped);
        });

        btnSaveLoc.setOnClickListener(v -> showSaveLocationDialog());

        btnManageLocations.setOnClickListener(v -> showManageLocationsDialog());

        btnTrajectory.setOnClickListener(v -> showTrajectoryDialog());
    }

    private void parseInputLocation() {
        try {
            String latStr = inputLongitude.getText().toString().trim();
            String lonStr = inputLatitude.getText().toString().trim();

            if (latStr.equals("1") || lonStr.equals("1")) {
                Random random = new Random();
                for (int i = 0; i < 2; i++) {
                    int number = random.nextInt(10);
                    float v = Float.parseFloat(String.valueOf(number)) / 10000;
                    iLatitude += v;
                    iLongitude += v;
                }
            } else {
                iLatitude = Double.parseDouble(latStr);
                iLongitude = Double.parseDouble(lonStr);
            }

            inputLatitude.setText(String.valueOf(iLatitude));
            inputLongitude.setText(String.valueOf(iLongitude));

            if (trajectoryMode && trajectorySimulator.getPointCount() > 0) {
                TrajectorySimulator.TrajectoryPoint point = trajectorySimulator.getCurrentPoint();
                if (point != null) {
                    iLatitude = point.latitude;
                    iLongitude = point.longitude;
                    iAltitude = point.altitude;
                    iBearing = point.bearing;
                    iSpeed = point.speed;
                    iAccuracy = point.accuracy;
                }
            }

            Toast.makeText(this, getString(R.string.location_updated), Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void showSaveLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save_location);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_location, null);
        EditText nameInput = dialogView.findViewById(R.id.input_location_name);
        TextView coordsText = dialogView.findViewById(R.id.tv_coordinates);
        coordsText.setText(String.format(Locale.getDefault(),
            getString(R.string.coordinates_format), iLatitude, iLongitude));

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                name = getString(R.string.location) + "_" + System.currentTimeMillis();
            }
            locationStorage.saveLocationWithName(name, iLatitude, iLongitude,
                iAltitude, iBearing, iSpeed, iAccuracy);
            Toast.makeText(this, getString(R.string.location_saved), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @SuppressLint("SetTextI18n")
    private void showManageLocationsDialog() {
        List<MockLocation> locations = locationStorage.getAllLocations();

        if (locations.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_saved_locations), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.manage_locations);

        String[] names = new String[locations.size()];
        for (int i = 0; i < locations.size(); i++) {
            MockLocation loc = locations.get(i);
            names[i] = loc.getName() + "\n" + loc.getFormattedLocation();
        }

        builder.setItems(names, (dialog, which) -> {
            MockLocation selected = locations.get(which);
            iLatitude = selected.getLatitude();
            iLongitude = selected.getLongitude();
            iAltitude = selected.getAltitude();
            iBearing = selected.getBearing();
            iSpeed = selected.getSpeed();
            iAccuracy = selected.getAccuracy();
            inputLatitude.setText(String.valueOf(iLatitude));
            inputLongitude.setText(String.valueOf(iLongitude));
            Toast.makeText(this, getString(R.string.location_loaded), Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton(R.string.delete, (dialog, which) -> {
            showDeleteLocationDialog(locations);
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showDeleteLocationDialog(List<MockLocation> locations) {
        String[] names = new String[locations.size()];
        final List<Long> ids = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            MockLocation loc = locations.get(i);
            names[i] = loc.getName();
            ids.add(loc.getId());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_location);
        builder.setItems(names, (dialog, which) -> {
            locationStorage.deleteLocation(ids.get(which));
            Toast.makeText(this, getString(R.string.location_deleted), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @SuppressLint("SetTextI18n")
    private void showTrajectoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.trajectory_mode);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trajectory, null);

        Spinner trajectoryType = dialogView.findViewById(R.id.spinner_trajectory_type);
        SeekBar seekInterval = dialogView.findViewById(R.id.seek_interval);
        SeekBar seekSpeed = dialogView.findViewById(R.id.seek_speed);
        TextView tvInterval = dialogView.findViewById(R.id.tv_interval_value);
        TextView tvSpeed = dialogView.findViewById(R.id.tv_speed_value);
        EditText inputCenterLat = dialogView.findViewById(R.id.input_center_lat);
        EditText inputCenterLon = dialogView.findViewById(R.id.input_center_lon);
        EditText inputRadius = dialogView.findViewById(R.id.input_radius);

        inputCenterLat.setText(String.valueOf(iLatitude));
        inputCenterLon.setText(String.valueOf(iLongitude));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.trajectory_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trajectoryType.setAdapter(adapter);

        seekInterval.setMax(90);
        seekInterval.setProgress((int) ((trajectoryInterval - 1000) / 100));
        tvInterval.setText(trajectoryInterval + "ms");

        seekSpeed.setMax(90);
        seekSpeed.setProgress((int) (trajectorySpeed - 1));
        tvSpeed.setText(String.format(Locale.getDefault(), "%.1f m/s", trajectorySpeed));

        seekInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                trajectoryInterval = 1000 + progress * 100L;
                tvInterval.setText(trajectoryInterval + "ms");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                trajectorySpeed = 1.0f + progress;
                tvSpeed.setText(String.format(Locale.getDefault(), "%.1f m/s", trajectorySpeed));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.start_trajectory, (dialog, which) -> {
            try {
                int type = trajectoryType.getSelectedItemPosition();
                double centerLat = Double.parseDouble(inputCenterLat.getText().toString());
                double centerLon = Double.parseDouble(inputCenterLon.getText().toString());
                double radius = Double.parseDouble(inputRadius.getText().toString());

                switch (type) {
                    case 0:
                        trajectorySimulator.generateCirclePath(centerLat, centerLon, radius, 36);
                        break;
                    case 1:
                        trajectorySimulator.generateRectanglePath(centerLat, centerLon, radius, radius, 10);
                        break;
                    case 2:
                        trajectorySimulator.generateLinePath(centerLat, centerLon,
                            centerLat + 0.01, centerLon + 0.01, 20);
                        break;
                }

                trajectorySimulator.start();
                trajectoryMode = true;
                bRun = true;
                btnStartMock.setEnabled(false);
                btnStopMock.setEnabled(true);

                locationStorage.setTrajectoryInterval(trajectoryInterval);
                locationStorage.setTrajectorySpeed(trajectorySpeed);

                tvTrajectoryStatus.setText(R.string.trajectory_running);
                tvPointCount.setText(getString(R.string.point_count, trajectorySimulator.getPointCount()));

                Toast.makeText(this, getString(R.string.trajectory_started), Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(this, getString(R.string.invalid_coordinates), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void initPermissions(Context context) {
        RequestPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION);
        RequestPermissions(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        RequestPermissions(context, Manifest.permission.ACCESS_MOCK_LOCATION);
    }

    public static boolean RequestPermissions(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.i("requestMyPermissions", ": 【 " + permission + " 】没有授权，申请权限");
            ActivityCompat.requestPermissions((Activity) context, new String[]{permission}, 100);
            return false;
        } else {
            Log.i("requestMyPermissions", ": 【 " + permission + " 】有权限");
            return true;
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (getUseMockPosition() == false) {
            bRun = false;
            btnStartMock.setEnabled(false);
            btnStopMock.setEnabled(false);
            tvSystemMockPositionStatus.setText(R.string.status_disabled);
        } else {
            btnStartMock.setEnabled(!bRun);
            btnStopMock.setEnabled(bRun);
            tvSystemMockPositionStatus.setText(R.string.status_enabled);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onPause() {
        locationManager.removeUpdates(locationListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        bRun = false;
        trajectoryMode = false;
        saveCurrentLocation();
        stopMockLocation();
        super.onDestroy();
    }

    private void initService(Context context) {
        mockProviders = new ArrayList<>();
        mockProviders.add(LocationManager.GPS_PROVIDER);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        stopMockLocation();
    }

    public boolean getUseMockPosition() {
        boolean canMockPosition = (Settings.Secure.getInt(getContentResolver(),
            Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0)
            || Build.VERSION.SDK_INT > 22;

        if (canMockPosition && hasAddTestProvider == false) {
            try {
                for (Object providerStr : mockProviders) {
                    LocationProvider provider = locationManager.getProvider((String) providerStr);
                    if (provider != null) {
                        locationManager.addTestProvider(
                            provider.getName(),
                            provider.requiresNetwork(),
                            provider.requiresSatellite(),
                            provider.requiresCell(),
                            provider.hasMonetaryCost(),
                            provider.supportsAltitude(),
                            provider.supportsSpeed(),
                            provider.supportsBearing(),
                            provider.getPowerRequirement(),
                            provider.getAccuracy());
                    } else {
                        if (providerStr.equals(LocationManager.GPS_PROVIDER)) {
                            locationManager.addTestProvider(
                                (String) providerStr,
                                true, true, false, false, true, true, true,
                                Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
                        } else if (providerStr.equals(LocationManager.NETWORK_PROVIDER)) {
                            locationManager.addTestProvider(
                                (String) providerStr,
                                true, false, true, false, false, false, false,
                                Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
                        } else {
                            locationManager.addTestProvider(
                                (String) providerStr,
                                false, false, false, false, true, true, true,
                                Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
                        }
                    }
                    locationManager.setTestProviderEnabled((String) providerStr, true);
                    locationManager.setTestProviderStatus((String) providerStr,
                        LocationProvider.AVAILABLE, null, System.currentTimeMillis());
                }
                hasAddTestProvider = true;
                canMockPosition = true;
            } catch (SecurityException e) {
                canMockPosition = false;
            }
        }

        if (canMockPosition == false) {
            stopMockLocation();
        }
        return canMockPosition;
    }

    public void stopMockLocation() {
        if (hasAddTestProvider) {
            for (Object provider : mockProviders) {
                try {
                    locationManager.removeTestProvider((String) provider);
                } catch (Exception ex) {
                }
            }
            hasAddTestProvider = false;
        }
    }

    private class RunnableMockLocation implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(trajectoryInterval);
                    if (hasAddTestProvider == false) {
                        continue;
                    }
                    if (bRun == false) {
                        stopMockLocation();
                        continue;
                    }

                    if (trajectoryMode && trajectorySimulator.hasMorePoints()) {
                        TrajectorySimulator.TrajectoryPoint point = trajectorySimulator.getNextPoint();
                        if (point != null) {
                            iLatitude = point.latitude;
                            iLongitude = point.longitude;
                            iAltitude = point.altitude;
                            iBearing = point.bearing;
                            iSpeed = point.speed;
                            iAccuracy = point.accuracy;
                        }
                        runOnUiThread(() -> {
                            tvPointCount.setText(getString(R.string.point_progress,
                                trajectorySimulator.getCurrentIndex() + 1,
                                trajectorySimulator.getPointCount()));
                        });
                    } else if (trajectoryMode && !trajectorySimulator.hasMorePoints()) {
                        trajectorySimulator.reset();
                    }

                    try {
                        for (Object providerStr : mockProviders) {
                            Location mockLocation = new Location((String) providerStr);
                            mockLocation.setLatitude(iLatitude);
                            mockLocation.setLongitude(iLongitude);
                            mockLocation.setAltitude(iAltitude);
                            mockLocation.setBearing(iBearing);
                            mockLocation.setSpeed(iSpeed);
                            mockLocation.setAccuracy(iAccuracy);
                            mockLocation.setTime(new Date().getTime());

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                            }
                            locationManager.setTestProviderLocation((String) providerStr, mockLocation);
                        }
                    } catch (Exception e) {
                        stopMockLocation();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            runOnUiThread(() -> {
                tvProvider.setText(location.getProvider());
                tvTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(location.getTime())));
                tvLatitude.setText(String.format(Locale.getDefault(), "%.8f°", location.getLatitude()));
                tvLongitude.setText(String.format(Locale.getDefault(), "%.8f°", location.getLongitude()));
                tvAltitude.setText(String.format(Locale.getDefault(), "%.2f m", location.getAltitude()));
                tvBearing.setText(String.format(Locale.getDefault(), "%.1f°", location.getBearing()));
                tvSpeed.setText(String.format(Locale.getDefault(), "%.2f m/s", location.getSpeed()));
                tvAccuracy.setText(String.format(Locale.getDefault(), "%.1f m", location.getAccuracy()));
            });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };
}
