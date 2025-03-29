package com.chung.a9rushtobus.database;

import android.database.sqlite.SQLiteDatabase;

public class GMBDatabase {

    private SQLiteDatabase db;

    public static class Tables {

        public static class GMB_ROUTES implements android.provider.BaseColumns {
            public static final String TABLE_NAME = "gmb_routes";
            public static final String ROUTE_ID = "route_id";
            public static final String ROUTE_NAME = "route_name";
        }

    }

}
