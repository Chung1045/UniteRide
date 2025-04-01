package com.chung.a9rushtobus.elements;

import java.util.ArrayList;
import java.util.List;

public class BusRouteStopItem {
    private String route, bound, serviceType, stopEn, stopTc, stopSc, stopID, company, closestETA;
    private String gmbRouteID, gmbRouteSeq;
    private int stopEta1;
    private int stopEta2;
    private int stopEta3;
    private boolean isExpanded = false;
    private List<String> etaData = new ArrayList<>();

    public BusRouteStopItem (String route, String bound, String serviceType,
                            String stopEn, String stopTc, String stopSc, String stopID, String company) {
        this.route = route;
        this.bound = bound;
        this.serviceType = serviceType;
        this.stopEn = stopEn;
        this.stopTc = stopTc;
        this.stopSc = stopSc;
        this.stopID = stopID;
        this.company = company;
    }

    // Constructor for GMB routes that need specific routeID and routeSeq
    public BusRouteStopItem (String route, String bound, String serviceType,
                             String stopEn, String stopTc, String stopSc, String stopID, 
                             String gmbRouteID, String gmbRouteSeq) {
        this.route = route;
        this.bound = bound;
        this.serviceType = serviceType;
        this.stopEn = stopEn;
        this.stopTc = stopTc;
        this.stopSc = stopSc;
        this.stopID = stopID;
        this.gmbRouteID = gmbRouteID;
        this.gmbRouteSeq = gmbRouteSeq;
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

    public String getCompany() {
        return company;
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

    public List<String> getEtaDataFull() {
        return etaData;
    }

    public void setClosestETA(String closestETA) {
        this.closestETA = closestETA;
    }

    public String getClosestETA() {
        return closestETA;
    }

    public void setEtaData(List<String> etaData) {
        this.etaData = etaData;
    }

    public String getGmbRouteID() {
        return gmbRouteID;
    }

    public void setGmbRouteID(String gmbRouteID) {
        this.gmbRouteID = gmbRouteID;
    }

    public String getGmbRouteSeq() {
        return gmbRouteSeq;
    }

    public void setGmbRouteSeq(String gmbRouteSeq) {
        this.gmbRouteSeq = gmbRouteSeq;
    }
    
    // Simplified constructor for GMB routes
    public BusRouteStopItem(String route, String serviceType, String stopID, String gmbRouteID) {
        this.route = route;
        this.serviceType = serviceType;
        this.stopID = stopID;
        this.gmbRouteID = gmbRouteID;
    }
}
