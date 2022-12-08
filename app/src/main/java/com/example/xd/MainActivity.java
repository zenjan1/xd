package com.example.xd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    private TextView tvSystemMockPositionStatus = null;

    private Button btnStartMock = null;

    private Button btnStopMock = null;

    private Button btn_SaveLoc = null;
    private TextView tvProvider = null;

    private TextView tvTime = null;

    private TextView tvLatitude = null;

    private TextView tvLongitude = null;

    private TextView tvAltitude = null;

    private TextView tvBearing = null;

    private TextView tvSpeed = null;

    private TextView tvAccuracy = null;
    private EditText inputLatitude = null;
    private EditText inputLongitude = null;
    /**
     * 动态权限申请
     */

    private void initPermissions(Context context) {
        RequestPermissions(context, "android.permission.ACCESS_FINE_LOCATION");
        RequestPermissions(context, "android.permission.ACCESS_COARSE_LOCATION");
        RequestPermissions(context, "android.permission.ACCESS_MOCK_LOCATION");
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
/**
 * 设置模拟经度纬度
 */
    private double  iLongitude = 113.02837638;
    private double  iLatitude = 38.56621628;
    private double iAltitude = 723.70837402;
    private float  iBearing = 0.0f;
    private float   iSpeed = 0.0f;
    private float  iAccuracy = 4.288F;



    /**
     * 位置管理器

     */

    private LocationManager locationManager = null;

    public LocationManager getLocationManager() {

        return locationManager;

    }

    /**
     * 模拟位置的提供者

     */

    private List mockProviders = null;

    public List getMockProviders() {

        return mockProviders;

    }

    /**
     * 是否成功addTestProvider，默认为true，软件启动时为防止意外退出导致未重置，重置一次
     * Android 6.0系统以下，可以通过Setting.Secure.ALLOW_MOCK_LOCATION获取是否【允许模拟位置】，
     * 当【允许模拟位置】开启时，可addTestProvider；
     * Android 6.0系统及以上，弃用Setting.Secure.ALLOW_MOCK_LOCATION变量，没有【允许模拟位置】选项，
     * 增加【选择模拟位置信息应用】，此时需要选择当前应用，才可以addTestProvider，
     * 但未找到获取当前选择应用的方法，因此通过addTestProvider是否成功来判断是否可用模拟位置。

     */

    private boolean hasAddTestProvider = true;

    /**
     * 启动和停止模拟位置的标识

     */

    private boolean bRun = false;

    @SuppressLint("ResourceType")
    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tvSystemMockPositionStatus = (TextView) findViewById(R.id.tv_system_mock_position_status);

        btnStartMock = (Button) findViewById(R.id.btn_start_mock);

        btnStopMock = (Button) findViewById(R.id.btn_stop_mock);
        btn_SaveLoc = (Button) findViewById(R.id.btn_SaveLoc);

        tvProvider = (TextView) findViewById(R.id.tv_provider);

        tvTime = (TextView) findViewById(R.id.tv_time);

        tvLatitude = (TextView) findViewById(R.id.tv_latitude);

        tvLongitude = (TextView) findViewById(R.id.tv_longitude);

        tvAltitude = (TextView) findViewById(R.id.tv_altitude);

        tvBearing = (TextView) findViewById(R.id.tv_bearing);

        tvSpeed = (TextView) findViewById(R.id.tv_speed);

        tvAccuracy = (TextView) findViewById(R.id.tv_accuracy);


        inputLatitude=(EditText)findViewById(R.id.input_latitude);
        inputLongitude=(EditText)findViewById(R.id.input_longitude);



        btnStartMock.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View view) {

                if (getUseMockPosition()) {


                    /**
                     * 获取输入框内经纬度，如果输入1，则使用默认位置
                     */
                    if (Double.parseDouble(inputLongitude.getText().toString()) ==1) {

                        // 生成 Random 对象
                        Random random = new Random();
                        for (int i = 0; i < 2; i++) {
                            // 生成 0-9 随机整数
                            int number = random.nextInt(10);

                            float v = Float.parseFloat(String.valueOf(number)) / 10000;
                            iLatitude =iLatitude+ v;
                            iLongitude=iLongitude+ v;
                        }
                    }else{
                            iLatitude = Double.parseDouble(inputLatitude.getText().toString());
                            iLongitude = Double.parseDouble(inputLongitude.getText().toString());
                            Toast.makeText(MainActivity.this, inputLatitude.getText().toString(),Toast.LENGTH_LONG).show();


                    }
                    inputLatitude.setText(String.valueOf(iLatitude));
                    inputLongitude.setText(String.valueOf(iLongitude));

                    bRun = true;
                    btnStartMock.setEnabled(false);

                    btnStopMock.setEnabled(true);


                }

            }

        });

        btnStopMock.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View view) {

                bRun = false;

                stopMockLocation();

                btnStartMock.setEnabled(true);

                btnStopMock.setEnabled(false);

            }

        });
        //保存当前位置
        btn_SaveLoc.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View view) {

                //保存到列表


                Location hereLoc=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            }

        });

        initService(this);

      //模拟位置线程
        new Thread(new RunnableMockLocation()).start();
    }

    @Override

    protected void onPostResume() {

        super.onPostResume();

        // 判断系统是否允许模拟位置，并addTestProvider

        if (getUseMockPosition() == false) {

            bRun = false;

            btnStartMock.setEnabled(false);

            btnStopMock.setEnabled(false);

            tvSystemMockPositionStatus.setText("未开启");

        } else {

            if (bRun) {

                btnStartMock.setEnabled(false);

                btnStopMock.setEnabled(true);

            } else {

                btnStartMock.setEnabled(true);

                btnStopMock.setEnabled(false);

            }

            tvSystemMockPositionStatus.setText("已开启");

        }

// 注册位置服务，获取系统位置

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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

        stopMockLocation();

        super.onDestroy();

    }

    /**

     * 初始化服务

     * @param context

     */

    private void initService(Context context) {

/**

 * 模拟位置服务

 */

        mockProviders = new ArrayList<>();

        mockProviders.add(LocationManager.GPS_PROVIDER);

// mockProviders.add(LocationManager.NETWORK_PROVIDER);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

// 防止程序意外终止，没有停止模拟GPS

        stopMockLocation();

    }

    /**

     * 模拟位置是否启用

     * 若启用，则addTestProvider

     */

    public boolean getUseMockPosition() {

// Android 6.0以下，通过Setting.Secure.ALLOW_MOCK_LOCATION判断

// Android 6.0及以上，需要【选择模拟位置信息应用】，未找到方法，因此通过addTestProvider是否可用判断

        boolean canMockPosition = (Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0)

                || Build.VERSION.SDK_INT > 22;

        if (canMockPosition && hasAddTestProvider == false) {

            try {

                for (Object providerStr : mockProviders) {

                    LocationProvider provider = locationManager.getProvider((String) providerStr);

                    if (provider != null) {

                        locationManager.addTestProvider(

                                provider.getName()

                                , provider.requiresNetwork()

                                , provider.requiresSatellite()

                                , provider.requiresCell()

                                , provider.hasMonetaryCost()

                                , provider.supportsAltitude()

                                , provider.supportsSpeed()

                                , provider.supportsBearing()

                                , provider.getPowerRequirement()

                                , provider.getAccuracy());

                    } else {

                        if (providerStr.equals(LocationManager.GPS_PROVIDER)) {

                            locationManager.addTestProvider(

                                    (String) providerStr

                                    , true, true, false, false, true, true, true

                                    , Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);

                        } else if (providerStr.equals(LocationManager.NETWORK_PROVIDER)) {

                            locationManager.addTestProvider(

                                    (String) providerStr

                                    , true, false, true, false, false, false, false

                                    , Criteria.POWER_LOW, Criteria.ACCURACY_FINE);

                        } else {

                            locationManager.addTestProvider(

                                    (String) providerStr

                                    , false, false, false, false, true, true, true

                                    , Criteria.POWER_LOW, Criteria.ACCURACY_FINE);

                        }

                    }

                    locationManager.setTestProviderEnabled((String) providerStr, true);

                    locationManager.setTestProviderStatus((String) providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());

                }

                hasAddTestProvider = true; // 模拟位置可用

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

    /**

     * 取消位置模拟，以免启用模拟数据后无法还原使用系统位置

     * 若模拟位置未开启，则removeTestProvider将会抛出异常；

     * 若已addTestProvider后，关闭模拟位置，未removeTestProvider将导致系统GPS无数据更新；

     */

    public void stopMockLocation() {

        if (hasAddTestProvider) {

            for (Object provider : mockProviders) {

                try {

                    locationManager.removeTestProvider((String) provider);

                } catch (Exception ex) {

// 此处不需要输出日志，若未成功addTestProvider，则必然会出错

// 这里是对于非正常情况的预防措施

                }

            }

            hasAddTestProvider = false;

        }

    }

    /**

     * 模拟位置线程

     */

    private class RunnableMockLocation implements Runnable {

        @Override

        public void run() {

            while (true) {

                try {

                    Thread.sleep(1000);

                    if (hasAddTestProvider == false) {

                        continue;

                    }

                    if (bRun == false) {

                        stopMockLocation();

                        continue;

                    }

                    try {

                       // 模拟位置(addTestProvider成功的前提下)

                        for (Object providerStr : mockProviders) {

                            Location mockLocation = new Location((String) providerStr);

                            mockLocation.setLatitude(iLatitude ); // 纬度(度)

                            mockLocation.setLongitude(iLongitude ); // 经度(度)

                            mockLocation.setAltitude(iAltitude); // 高程(米)

                            mockLocation.setBearing(iBearing); // 方向(度)

                            mockLocation.setSpeed(iSpeed); //速度(米/秒)

                            mockLocation.setAccuracy(iAccuracy); // 精度(米)

                            mockLocation.setTime(new Date().getTime()); // 本地时间

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {

                                mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                            }

                            locationManager.setTestProviderLocation((String) providerStr, mockLocation);

                        }

                    } catch (Exception e) {

// 防止用户在软件运行过程中关闭模拟位置或选择其他应用

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

            try {

                runOnUiThread(new Runnable() {

                    @SuppressLint("SetTextI18n")
                    @Override

                    public void run() {

                        tvProvider.setText(location.getProvider());

                        tvTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(location.getTime())));


                        tvLatitude.setText(location.getLatitude()+"°");

                        tvLongitude.setText(location.getLongitude() +"°");

                        tvAltitude.setText(location.getAltitude() +"m");

                        tvBearing.setText(location.getBearing() +" °");

                        tvSpeed.setText(location.getSpeed() +" m/s");

                        tvAccuracy.setText(location.getAccuracy() +" m");

                    }

                });

            } catch (Exception ex) {

                ex.printStackTrace();

            }

        }

        @Override

        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override

        public void onProviderEnabled(String provider) {

        }

        @Override

        public void onProviderDisabled(String provider) {

        }

    };

}