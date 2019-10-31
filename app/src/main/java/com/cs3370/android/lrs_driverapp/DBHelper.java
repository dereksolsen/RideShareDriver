package com.cs3370.android.lrs_driverapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cs3370.android.lrs_driverapp.DBSchema.MapsActivityTable;

import static com.cs3370.android.lrs_driverapp.DBSchema.MapsActivityTable.TABLE;

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper mydb;

    public static DBHelper getInstance(Context context) {
        if (mydb == null) {
            mydb = new DBHelper(context);
        }
        return mydb;
    }

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "rideShare.db";

    public DBHelper(Context context) {

        super(context, DATABASE_NAME, null, VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + "(" +
                MapsActivityTable.Cols.ID + ", " +
                MapsActivityTable.Cols.NAME + ", " +
                MapsActivityTable.Cols.PHONE_NUMBER + ", " +
                MapsActivityTable.Cols.LAT_PICK_UP + ", " +
                MapsActivityTable.Cols.LNG_PICK_UP + ", " +
                MapsActivityTable.Cols.LAT_DROP_OFF + ", " +
                MapsActivityTable.Cols.LNG_DROP_OFF + ", " +
                MapsActivityTable.Cols.PICK_UP_ADDRESS + ", " +
                MapsActivityTable.Cols.DROP_OFF_ADDRESS + ", " +
                MapsActivityTable.Cols.IS_CLIENT_PICKED_UP +
                ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + MapsActivityTable.TABLE);
        onCreate(db);
    }

}
