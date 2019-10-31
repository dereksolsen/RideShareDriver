package com.cs3370.android.lrs_driverapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.cs3370.android.lrs_driverapp.MapsActivity.TaskParser.mMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int REQUEST_LOCATION_PERMISSIONS = 0;

    private static final int LOCATION_REQUEST = 500;
    private FusedLocationProviderClient mFLPClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    //Member variable
    private float mZoomLevel = 15;


    private Button mStartButton;
    private Button mConfirmButton;

    private RequestQueue mRequestQueue;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        DataHolder.mPickUpAddress = getIntent().getSerializableExtra("pickUp").toString();
        DataHolder.mDropOffAddress = getIntent().getSerializableExtra("dropOff").toString();

        mFLPClient = LocationServices.getFusedLocationProviderClient(this);

        context = this;

        //create location request
        mLocationRequest = new LocationRequest();
        //mLocationRequest.setInterval(5000);
        //mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //Create location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (DataHolder.isConnected(MapsActivity.this)) {
                    for (Location location : locationResult.getLocations()) {
                        updateMap(location);
                    }
                }
                else
                    toastMessage("No internet connection!");
            }
        };

        mStartButton = (Button) findViewById(R.id.startButton);
        mConfirmButton = (Button) findViewById(R.id.confirmButton);

        if (getIntent().getSerializableExtra("status").toString().equals("0")) {
            mConfirmButton.setText("Accept Ride");
            mStartButton.setVisibility(View.GONE);
            mStartButton.setEnabled(false);
        } else {
            mConfirmButton.setEnabled(false);
            mConfirmButton.setVisibility(View.GONE);
            mStartButton.setVisibility(View.VISIBLE);
        }

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (DataHolder.isConnected(MapsActivity.this)) {

                    DataHolder.isClientPickedUp = false;
                    Intent intent = new Intent(MapsActivity.this, DriverMapsActivity.class);
                    startActivity(intent);
                }
                else
                    toastMessage("No internet connection!");
            }
        });

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DataHolder.isConnected(MapsActivity.this)) {
                    if (getIntent().getSerializableExtra("status").toString().equals("0")) {
                        acceptRide();
                    }
                }
                else
                    toastMessage("No internet connection!");
            }
        });
    }

    private void acceptRide() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accept this ride?");
        builder.setMessage("Press 'Yes' to confirm accepting this ride.");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String requestId = getIntent().getSerializableExtra("id").toString();
                String driverId = Driver.getInstance().get("id");
                DataHolder.showProgressDialog(MapsActivity.this);
                mRequestQueue = Volley.newRequestQueue(context);
                String url = getResources().getString(R.string.server_addr) + "/api/accept-request?driver_id=" + driverId + "&request_id=" + requestId;

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        JSONObject item = response;
                        try {
                            String clientID = response.getString("client_id");
                            toastMessage("Ride Accepted");
                            DataHolder.dismissProgressDialog();
                            DataHolder.navSelected = "acceptedRides";
                            RideListHandler.getInstance().updateList(MapsActivity.this, "/api/driver-requests?id=" + Driver.getInstance().get("id"), "Accepted");
                        } catch (JSONException e1) {
                            DataHolder.dismissProgressDialog();
                            e1.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        DataHolder.dismissProgressDialog();
                        error.printStackTrace();
                    }
                });

                mRequestQueue.add(request);

            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                toastMessage("Ride Not Accepted");
            }
        });

        builder.show();
    }

    private void updateMap(Location location) {

        // Get locations
        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        Address puAddr = geoLocate(DataHolder.mPickUpAddress);
        Address doAddr = geoLocate(DataHolder.mDropOffAddress);
        if (puAddr != null && doAddr != null) {
            DataHolder.mPickUp_latlng = new LatLng(puAddr.getLatitude(), puAddr.getLongitude());
            DataHolder.mDropOff_latlng = new LatLng(doAddr.getLatitude(), doAddr.getLongitude());

            // create the markers
            MarkerOptions myMarker = new MarkerOptions().title("Here you are!").position(myLatLng);
            myMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            MarkerOptions puMarker = new MarkerOptions().title("Pick up here!").position(DataHolder.mPickUp_latlng);
            puMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            MarkerOptions doMarker = new MarkerOptions().title("Drop off here!").position(DataHolder.mDropOff_latlng);
            doMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            // Remove previous marker
            mMap.clear();

            // Add new markers
            mMap.addMarker(myMarker);
            mMap.addMarker(puMarker);
            mMap.addMarker(doMarker);

            // Move and zoom to current location at the street level
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(myLatLng, 15);
            mMap.animateCamera(update);

            // Zoom to previously saved level
            update = CameraUpdateFactory.newLatLngZoom(myLatLng, mZoomLevel);
            mMap.animateCamera(update);

            //create directions url for each
            String urlToPickup = getRequestUrl(myLatLng, DataHolder.mPickUp_latlng);
            TaskRequestDirections taskRequestDirections1 = new TaskRequestDirections();
            taskRequestDirections1.execute(urlToPickup);
            String urlToDropoff = getRequestUrl(DataHolder.mPickUp_latlng, DataHolder.mDropOff_latlng);
            TaskRequestDirections taskRequestDirections2 = new TaskRequestDirections();
            taskRequestDirections2.execute(urlToDropoff);
        }
    }

    public static String getRequestUrl(LatLng origin, LatLng dest) {
        //Value of origin
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        //Value of destination
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        //Set value enable the sensor
        String sensor = "sensor=false";
        //Mode for find direction
        String mode = "mode=driving";
        //Build the full param
        String param = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
        //Output format
        String output = "json";
        //api key
        String apiKey = "&key=AIzaSyA2ge3xTYUPagFuMb5cWtR2Sk5aoNMDir0";
        //Create url to request
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param  + apiKey;

        return url;
    }

    private static String requestDirection(String reqUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnections = null;
        try {
            URL url = new URL(reqUrl);
            httpURLConnections = (HttpURLConnection) url.openConnection();
            httpURLConnections.connect();

            //Get the response result
            inputStream = httpURLConnections.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();

        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (inputStream != null) {
                inputStream.close();
            }
            httpURLConnections.disconnect();
        }
        return responseString;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Save zoom level
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                CameraPosition cameraPosition = mMap.getCameraPosition();
                mZoomLevel =cameraPosition.zoom;
            }
        });

        // Handle marker click
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(MapsActivity.this, "Lat: " + marker.getPosition().latitude +
                        "\nLong: " + marker.getPosition().longitude, Toast.LENGTH_LONG).show();
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mFLPClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataHolder.dismissProgressDialog();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();

        if (hasLocationPermission()) {
            mFLPClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    private boolean hasLocationPermission() {

        // Request fine location permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_LOCATION_PERMISSIONS);

            return false;
        }

        return true;
    }

    private Address geoLocate(String location) {
        String searchString = location;
        Address address = null;

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e) {
            Log.e("TAG", "geoLocate: IOException: " + e.getMessage());
        }
        if (list.size() > 0) {
            address = list.get(0);

            Log.d("Tag", "geoLocate: foundLocation: " + address.toString());
        }
        return address;
    }

    public static class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String...strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            }catch (IOException e) {
                Log.d("exc", "exception caught");
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d("what", "is s " + s);
            super.onPostExecute(s);
            //Parse json here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public static class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {

        public static GoogleMap mMap;

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String...strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            }catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            //Get list route and display it into the map
            Log.d("lists", "here it is " + lists);
            ArrayList points = null;

            PolylineOptions polylineOptions = null;

            for(List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    points.add(new LatLng(lat, lng));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }

            if (polylineOptions != null) {
                mMap.addPolyline(polylineOptions);
            }else {
                Log.d("hey", "is this on");
            }
        }
    }

    public void toastMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
