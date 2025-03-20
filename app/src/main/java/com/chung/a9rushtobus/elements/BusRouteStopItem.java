package com.chung.a9rushtobus.elements;

import java.util.ArrayList;
import java.util.List;

public class BusRouteStopItem {
    private String route;
    private String bound;
    private String serviceType;
    private String stopEn;
    private String stopTc;
    private String stopSc;
    private String stopID;
    private int stopEta1;
    private int stopEta2;
    private int stopEta3;
    private boolean isExpanded = false;
    private List<String> etaData = new ArrayList<>();

    public BusRouteStopItem(String route, String bound, String serviceType,
                            String stopEn, String stopTc, String stopSc, String stopID) {
        this.route = route;
        this.bound = bound;
        this.serviceType = serviceType;
        this.stopEn = stopEn;
        this.stopTc = stopTc;
        this.stopSc = stopSc;
        this.stopID = stopID;
    }

    public String getRoute() {
        return route;
    }

    public String getBound() {
        return bound;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getStopEn() {
        return stopEn;
    }

    public String getStopTc() {
        return stopTc;
    }

    public String getStopSc() {
        return stopSc;
    }

    public String getStopID() {
        return stopID;
    }

    public int getStopEta1() {
        return stopEta1;
    }

    public int getStopEta2() {
        return stopEta2;
    }

    public int getStopEta3() {
        return stopEta3;
    }

    public void setStopEta1(int stopEta1) {
        this.stopEta1 = stopEta1;
    }

    public void setStopEta2(int stopEta2) {
        this.stopEta2 = stopEta2;
    }

    public void setStopEta3(int stopEta3) {
        this.stopEta3 = stopEta3;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public List<String> getEtaData() {
        return etaData;
    }

    public void setEtaData(List<String> etaData) {
        this.etaData = etaData;
    }

}
