package edd.campuscompass01;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.slideup.SlideUp;
import com.mancj.slideup.SlideUpBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.INTERNET;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static edd.campuscompass01.Constants.ARRAY_ICONS;
import static edd.campuscompass01.Constants.CAMPUS_NAVIGATION;
import static edd.campuscompass01.Constants.DESCRIPTION;
import static edd.campuscompass01.Constants.KEY;
import static edd.campuscompass01.Constants.LATITUDE;
import static edd.campuscompass01.Constants.LONGITUDE;
import static edd.campuscompass01.Constants.NAME;
import static edd.campuscompass01.Constants.PHONE;
import static edd.campuscompass01.Constants.POINTS_OF_INTEREST;
import static edd.campuscompass01.Constants.REQ_GPS;
import static edd.campuscompass01.Constants.TIME;
import static edd.campuscompass01.Constants.VIT;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final double ANGLE_FOV = 15;
    private static final double ROLL_THRESHOLD = 40; // Degree after wich POIs are to be rendered
    private static int currentLimit = 0;

    //Layout Declarations
    private FloatingSearchView fsv;
    private ImageView img_up_down;
    private TextView tv_lat, tv_lon;
    private TextView tv_rot_x, tv_rot_y, tv_rot_z;
    private TextView tv_acc_x, tv_acc_y, tv_acc_z;
    private TextView feedback, about;
    private CardView cv_location, cv_acceleration, cv_rotation;
    private SeekBar sb_limit;
    private ImageView nav_menu;
    private Switch sw_enable_poi;
    private View menu;

    private RelativeLayout relativeLayout;
    private FrameLayout preview;
    private CoordinatorLayout rootView;

    //General Variables
    private boolean isDisplayOn = false;
    private static final int COUNT_LIMIT = 300;
    private static final int DELAY_RENDER_POI = 500;
    private int minDist = 0, maxDist = 1000;
    private int counter_rendering = 0;
    private int prev_x = 100, prev_y = 200;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static boolean isnav = false;

    public static List<PointOfInterest> pointOfInterestList;
    public static List<PointOfInterest> pointOfInterestToRenderList;
    private List<Float> distanceList;
    private List<String> listNames;
    private List<Double> anglesToPOI;

    //Sensors
    private SensorManager sensorManager;
    private Sensor sensorOrientation;
    private Sensor sensorAccelerometer;
    private SensorEventListener eventListenerAccelerometer;
    private SensorEventListener eventListenerOrientation;

    private SlideUp slideUp;
    private View sliderView;

    private double thita, slope, roll = 0;
    private float direction = 0;

    private Location userLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViewByIds();

        anglesToPOI = new ArrayList<>();
        pointOfInterestToRenderList = new ArrayList<>();
        pointOfInterestList = new ArrayList<>();
        distanceList = new ArrayList<>();
        listNames = new ArrayList<>();

        sliderView = findViewById(R.id.slideView);
        sliderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        slideUp = new SlideUpBuilder(sliderView)
                .withStartGravity(Gravity.BOTTOM)
                .withLoggingEnabled(true)
                .withGesturesEnabled(true)
                .withStartState(SlideUp.State.HIDDEN)
                .withSlideFromOtherView(findViewById(R.id.rootView))
                .build();

        sw_enable_poi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    rootView.setVisibility(View.GONE);
                } else {
                    Toast.makeText(MainActivity.this, "Swipe up for more!", Toast.LENGTH_SHORT).show();
                    rootView.setVisibility(View.VISIBLE);
                }
            }
        });

        nav_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isnav) {
                    menu.animate().translationX(0);
                    isnav = true;
                    relativeLayout.addView(menu);
                } else {
                    isnav = false;
                    menu.animate().translationX(-600);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            relativeLayout.removeView(menu);
                        }
                    }, 300);

                }

            }
        });

        feedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), FeedbackActivity.class));
            }
        });

        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), AboutUsActivity.class));
            }
        });

        cv_location.setVisibility(View.INVISIBLE);
        cv_acceleration.setVisibility(View.INVISIBLE);
        cv_rotation.setVisibility(View.INVISIBLE);

        fsv.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {

            }

            @Override
            public void onSearchAction(String currentQuery) {

            }
        });

        //Initialize Camera View
        Camera mCamera = getCameraInstance(this);
        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        preview.addView(mPreview);
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {

            }
        });

        //Get location of user's device
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "In location");

                userLocation = location;

                double latUser = location.getLatitude();
                double lonUser = location.getLongitude();

                String str_lat = "Latitude: " + String.format("%.3f", latUser);
                String str_lon = "Longitude: " + String.format("%.3f", lonUser);
                tv_lat.setText(str_lat);
                tv_lon.setText(str_lon);

                distanceList.clear();
                anglesToPOI.clear();

                Log.d(TAG, "POI: " + String.valueOf(pointOfInterestList.size()));

                for (PointOfInterest poi : pointOfInterestList) {
                    double latDest = Double.parseDouble(poi.getLat());
                    double lonDest = Double.parseDouble(poi.getLon());
                    float[] results = new float[3];
                    Location.distanceBetween(latUser, lonUser, latDest, lonDest, results);
                    distanceList.add(results[0]);
                    Log.d(TAG, "POI: " + poi.getName() + " Distance: " + String.valueOf(results[0]));
                }
                Log.d(TAG, "\n\n");
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
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
                != PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                != PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        INTERNET
                        , ACCESS_COARSE_LOCATION
                        , ACCESS_FINE_LOCATION
                }, REQ_GPS);
            }
            return;
        }
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        //Initialize Firebase
        DatabaseReference mReference = FirebaseDatabase.getInstance().getReference(CAMPUS_NAVIGATION).child(VIT).child(POINTS_OF_INTEREST);
        mReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int i = 0;
                for (DataSnapshot dataPOI : dataSnapshot.getChildren()) {
                    String desc = dataPOI.child(DESCRIPTION).getValue().toString().trim();
                    String key = dataPOI.child(KEY).getValue().toString().trim();
                    String lat = dataPOI.child(LATITUDE).getValue().toString().trim();
                    String lon = dataPOI.child(LONGITUDE).getValue().toString().trim();
                    String name = dataPOI.child(NAME).getValue().toString().trim();
                    String phone = dataPOI.child(PHONE).getValue().toString().trim();
                    String time = dataPOI.child(TIME).getValue().toString().trim();
                    PointOfInterest poi = new PointOfInterest(desc, key, lat, lon, name, phone, time);
                    listNames.add(name);
                    pointOfInterestList.add(poi);
                    distanceList.add(0f);
                }
                Toast.makeText(MainActivity.this, "Done initializing.", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorOrientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }

        eventListenerOrientation = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] values = event.values;
                float x = values[0];
                float y = values[1];
                float z = values[2];

                String str_x = "X: " + String.format("%.3f", x);
                String str_y = "Y: " + String.format("%.3f", y);
                String str_z = "Z: " + String.format("%.3f", z);

                tv_rot_x.setText(str_x);
                tv_rot_y.setText(str_y);
                tv_rot_z.setText(str_z);

                direction = (x + 90) % 360; // float direction
                roll = z;
                int i = 0;
                double userLat = 0, userLon = 0;
                if (userLocation != null) {
                    userLat = userLocation.getLatitude();
                    userLon = userLocation.getLongitude();

                }
                StringBuilder builder = new StringBuilder(); // Store the results

                if (roll > ROLL_THRESHOLD) {
                    for (PointOfInterest pointOfInterest : pointOfInterestList) {
                        double distanceToDestination = distanceList.get(i);

                        if (distanceToDestination < currentLimit) {
                            double destLat = Double.parseDouble(pointOfInterest.getLat());
                            double destLon = Double.parseDouble(pointOfInterest.getLon());
                            Log.d(TAG, "User Location: " + String.valueOf(userLat) + " " + String.valueOf(userLon));
                            Log.d(TAG, "Dest Location: " + String.valueOf(destLat) + " " + String.valueOf(destLon));

                            slope = (Double.valueOf(pointOfInterest.getLon()) - userLon)
                                    / (Double.valueOf(pointOfInterest.getLat()) - userLat);
                            thita = Math.atan(slope);
                            thita *= 180;
                            thita /= 3.14;
                            thita += 360;
                            thita %= 360;

                            if (userLat > destLat && userLon < destLon) {
                                thita = (thita + 180) % 360;
                            }

                            Log.d(TAG, "Your Angle: " + String.valueOf(direction) + " POI: " + pointOfInterest.getName() + " Angle: " + String.valueOf(thita)); // Angle with Magnetic North
                            String output = pointOfInterest.getName() + ": " + (String.valueOf(thita)) + "\n";

                            double low = (direction - ANGLE_FOV + 360) % 360;
                            double high = (direction + ANGLE_FOV + 360) % 360;

                            Log.d(TAG, "Lower Limit: " + String.valueOf(low) + " Upper Limit: " + String.valueOf(high));

                            if ((low > high) && ((low <= thita && thita < 360) || (0 <= thita && thita <= high))) {
                                Log.d(TAG, "Rendered");
                                renderPOI(pointOfInterest.getName(), pointOfInterest.getTim(), String.valueOf(distanceToDestination), Integer.parseInt(pointOfInterest.getKey()));
                                builder.append(output);
                            } else if (thita > low && thita < high) {
                                Log.d(TAG, "Rendered");
                                renderPOI(pointOfInterest.getName(), pointOfInterest.getTim(), String.valueOf(distanceToDestination), Integer.parseInt(pointOfInterest.getKey()));
                                builder.append(output);
                            }
                        }

                    }
                }

                Log.d(TAG, builder.toString() + "\n");
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(eventListenerOrientation, sensorOrientation, SensorManager.SENSOR_DELAY_NORMAL);

        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        eventListenerAccelerometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] values = event.values;
                float x = values[0];
                float y = values[1];
                float z = values[2];

                String str_x = "X: " + String.format("%.3f", x);
                String str_y = "Y: " + String.format("%.3f", y);
                String str_z = "Z: " + String.format("%.3f", z);

                tv_acc_x.setText(str_x);
                tv_acc_y.setText(str_y);
                tv_acc_z.setText(str_z);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(eventListenerAccelerometer, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        sb_limit.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentLimit = minDist + maxDist / 100 * progress;
                Toast.makeText(MainActivity.this, String.valueOf(currentLimit), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_GPS:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(this, "Please grant permission", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void renderPOI(final String name, final String time, String distance, final int image) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        int index, r, c;
        int width_col = width / COLS;
        int width_row = height / ROWS;
        int render_x, render_y;

        if (counter_rendering == COUNT_LIMIT) {
            index = new Random().nextInt() % (ROWS * COLS);
            r = index / COLS;
            c = index % COLS;
            counter_rendering = 0;
            if (r < 1) r = 1;
            if (c > COLS - 2) {
                c = COLS - 2;
            }
            render_x = c * width_col;
            render_y = r * width_row;
            if (render_y == prev_y) {
                r = r + 1;
                c = c + 1;
                render_x = c * width_col;
                render_y = r * width_row;
            }
        } else {
            render_x = prev_x;
            render_y = prev_y;
        }

        counter_rendering++;

        prev_x = render_x;
        prev_y = render_y;

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(render_x, render_y, 0, 0);

        final View poi = getLayoutInflater().inflate(R.layout.layout_poi, null);

        ImageView imageView = poi.findViewById(R.id.iv_poi);
        imageView.setImageResource(ARRAY_ICONS[image]);

        TextView txt_name = poi.findViewById(R.id.txt_poi_name);
        txt_name.setText(name);

        TextView txt_time = poi.findViewById(R.id.txt_time);
        txt_time.setText(time);

        TextView txt_dist = poi.findViewById(R.id.txt_distance);
        String text = distance + "m from you.";
        txt_dist.setText(text);

        poi.setLayoutParams(layoutParams);

        poi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("time", time);
                intent.putExtra("image", String.valueOf(image));
                intent.putExtra("desc", pointOfInterestList.get(image).getDescr());

                startActivity(intent);
            }
        });

        relativeLayout.addView(poi);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                relativeLayout.removeView(poi);
            }
        }, DELAY_RENDER_POI);

    }

    private void findViewByIds() {
        fsv = findViewById(R.id.fsv);
        img_up_down = findViewById(R.id.img_up_down);
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_rot_x = findViewById(R.id.tv_rot_x);
        tv_rot_y = findViewById(R.id.tv_rot_y);
        tv_rot_z = findViewById(R.id.tv_rot_z);
        tv_acc_x = findViewById(R.id.tv_acc_x);
        tv_acc_y = findViewById(R.id.tv_acc_y);
        tv_acc_z = findViewById(R.id.tv_acc_z);
        cv_acceleration = findViewById(R.id.cv_acceleration);
        cv_location = findViewById(R.id.cv_location);
        cv_rotation = findViewById(R.id.cv_rotation);
        sb_limit = findViewById(R.id.sb_limit);
        nav_menu = findViewById(R.id.nav_menu);
        sw_enable_poi = findViewById(R.id.sw_enable_poi);

        preview = findViewById(R.id.camera_preview);
        relativeLayout = findViewById(R.id.relativeLayout);
        rootView = findViewById(R.id.rootView);

        menu = getLayoutInflater().inflate(R.layout.nav, null);
        feedback = menu.findViewById(R.id.feedback);
        about = menu.findViewById(R.id.about_us);
    }

    public static Camera getCameraInstance(Context context) {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return c;
    }

    public void onClick(View view) {
        int id = view.getId();
        PointOfInterest pointOfInterest;
        Intent intent;
        switch (id) {
            case R.id.img_up_down:
                if (!isDisplayOn) {
                    isDisplayOn = true;
                    img_up_down.setImageResource(R.drawable.ic_up_arrow);

                    cv_location.setVisibility(View.VISIBLE);
                    cv_acceleration.setVisibility(View.VISIBLE);
                    cv_rotation.setVisibility(View.VISIBLE);

                    cv_location.animate().translationY(0);
                    cv_acceleration.animate().translationY(350);
                    cv_rotation.animate().translationY(550);

                } else {
                    isDisplayOn = false;
                    img_up_down.setImageResource(R.drawable.ic_down_arrow);

                    cv_location.animate().translationY(0);
                    cv_acceleration.animate().translationY(0);
                    cv_rotation.animate().translationY(0);


                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            cv_location.setVisibility(View.GONE);
                            cv_acceleration.setVisibility(View.GONE);
                            cv_rotation.setVisibility(View.GONE);
                        }
                    }, 360);
                }
                break;
            case R.id.cv_poi_2:
                pointOfInterest = pointOfInterestList.get(0);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_atm:
                pointOfInterest = pointOfInterestList.get(1);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_admission:
                pointOfInterest = pointOfInterestList.get(2);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_boat:
                pointOfInterest = pointOfInterestList.get(3);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;
