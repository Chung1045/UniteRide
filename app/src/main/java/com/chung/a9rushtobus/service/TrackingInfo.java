package com.chung.a9rushtobus.service;

import android.os.Handler;
import com.chung.a9rushtobus.elements.BusRouteStopItem;

public class TrackingInfo {
    private final BusRouteStopItem stopItem;
    private boolean isTracking;
    private final int notificationId;
    private final Handler handler;

    public TrackingInfo(BusRouteStopItem stopItem, Handler handler, int notificationId) {
        this.stopItem = stopItem;
        this.handler = handler;
        this.notificationId = notificationId;
        this.isTracking = true;
    }

    public BusRouteStopItem getStopItem() {
        return stopItem;
    }

    public boolean isTracking() {
        return isTracking;
    }

    public void setTracking(boolean tracking) {
        isTracking = tracking;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public Handler getHandler() {
        return handler;
    }
}
