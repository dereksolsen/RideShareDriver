package com.cs3370.android.lrs_driverapp;

public class DBSchema {

    public static final class MapsActivityTable {
        public static final String TABLE = "data";
        public static final class Cols {
            public static final String ID = "id";
            public static final String NAME = "name";
            public static final String PHONE_NUMBER = "phoneNumber";

            public static final String LAT_PICK_UP = "latPickUp";
            public static final String LNG_PICK_UP = "lngPickUp";

            public static final String LAT_DROP_OFF = "latDropOff";
            public static final String LNG_DROP_OFF = "lngDropOff";

            public static final String PICK_UP_ADDRESS = "pickUpAddress";
            public static final String DROP_OFF_ADDRESS = "dropOffAddress";

            public static final String IS_CLIENT_PICKED_UP = "isClientPickedUp";
            public static final String IS_CLIENT_DROPPED_OFF = "isClientDroppedOff";
        }
    }
}
