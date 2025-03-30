package com.chung.a9rushtobus.database;

import android.database.sqlite.SQLiteDatabase;

public class GMBDatabase {

    private SQLiteDatabase db;

    public static class Tables {

        public static class GMB_ROUTES_INFO implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "gmb_routes";
            public static final String ROUTE_ID = "route_id"; // 7 digit number
            public static final String ROUTE_NUMBER = "route_number";
            public static final String ROUTE_REGION = "route_region"; // HKI / KLN / NT
            public static final String ROUTE_TYPE = "route_type"; // normal or special
            public static final String ROUTE_ORIGIN_EN = "route_origin_en";
            public static final String ROUTE_ORIGIN_TC = "route_origin_tc";
            public static final String ROUTE_ORIGIN_SC = "route_origin_sc";
            public static final String ROUTE_DEST_EN = "route_destination_en";
            public static final String ROUTE_DEST_TC = "route_destination_tc";
            public static final String ROUTE_DEST_SC = "route_destination_sc";
            public static final String REMARKS_EN = "remarks_en";
            public static final String REMARKS_TC = "remarks_tc";
            public static final String REMARKS_SC = "remarks_sc";
            public static final String ROUTE_SEQ = "route_sequence"; // 1 : inbound, 2 : outbound
            public static final String ROUTE_HEADWAY_SEQ = "route_headway_sequence";
            public static final String PUBLIC_HOLIDAY_AVAILABLE = "public_holiday_available"; // boolean
        }

    }

}