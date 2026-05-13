package com.mockgps.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends Activity {
    private static final String TAG = "MockGPS";
    private static final String MOCK_PROVIDER_NAME = "mock_gps_provider";

    private LocationManager locationManager;
    private MockLocationEngine mockEngine;
    private LocationStorage storage;
    private GpxParser gpxParser;
    private NotificationHelper notificationHelper;

    private boolean isMockEnabled = false;
    private boolean isJoystickMode = false;

    private double currentLatitude = 39.9042;
    private double currentLongitude = 116.4074;
    private double currentAltitude = 0;
    private float currentBearing = 0;
    private float currentSpeed = 0;
    private float currentAccuracy = 5;

    private FloatingActionButton fabMain;
    private LinearLayout layoutControls;
    private MaterialCardView cardStatus;
    private MaterialCardView cardJoystick;
    private TextView tvStatus;
    private TextView tvLat;
    private TextView tvLon;
    private TextView tvAlt;
    private TextView tvSpeed;
    private TextView tvBearing;
    private TextView tvAccuracy;
    private TextView tvProvider;
    private TextView tvTime;
    private TextView tvPointIndex;
    private View joystickBase;
    private View joystickKnob;
    private float joystickCenterX, joystickCenterY;

    private final List<LocationPoint> savedLocations = new CopyOnWriteArrayList<>();
    private List<LocationPoint> gpxPoints = new ArrayList<>();
    private int gpxCurrentIndex = 0;
    private long updateIntervalMs = 1000;
    private float moveStepMeters = 10;

    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            runOnUiThread(() -> updateLocationDisplay(location));
        }

        @Override
        public void onProviderEnabled(String provider) {
            runOnUiThread(() -> tvProvider.setText(provider.toUpperCase()));
        }

        @Override
        public void onProviderDisabled(String provider) {
            runOnUiThread(() -> tvProvider.setText("DISABLED"));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initActivityLaunchers();
        initViews();
        initServices();
        setupListeners();
        loadSavedData();
        checkPermissions();
    }

    private void initActivityLaunchers() {
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    checkMockSettings();
                } else {
                    Toast.makeText(this, "需要位置权限才能使用此应用", Toast.LENGTH_LONG).show();
                }
            }
        );

        settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (isMockLocationEnabled()) {
                    enableMockLocation();
                }
            }
        );
    }

    private void initViews() {
        fabMain = findViewById(R.id.fab_main);
        layoutControls = findViewById(R.id.layout_controls);
        cardStatus = findViewById(R.id.card_status);
        cardJoystick = findViewById(R.id.card_joystick);
        tvStatus = findViewById(R.id.tv_status);
        tvLat = findViewById(R.id.tv_lat);
        tvLon = findViewById(R.id.tv_lon);
        tvAlt = findViewById(R.id.tv_alt);
        tvSpeed = findViewById(R.id.tv_speed);
        tvBearing = findViewById(R.id.tv_bearing);
        tvAccuracy = findViewById(R.id.tv_accuracy);
        tvProvider = findViewById(R.id.tv_provider);
        tvTime = findViewById(R.id.tv_time);
        tvPointIndex = findViewById(R.id.tv_point_index);
        joystickBase = findViewById(R.id.joystick_base);
        joystickKnob = findViewById(R.id.joystick_knob);

        cardJoystick.setVisibility(View.GONE);
    }

    private void initServices() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        storage = new LocationStorage(this);
        mockEngine = new MockLocationEngine();
        gpxParser = new GpxParser();
        notificationHelper = new NotificationHelper(this);
    }

    private void setupListeners() {
        findViewById(R.id.btn_start).setOnClickListener(v -> startMocking());
        findViewById(R.id.btn_stop).setOnClickListener(v -> stopMocking());
        findViewById(R.id.btn_save_location).setOnClickListener(v -> showSaveLocationDialog());
        findViewById(R.id.btn_load_location).setOnClickListener(v -> showLoadLocationDialog());
        findViewById(R.id.btn_joystick).setOnClickListener(v -> toggleJoystickMode());
        findViewById(R.id.btn_gpx).setOnClickListener(v -> importGpxFile());
        findViewById(R.id.btn_settings).setOnClickListener(v -> showSettingsDialog());
        findViewById(R.id.btn_random).setOnClickListener(v -> setRandomLocation());

        fabMain.setOnClickListener(v -> {
            if (isMockEnabled) {
                stopMocking();
            } else {
                startMocking();
            }
        });

        setupJoystick();
        setupDragEditText();
    }

    private void setupJoystick() {
        if (joystickBase == null || joystickKnob == null) return;

        joystickBase.post(() -> {
            int[] location = new int[2];
            joystickBase.getLocationOnScreen(location);
            joystickCenterX = location[0] + joystickBase.getWidth() / 2f;
            joystickCenterY = location[1] + joystickBase.getHeight() / 2f;
        });

        joystickKnob.setOnTouchListener((v, event) -> {
            if (!isJoystickMode) return false;

            float maxRadius = joystickBase.getWidth() / 2f - joystickKnob.getWidth() / 2f;

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - joystickCenterX;
                    float dy = event.getRawY() - joystickCenterY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distance > maxRadius) {
                        dx = dx / distance * maxRadius;
                        dy = dy / distance * maxRadius;
                    }

                    joystickKnob.setX(joystickBase.getX() + joystickBase.getWidth() / 2 - joystickKnob.getWidth() / 2 + dx);
                    joystickKnob.setY(joystickBase.getY() + joystickBase.getHeight() / 2 - joystickKnob.getHeight() / 2 + dy);

                    if (distance > 10) {
                        float bearing = (float) Math.toDegrees(Math.atan2(dx, -dy));
                        moveInDirection(bearing, moveStepMeters);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    joystickKnob.setX(joystickBase.getX() + joystickBase.getWidth() / 2 - joystickKnob.getWidth() / 2);
                    joystickKnob.setY(joystickBase.getY() + joystickBase.getHeight() / 2 - joystickKnob.getHeight() / 2);
                    return true;
            }
            return false;
        });
    }

    private void setupDragEditText() {
        TextInputEditText etLat = findViewById(R.id.et_latitude);
        TextInputEditText etLon = findViewById(R.id.et_longitude);

        etLat.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    currentLatitude = Double.parseDouble(etLat.getText().toString());
                    updateLocationFromInputs();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "无效的纬度", Toast.LENGTH_SHORT).show();
                }
            }
        });

        etLon.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    currentLongitude = Double.parseDouble(etLon.getText().toString());
                    updateLocationFromInputs();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "无效的经度", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadSavedData() {
        savedLocations.clear();
        List<LocationPoint> loaded = storage.getSavedLocations();
        savedLocations.addAll(loaded);

        double[] last = storage.getLastLocation();
        if (last != null) {
            currentLatitude = last[0];
            currentLongitude = last[1];
            currentAltitude = last[2];
            currentBearing = (float) last[3];
            currentSpeed = (float) last[4];
            currentAccuracy = (float) last[5];
        }

        updateInputFields();
        updateLocationDisplay(createLocation(currentLatitude, currentLongitude));
    }

    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            checkMockSettings();
        } else {
            permissionLauncher.launch(permissions);
        }
    }

    private void checkMockSettings() {
        if (!isMockLocationEnabled()) {
            new AlertDialog.Builder(this)
                .setTitle("启用模拟位置")
                .setMessage("此应用需要启用模拟位置功能。请在开发者选项中选择此应用作为模拟位置应用。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    settingsLauncher.launch(intent);
                })
                .setNegativeButton("取消", null)
                .show();
        } else {
            enableMockLocation();
        }
    }

    private boolean isMockLocationEnabled() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0;
        }
        return true;
    }

    private void enableMockLocation() {
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {}

        try {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);

            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false, true, true, true,
                Criteria.POWER_HIGH, Criteria.ACCURACY_FINE
            );

            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER,
                LocationProvider.AVAILABLE, null, System.currentTimeMillis());

            isMockEnabled = true;
            tvStatus.setText("就绪");
            tvStatus.setTextColor(getColor(R.color.status_ready));
            fabMain.setImageResource(android.R.drawable.ic_media_play);

            Toast.makeText(this, "Mock GPS 已就绪", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "无法启用模拟位置: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startMocking() {
        if (!isMockLocationEnabled()) {
            checkMockSettings();
            return;
        }

        updateLocationFromInputs();
        mockEngine.start(this::getCurrentLocation);
        isMockEnabled = true;

        tvStatus.setText("模拟中");
        tvStatus.setTextColor(getColor(R.color.status_active));
        fabMain.setImageResource(android.R.drawable.ic_media_pause);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, locationListener, Looper.getMainLooper());
        }

        notificationHelper.showNotification("Mock GPS 正在运行", String.format(Locale.US,
            "位置: %.6f, %.6f", currentLatitude, currentLongitude));
    }

    private void stopMocking() {
        mockEngine.stop();
        isMockEnabled = false;

        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception e) {}

        tvStatus.setText("已停止");
        tvStatus.setTextColor(getColor(R.color.status_stopped));
        fabMain.setImageResource(android.R.drawable.ic_media_play);

        notificationHelper.cancelNotification();
    }

    private Location getCurrentLocation() {
        Location loc = createLocation(currentLatitude, currentLongitude);
        loc.setAltitude(currentAltitude);
        loc.setBearing(currentBearing);
        loc.setSpeed(currentSpeed);
        loc.setAccuracy(currentAccuracy);
        loc.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        return loc;
    }

    private Location createLocation(double lat, double lon) {
        return new Location(LocationManager.GPS_PROVIDER);
    }

    @SuppressLint("SetTextI18n")
    private void updateLocationDisplay(Location location) {
        if (location == null) return;

        tvLat.setText(String.format(Locale.US, "%.8f°", location.getLatitude()));
        tvLon.setText(String.format(Locale.US, "%.8f°", location.getLongitude()));
        tvAlt.setText(String.format(Locale.US, "%.2f m", location.hasAltitude() ? location.getAltitude() : 0));
        tvSpeed.setText(String.format(Locale.US, "%.2f m/s", location.hasSpeed() ? location.getSpeed() : 0));
        tvBearing.setText(String.format(Locale.US, "%.1f°", location.hasBearing() ? location.getBearing() : 0));
        tvAccuracy.setText(String.format(Locale.US, "%.1f m", location.hasAccuracy() ? location.getAccuracy() : 0));
        tvProvider.setText(location.getProvider() != null ? location.getProvider().toUpperCase() : "GPS");
        tvTime.setText(new SimpleDateFormat("HH:mm:ss").format(new Date(location.getTime())));

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        if (!isJoystickMode) {
            updateInputFields();
        }

        storage.saveLastLocation(currentLatitude, currentLongitude, currentAltitude,
            currentBearing, currentSpeed, currentAccuracy);
    }

    private void updateInputFields() {
        TextInputEditText etLat = findViewById(R.id.et_latitude);
        TextInputEditText etLon = findViewById(R.id.et_longitude);
        if (etLat.hasFocus() || etLon.hasFocus()) return;

        etLat.setText(String.format(Locale.US, "%.8f", currentLatitude));
        etLon.setText(String.format(Locale.US, "%.8f", currentLongitude));
    }

    private void updateLocationFromInputs() {
        EditText etLat = findViewById(R.id.et_latitude);
        EditText etLon = findViewById(R.id.et_longitude);

        try {
            currentLatitude = Double.parseDouble(etLat.getText().toString());
            currentLongitude = Double.parseDouble(etLon.getText().toString());

            if (currentLatitude < -90 || currentLatitude > 90) {
                Toast.makeText(this, "纬度应在 -90 到 90 之间", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentLongitude < -180 || currentLongitude > 180) {
                Toast.makeText(this, "经度应在 -180 到 180 之间", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid coordinates", e);
        }
    }

    private void moveInDirection(float bearingDegrees, float distanceMeters) {
        double earthRadius = 6371000;
        double latRad = Math.toRadians(currentLatitude);
        double lonRad = Math.toRadians(currentLongitude);
        double bearingRad = Math.toRadians(bearingDegrees);

        double dLat = (distanceMeters / earthRadius) * Math.cos(bearingRad);
        double dLon = (distanceMeters / (earthRadius * Math.cos(latRad))) * Math.sin(bearingRad);

        currentLatitude = Math.toDegrees(latRad + dLat);
        currentLongitude = Math.toDegrees(lonRad + dLon);
        currentBearing = bearingDegrees;

        if (isMockEnabled) {
            Location mockLoc = getCurrentLocation();
            mockEngine.updateLocation(mockLoc);
            updateLocationDisplay(mockLoc);
        } else {
            Location tempLoc = new Location(LocationManager.GPS_PROVIDER);
            tempLoc.setLatitude(currentLatitude);
            tempLoc.setLongitude(currentLongitude);
            updateLocationDisplay(tempLoc);
        }
    }

    private void toggleJoystickMode() {
        isJoystickMode = !isJoystickMode;
        cardJoystick.setVisibility(isJoystickMode ? View.VISIBLE : View.GONE);

        MaterialButton btnJoystick = findViewById(R.id.btn_joystick);
        btnJoystick.setText(isJoystickMode ? "退出摇杆" : "摇杆控制");
        btnJoystick.setIconResource(isJoystickMode ?
            android.R.drawable.ic_menu_close_clear_cancel :
            android.R.drawable.ic_menu_compass);
    }

    private void showSaveLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存当前位置");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_save, null);
        EditText etName = view.findViewById(R.id.et_location_name);
        TextView tvCoords = view.findViewById(R.id.tv_coords);

        tvCoords.setText(String.format(Locale.US, "%.8f, %.8f", currentLatitude, currentLongitude));
        builder.setView(view);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                name = "位置_" + System.currentTimeMillis();
            }
            LocationPoint point = new LocationPoint(name, currentLatitude, currentLongitude,
                currentAltitude, currentBearing, currentSpeed, currentAccuracy);
            savedLocations.add(point);
            storage.saveLocation(point);
            Toast.makeText(this, "位置已保存", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showLoadLocationDialog() {
        if (savedLocations.isEmpty()) {
            Toast.makeText(this, "没有保存的位置", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = savedLocations.stream()
            .map(p -> p.name + " (" + String.format(Locale.US, "%.6f, %.6f", p.latitude, p.longitude) + ")")
            .toArray(String[]::new);

        new AlertDialog.Builder(this)
            .setTitle("加载位置")
            .setItems(names, (dialog, which) -> {
                LocationPoint point = savedLocations.get(which);
                currentLatitude = point.latitude;
                currentLongitude = point.longitude;
                currentAltitude = point.altitude;
                currentBearing = point.bearing;
                currentSpeed = point.speed;
                currentAccuracy = point.accuracy;

                updateInputFields();
                Location tempLoc = new Location(LocationManager.GPS_PROVIDER);
                tempLoc.setLatitude(currentLatitude);
                tempLoc.setLongitude(currentLongitude);
                updateLocationDisplay(tempLoc);

                if (isMockEnabled) {
                    mockEngine.updateLocation(getCurrentLocation());
                }

                Toast.makeText(this, "已加载: " + point.name, Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("删除", (dialog, which) -> showDeleteLocationDialog())
            .setNegativeButton("取消", null)
            .show();
    }

    private void showDeleteLocationDialog() {
        if (savedLocations.isEmpty()) return;

        String[] names = savedLocations.stream()
            .map(p -> p.name)
            .toArray(String[]::new);

        new AlertDialog.Builder(this)
            .setTitle("删除位置")
            .setItems(names, (dialog, which) -> {
                LocationPoint point = savedLocations.get(which);
                storage.deleteLocation(point.id);
                savedLocations.remove(which);
                Toast.makeText(this, "已删除: " + point.name, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设置");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);

        SeekBar seekInterval = view.findViewById(R.id.seek_interval);
        SeekBar seekStep = view.findViewById(R.id.seek_step);
        TextView tvIntervalVal = view.findViewById(R.id.tv_interval_value);
        TextView tvStepVal = view.findViewById(R.id.tv_step_value);

        seekInterval.setProgress((int) (updateIntervalMs / 100 - 10));
        tvIntervalVal.setText(updateIntervalMs + " ms");
        seekStep.setProgress((int) (moveStepMeters - 1));
        tvStepVal.setText(String.format(Locale.US, "%.0f m", moveStepMeters));

        seekInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateIntervalMs = (progress + 10) * 100L;
                tvIntervalVal.setText(updateIntervalMs + " ms");
                mockEngine.setInterval(updateIntervalMs);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekStep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                moveStepMeters = progress + 1;
                tvStepVal.setText(String.format(Locale.US, "%.0f m", moveStepMeters));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setView(view);
        builder.setPositiveButton("确定", (dialog, which) -> {
            storage.saveSettings(updateIntervalMs, moveStepMeters);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void importGpxFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/gpx+xml", "text/xml", "application/xml"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    reader.close();

                    gpxPoints = gpxParser.parse(content.toString());
                    gpxCurrentIndex = 0;

                    if (!gpxPoints.isEmpty()) {
                        showGpxPlaybackDialog();
                    } else {
                        Toast.makeText(this, "未能解析GPX文件", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "读取文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showGpxPlaybackDialog() {
        new AlertDialog.Builder(this)
            .setTitle("GPX轨迹已加载")
            .setMessage("共 " + gpxPoints.size() + " 个点")
            .setPositiveButton("开始模拟", (dialog, which) -> {
                startGpxPlayback();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void startGpxPlayback() {
        if (gpxPoints.isEmpty()) return;

        gpxCurrentIndex = 0;
        new Thread(() -> {
            while (gpxCurrentIndex < gpxPoints.size() && isMockEnabled) {
                LocationPoint point = gpxPoints.get(gpxCurrentIndex);

                runOnUiThread(() -> {
                    currentLatitude = point.latitude;
                    currentLongitude = point.longitude;
                    currentAltitude = point.altitude;
                    currentBearing = point.bearing;
                    currentSpeed = point.speed;
                    currentAccuracy = point.accuracy;

                    Location mockLoc = getCurrentLocation();
                    mockEngine.updateLocation(mockLoc);
                    updateLocationDisplay(mockLoc);

                    tvPointIndex.setText(String.format(Locale.US, "GPX: %d/%d", gpxCurrentIndex + 1, gpxPoints.size()));
                });

                try {
                    Thread.sleep(updateIntervalMs);
                } catch (InterruptedException e) {
                    break;
                }

                gpxCurrentIndex++;
            }

            runOnUiThread(() -> {
                if (gpxCurrentIndex >= gpxPoints.size()) {
                    Toast.makeText(this, "轨迹播放完成", Toast.LENGTH_SHORT).show();
                    gpxCurrentIndex = 0;
                }
            });
        }).start();
    }

    private void setRandomLocation() {
        Random random = new Random();
        currentLatitude = -90 + random.nextDouble() * 180;
        currentLongitude = -180 + random.nextDouble() * 360;

        Location tempLoc = new Location(LocationManager.GPS_PROVIDER);
        tempLoc.setLatitude(currentLatitude);
        tempLoc.setLongitude(currentLongitude);
        updateLocationDisplay(tempLoc);
        updateInputFields();

        if (isMockEnabled) {
            mockEngine.updateLocation(getCurrentLocation());
        }

        Toast.makeText(this, "已设置随机位置", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception e) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && isMockEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, locationListener, Looper.getMainLooper());
        }
    }

    @Override
    protected void onDestroy() {
        stopMocking();
        super.onDestroy();
    }

    private static class LocationPoint {
        long id;
        String name;
        double latitude;
        double longitude;
        double altitude;
        float bearing;
        float speed;
        float accuracy;

        LocationPoint() {}

        LocationPoint(String name, double lat, double lon, double alt, float bearing, float speed, float accuracy) {
            this.name = name;
            this.latitude = lat;
            this.longitude = lon;
            this.altitude = alt;
            this.bearing = bearing;
            this.speed = speed;
            this.accuracy = accuracy;
        }
    }

    private class MockLocationEngine {
        private Thread mockThread;
        private volatile boolean isRunning = false;
        private long intervalMs = 1000;
        private Location currentLocation;
        private final Object lock = new Object();

        void start(java.util.function.Supplier<Location> locationSupplier) {
            stop();
            isRunning = true;

            mockThread = new Thread(() -> {
                while (isRunning) {
                    try {
                        Location loc;
                        synchronized (lock) {
                            loc = currentLocation != null ? new Location(currentLocation) : locationSupplier.get();
                        }

                        if (loc != null) {
                            try {
                                locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
                            } catch (SecurityException e) {
                                Log.e(TAG, "Failed to set mock location", e);
                            }
                        }

                        Thread.sleep(intervalMs);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Mock location error", e);
                    }
                }
            });
            mockThread.start();
        }

        void stop() {
            isRunning = false;
            if (mockThread != null) {
                mockThread.interrupt();
                try {
                    mockThread.join(1000);
                } catch (InterruptedException e) {}
            }
        }

        void updateLocation(Location location) {
            synchronized (lock) {
                this.currentLocation = location;
            }
        }

        void setInterval(long intervalMs) {
            this.intervalMs = intervalMs;
        }
    }

    private class GpxParser {
        List<LocationPoint> parse(String content) {
            List<LocationPoint> points = new ArrayList<>();

            try {
                String[] lines = content.split("\n");
                LocationPoint current = null;

                for (String line : lines) {
                    line = line.trim();

                    if (line.contains("<trkpt")) {
                        current = new LocationPoint();
                        String latStr = extractAttribute(line, "lat");
                        String lonStr = extractAttribute(line, "lon");
                        if (latStr != null && lonStr != null) {
                            current.latitude = Double.parseDouble(latStr);
                            current.longitude = Double.parseDouble(lonStr);
                        }
                    } else if (line.contains("<ele") && current != null) {
                        current.altitude = Double.parseDouble(extractText(line, "ele"));
                    } else if (line.contains("</trkpt>") && current != null) {
                        points.add(current);
                        current = null;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "GPX parse error", e);
            }

            return points;
        }

        private String extractAttribute(String xml, String attr) {
            String marker = attr + "=\"";
            int start = xml.indexOf(marker);
            if (start == -1) return null;
            start += marker.length();
            int end = xml.indexOf("\"", start);
            return end > start ? xml.substring(start, end) : null;
        }

        private String extractText(String xml, String tag) {
            int start = xml.indexOf("<" + tag + ">") + tag.length() + 2;
            int end = xml.indexOf("</" + tag + ">");
            return start > 0 && end > start ? xml.substring(start, end) : "0";
        }
    }

    private class LocationStorage {
        private static final String PREF_NAME = "mock_gps_prefs";
        private static final String KEY_LOCATIONS = "saved_locations";
        private static final String KEY_LAST_LAT = "last_lat";
        private static final String KEY_LAST_LON = "last_lon";
        private static final String KEY_LAST_ALT = "last_alt";
        private static final String KEY_LAST_BEARING = "last_bearing";
        private static final String KEY_LAST_SPEED = "last_speed";
        private static final String KEY_LAST_ACCURACY = "last_accuracy";
        private static final String KEY_INTERVAL = "update_interval";
        private static final String KEY_STEP = "move_step";

        private final android.content.SharedPreferences prefs;
        private final Gson gson = new Gson();

        LocationStorage(Context context) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            updateIntervalMs = prefs.getLong(KEY_INTERVAL, 1000);
            moveStepMeters = prefs.getFloat(KEY_STEP, 10);
        }

        void saveLocation(LocationPoint point) {
            List<LocationPoint> locations = getSavedLocations();
            point.id = System.currentTimeMillis();
            locations.add(point);
            String json = gson.toJson(locations);
            prefs.edit().putString(KEY_LOCATIONS, json).apply();
        }

        List<LocationPoint> getSavedLocations() {
            String json = prefs.getString(KEY_LOCATIONS, "[]");
            Type type = new TypeToken<List<LocationPoint>>(){}.getType();
            List<LocationPoint> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        }

        void deleteLocation(long id) {
            List<LocationPoint> locations = getSavedLocations();
            locations.removeIf(p -> p.id == id);
            String json = gson.toJson(locations);
            prefs.edit().putString(KEY_LOCATIONS, json).apply();
        }

        void saveLastLocation(double lat, double lon, double alt, float bearing, float speed, float accuracy) {
            prefs.edit()
                .putLong(KEY_LAST_LAT, Double.doubleToRawLongBits(lat))
                .putLong(KEY_LAST_LON, Double.doubleToRawLongBits(lon))
                .putLong(KEY_LAST_ALT, Double.doubleToRawLongBits(alt))
                .putFloat(KEY_LAST_BEARING, bearing)
                .putFloat(KEY_LAST_SPEED, speed)
                .putFloat(KEY_LAST_ACCURACY, accuracy)
                .apply();
        }

        double[] getLastLocation() {
            double[] result = new double[6];
            result[0] = Double.longBitsToDouble(prefs.getLong(KEY_LAST_LAT, Double.doubleToRawLongBits(39.9042)));
            result[1] = Double.longBitsToDouble(prefs.getLong(KEY_LAST_LON, Double.doubleToRawLongBits(116.4074)));
            result[2] = Double.longBitsToDouble(prefs.getLong(KEY_LAST_ALT, 0));
            result[3] = prefs.getFloat(KEY_LAST_BEARING, 0);
            result[4] = prefs.getFloat(KEY_LAST_SPEED, 0);
            result[5] = prefs.getFloat(KEY_LAST_ACCURACY, 5);
            return result;
        }

        void saveSettings(long interval, float step) {
            prefs.edit()
                .putLong(KEY_INTERVAL, interval)
                .putFloat(KEY_STEP, step)
                .apply();
            updateIntervalMs = interval;
            moveStepMeters = step;
        }
    }
}
