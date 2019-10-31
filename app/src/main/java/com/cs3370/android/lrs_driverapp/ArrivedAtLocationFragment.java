package com.cs3370.android.lrs_driverapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import static com.cs3370.android.lrs_driverapp.DriverMapsActivity.getDistance;
import static com.cs3370.android.lrs_driverapp.SignInActivity.mDriver;

public class ArrivedAtLocationFragment extends Fragment {

    private Button mButtonConfirm;
    private Button mButtonCall;
    private Button mButtonCancel;
    private SQLiteDatabase myDatabase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_arrived_at_location, container, false);

        DataHolder.distanceToPickUp = getDistance(DataHolder.mCurrent_latLng, DataHolder.mPickUp_latlng);
        DataHolder.distanceToDropOff = getDistance(DataHolder.mCurrent_latLng, DataHolder.mDropOff_latlng);

        myDatabase = new DBHelper(getContext()).getWritableDatabase();

        mButtonConfirm = (Button) v.findViewById(R.id.btn_confirm);
        mButtonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Convert distance from miles to meters
                double dist = Double.parseDouble(DataHolder.distanceToPickUp);
                //Create the object of AlertDialog Builder class
                AlertDialog.Builder alertDialogBuilder = new AlertDialog
                        .Builder(getActivity());
                //Set the message to show for the alert time
                String message = null;
                message = "You are " + dist + " miles away from your destination.\n Would you like to proceed?";
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder.setTitle(message);
                alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        DataHolder.isClientPickedUp = true;

                        //**********************Save to MySQLite*************************
                        addDataInDatabase(myDatabase);

                        Intent intent = new Intent(getActivity(), DriverMapsActivity.class);
                        startActivity(intent);
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

        mButtonCall = (Button) v.findViewById(R.id.btn_call);

        if (DataHolder.clientPhoneNumber == null || DataHolder.clientPhoneNumber.equals(""))
            mButtonCall.setVisibility(View.GONE);

        mButtonCall.setText("Call " + DataHolder.clientName);
        mButtonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + DataHolder.clientPhoneNumber));
                startActivity(callIntent);

                if (ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                startActivity(callIntent);
            }
        });

        mButtonCancel = (Button) v.findViewById(R.id.btn_cancel);
        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DataHolder.mPickUpAddress = "";
                DataHolder.mDropOffAddress = "";
                DataHolder.mPickUp_latlng = null;
                DataHolder.mDropOff_latlng = null;
                deleteAllRows(myDatabase);
                Intent intent = new Intent(getActivity(), RecyclerViewActivity.class);
                startActivity(intent);
                /* Rate passenger
                 * Rate passenger
                 * Go back to your home page.
                 */

            }
        });

        return v;
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
}

