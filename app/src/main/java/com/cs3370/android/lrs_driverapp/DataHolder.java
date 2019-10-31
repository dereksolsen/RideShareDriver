package com.cs3370.android.lrs_driverapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.Objects;

public class DataHolder {
    public static LatLng mDropOff_latlng;
    public static LatLng mPickUp_latlng;
    public static LatLng mCurrent_latLng;
    public static String mPickUpAddress = "";
    public static String mDropOffAddress = "";
    public static String mCurrentAddress = "";
    public static String distanceToPickUp;
    public static String distanceToDropOff;
    public static String clientName = "";
    public static String clientPhoneNumber = "";
    public static String navSelected = null;
    public static boolean startedRide = false;
    public static boolean isClientPickedUp = false;
    public static boolean isClientDroppedOff = false;

    public static String mPickup;
    public static String mDropOff;
    public static String mId;
    public static String mStatus;

    public static ProgressDialog progressDialog;

    public static boolean isConnected(Context context) {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = Objects.requireNonNull(cm).getActiveNetworkInfo();
            connected = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();

            return connected;

        } catch (Exception e) {
            Log.e("Connectivity Exception", Objects.requireNonNull(e.getMessage()));
        }

        return connected;
    }

    public static void showProgressDialog(Context context) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(context);

        if (!progressDialog.isShowing()) {
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }
    }

    public static void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public static void toastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