/////////////////////////////////////////////////////////////////////////////////////////////////////

            case R.id.cv_poi_building1:
                pointOfInterest = pointOfInterestList.get(4);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_building2:
                pointOfInterest = pointOfInterestList.get(5);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_building3:
                pointOfInterest = pointOfInterestList.get(6);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_building4:
                pointOfInterest = pointOfInterestList.get(7);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;
////////////////////////////////////////////////////////////////////////////////////////////////////

            case R.id.cv_poi_campus_comm:
                pointOfInterest = pointOfInterestList.get(8);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_fruit_canteen:
                pointOfInterest = pointOfInterestList.get(9);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_ground:
                pointOfInterest = pointOfInterestList.get(10);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_entry_gate:
                pointOfInterest = pointOfInterestList.get(11);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;
////////////////////////////////////////////////////////////////////////////////////////////////////


            case R.id.cv_poi_lawn:
                pointOfInterest = pointOfInterestList.get(12);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_library:
                pointOfInterest = pointOfInterestList.get(13);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_main_canteen:
                pointOfInterest = pointOfInterestList.get(14);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_nescafe:
                pointOfInterest = pointOfInterestList.get(15);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;
////////////////////////////////////////////////////////////////////////////////////////////////////


            case R.id.cv_poi_out_gate:
                pointOfInterest = pointOfInterestList.get(16);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_reading_hall:
                pointOfInterest = pointOfInterestList.get(17);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_seminar_hall:
                pointOfInterest = pointOfInterestList.get(18);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_sharad_arena:
                pointOfInterest = pointOfInterestList.get(19);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;
////////////////////////////////////////////////////////////////////////////////////////////////////

            case R.id.cv_poi_bank:
                pointOfInterest = pointOfInterestList.get(20);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;

            case R.id.cv_poi_workshop:
                pointOfInterest = pointOfInterestList.get(21);
                intent = new Intent(getApplicationContext(), POIDisplayActivity.class);
                intent.putExtra("name", pointOfInterest.getName());
                intent.putExtra("desc", pointOfInterest.getDescr());
                intent.putExtra("time", pointOfInterest.getTim());
                intent.putExtra("image", pointOfInterest.getKey());
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        menu.animate().translationX(-600);
    }

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mHolder.getSurface() == null) return;

            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}