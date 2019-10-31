package com.cs3370.android.lrs_driverapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.cs3370.android.lrs_driverapp.MapsActivity.TaskParser.mMap;
import static com.cs3370.android.lrs_driverapp.MapsActivity.getRequestUrl;
import static com.cs3370.android.lrs_driverapp.SignInActivity.mDriver;


public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback, TaskLoadedCallback {

    public String TAG = "MapsActivityClient";

    private Location mCurrentLocation;
    private String estimatedTime;

    //Variables to get my current location
    private Polyline currentPolyline;
    private Boolean mLocationPermissionsGranted = false;
    private ImageButton mGps, mOpenGoogleMap;
    private TextView textView_title;
    private TextView textView_time;
    private TextView textView_distance;
    private TextView textView_destination;
    private Button mButtonArrivedPickUp;
    private Button mButtonArrivedDropOff;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private RequestQueue mRequestQueue;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    SQLiteDatabase myDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.driver_map);
        mapFragment.getMapAsync(this);


        //************* DataBase for DriverMapsActivity *****************
        myDatabase = new DBHelper(DriverMapsActivity.this).getWritableDatabase();
        addDataInDatabase(myDatabase);
        //************************************************************

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        //Get permission to use our location
        getLocationPermission();

        mGps = (ImageButton) findViewById(R.id.driver_gps);
        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked gps icon");
                //Position the map on our current location
                getDeviceLocation();
            }
        });

        //Initialize Places
        Places.initialize(getApplicationContext(),
                "AIzaSyAefQArjnf5orqkbWJj7aXTzAxES_YVErM");

        // Create location request
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        // Create location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (DataHolder.isConnected(DriverMapsActivity.this)) {
                    if (locationResult != null) {
                        for (Location location : locationResult.getLocations()) {
                            updateMap(location);
                        }
                    }
                    if (!DataHolder.isClientPickedUp) {
                        addMarker(DataHolder.mPickUp_latlng, "Pick up location");
                        addPolyline(DataHolder.mCurrent_latLng, DataHolder.mPickUp_latlng);
                    } else {
                        addMarker(DataHolder.mDropOff_latlng, "Drop off location");
                        addPolyline(DataHolder.mCurrent_latLng, DataHolder.mDropOff_latlng);
                    }
                    moveCamera(DataHolder.mCurrent_latLng, DEFAULT_ZOOM, "My Location");
                }
                else
                    toastMessage("No internet connection");
            }
        };

        //******************** Get Info From MapsActivity *************************************

        textView_title = (TextView) findViewById(R.id.textView_name);
        textView_destination = (TextView) findViewById(R.id.textView_destination);
        textView_time = (TextView) findViewById(R.id.textView_time);
        textView_distance = (TextView) findViewById(R.id.textView_distance);

        //***************************** button pick up and drop off ****************************
        mButtonArrivedPickUp = (Button) findViewById(R.id.btn_arrived_pickUp);
        mButtonArrivedPickUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Hide button
                mButtonArrivedPickUp.setVisibility(View.GONE);
                //Display fragment
                showFragmentArrived();

            }
        });

        mButtonArrivedDropOff = (Button) findViewById(R.id.btn_arrived_dropOff);
        mButtonArrivedDropOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create the object of AlertDialog Builder class
                AlertDialog.Builder alertDialogBuilder = new AlertDialog
                        .Builder(DriverMapsActivity.this);
                //Set the message to show for the alert time
                alertDialogBuilder.setMessage("Confirm drop off.");
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder.setTitle("Drop off!");
                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //************* DataBase for DriverMapsActivity *****************
                        deleteAllRows(myDatabase);
                        //************************************************************
                        DataHolder.mPickUpAddress = "";
                        DataHolder.mDropOffAddress = "";
                        /*
                         * Save trip to the history
                         * Rate passenger
                         * Set variables on DataHolder to null
                         */
                        finishRide();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        mOpenGoogleMap = (ImageButton) findViewById(R.id.imageView_openGoogleMap);
        mOpenGoogleMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!DataHolder.isClientPickedUp) {
                    String uri = "http://maps.google.com/maps?saddr=" + DataHolder.mCurrent_latLng.latitude + "," + DataHolder.mCurrent_latLng.longitude +
                            "&daddr=" + DataHolder.mPickUp_latlng.latitude + "," + DataHolder.mPickUp_latlng.longitude;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                }
                else {
                    String uri = "http://maps.google.com/maps?saddr=" + DataHolder.mCurrent_latLng.latitude + "," + DataHolder.mCurrent_latLng.longitude +
                            "&daddr=" + DataHolder.mDropOff_latlng.latitude + "," + DataHolder.mDropOff_latlng.longitude;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                }
            }
        });

        if (!DataHolder.isClientPickedUp) {
            textView_title.setText("Picking up " + DataHolder.clientName);
            textView_destination.setText("At " + DataHolder.mPickUpAddress);
            mButtonArrivedPickUp.setVisibility(View.VISIBLE);
            mButtonArrivedDropOff.setVisibility(View.INVISIBLE);

        } else {
            textView_title.setText("Dropping off " + DataHolder.clientName);
            textView_destination.setText("At " + DataHolder.mDropOffAddress);
            mButtonArrivedDropOff.setVisibility(View.VISIBLE);
            mButtonArrivedPickUp.setVisibility(View.INVISIBLE);
        }
    }

    private void updateMap(Location location) {
        // Get current location
        DataHolder.mCurrent_latLng = new LatLng(location.getLatitude(),
                location.getLongitude());

        //Compute the distance between current position and pick up position
        DataHolder.distanceToPickUp = getDistance(DataHolder.mCurrent_latLng, DataHolder.mPickUp_latlng);
        DataHolder.distanceToDropOff = getDistance(DataHolder.mCurrent_latLng, DataHolder.mDropOff_latlng);

        if (isConnected()) {
            if (!DataHolder.isClientPickedUp) {
                textView_distance.setText("Distance: " + DataHolder.distanceToPickUp + "miles.");
            } else {
                textView_distance.setText("Distance: " + DataHolder.distanceToDropOff + "miles.");
            }

            if (estimatedTime != null)
                textView_time.setText("Arrival time: " + estimatedTime);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        if (mLocationPermissionsGranted) {
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataHolder.dismissProgressDialog();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    public static String getDistance(LatLng latLng1, LatLng latLng2) {
        if (latLng1 == null || latLng2 == null)
            return null;
        float[] result = new float[1];
        Location.distanceBetween(latLng1.latitude, latLng1.longitude,
                latLng2.latitude, latLng2.longitude, result);

        double distance = result[0] * 0.000621371;

        return String.format("%.2f", distance);
    }

    public void toastMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void addMarker(LatLng latLng, String title) {
        MarkerOptions marker = new MarkerOptions();
        marker.position(latLng);
        marker.title(title);
        mMap.addMarker(marker).showInfoWindow();
    }

    public void addPolyline(LatLng latLng1, LatLng latLng2) {
        if (isConnected()) {
            String urlToPickup = getRequestUrl(latLng1, latLng2);
            MapsActivity.TaskRequestDirections taskRequestDirections1 = new MapsActivity.TaskRequestDirections();
            taskRequestDirections1.execute(urlToPickup);
            //Draw Polyline
            /*
            new FetchURL(DriverMapsActivity.this)
                    .execute(getUrl(latLng1, latLng2, "driving"), "driving");

             */
        } else
            toastMessage("No internet connection!");
    }

    private String getAddress(double latitude, double longitude) {
        StringBuilder result = new StringBuilder();
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                result.append(address.getAddressLine(0));
            }
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }

        return result.toString();
    }

    @Override
    public void onTaskDone(String time, Object... values) {
        if (isConnected()) {
            currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
            estimatedTime = time;
        } else
            toastMessage("No internet connection!");
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        hideSoftKeyboard();
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            //Get Location
                            mCurrentLocation = (Location) task.getResult();
                            //Get LatLng
                            DataHolder.mCurrent_latLng = (new LatLng(mCurrentLocation.getLatitude(),
                                    mCurrentLocation.getLongitude()));
                            //Get address name
                            DataHolder.mCurrentAddress = getAddress(DataHolder.mCurrent_latLng.latitude,
                                    DataHolder.mCurrent_latLng.longitude);

                            moveCamera(DataHolder.mCurrent_latLng, DEFAULT_ZOOM, "My Location");
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            toastMessage("unable to get current location");
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    public void showFragmentArrived() {
        ArrivedAtLocationFragment arrivedAtLocationFragment = new ArrivedAtLocationFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment_arrived, arrivedAtLocationFragment);
        fragmentTransaction.commit();
    }

    public boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = cm.getActiveNetworkInfo();
            connected = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();

            return connected;

        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
        }

        return connected;
    }

    public void addDataInDatabase(SQLiteDatabase db) {
        deleteAllRows(db);
        ContentValues values = getContentValues();
        db.insert(DBSchema.MapsActivityTable.TABLE, null, values);
        System.out.println("Data added into the local database");
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(DBSchema.MapsActivityTable.Cols.ID, DataHolder.mId); //Request ID
        values.put(DBSchema.MapsActivityTable.Cols.NAME, DataHolder.clientName);
        values.put(DBSchema.MapsActivityTable.Cols.PHONE_NUMBER, DataHolder.clientPhoneNumber);
        values.put(DBSchema.MapsActivityTable.Cols.LAT_PICK_UP, Double.toString(DataHolder.mPickUp_latlng.latitude));
        values.put(DBSchema.MapsActivityTable.Cols.LNG_PICK_UP, Double.toString(DataHolder.mPickUp_latlng.longitude));
        values.put(DBSchema.MapsActivityTable.Cols.LAT_DROP_OFF, Double.toString(DataHolder.mDropOff_latlng.latitude));
        values.put(DBSchema.MapsActivityTable.Cols.LNG_DROP_OFF, Double.toString(DataHolder.mDropOff_latlng.longitude));
        values.put(DBSchema.MapsActivityTable.Cols.PICK_UP_ADDRESS, DataHolder.mPickUpAddress);
        values.put(DBSchema.MapsActivityTable.Cols.DROP_OFF_ADDRESS, DataHolder.mDropOffAddress);

        if (DataHolder.isClientPickedUp)
            values.put(DBSchema.MapsActivityTable.Cols.IS_CLIENT_PICKED_UP, "1");
        else
            values.put(DBSchema.MapsActivityTable.Cols.IS_CLIENT_PICKED_UP, "0");

        return values;
    }

    public void deleteAllRows(SQLiteDatabase db) {
        if (isTableExist(db))
            db.execSQL("DELETE FROM " + DBSchema.MapsActivityTable.TABLE);
        System.out.println("Data removed in the local database");
    }

    public boolean isTableExist(SQLiteDatabase db) {
        String query = "SELECT * FROM " + DBSchema.MapsActivityTable.TABLE;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        cursor.close();
        return false;
    }

    private void finishRide() {
        DataHolder.showProgressDialog(this);
        String requestId = DataHolder.mId;
        String driverId = Driver.getInstance().get("id");

        mRequestQueue = Volley.newRequestQueue(DriverMapsActivity.this);
        String url = getResources().getString(R.string.server_addr) + "/api/finished-request?request_id=" + requestId + "&driver_id=" + driverId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                DataHolder.dismissProgressDialog();
                JSONObject item = response;
                try {
                    String clientID = response.getString("client_id");
                    toastMessage("Ride Completed");
                    DataHolder.isClientDroppedOff = true;
                    startActivity(new Intent(DriverMapsActivity.this, RecyclerViewActivity.class));
                } catch (JSONException e1) {
                    Log.d("error", "error");
                    e1.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                DataHolder.dismissProgressDialog();
                Log.d("neter", "neter");
                error.printStackTrace();
            }
        });

        mRequestQueue.add(request);

    }
}

