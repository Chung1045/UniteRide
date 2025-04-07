package com.chung.a9rushtobus.elements;

import android.os.Parcel;
import android.os.Parcelable;
import com.chung.a9rushtobus.UserPreferences;
import java.util.ArrayList;
import java.util.List;

public class BusRouteStopItem implements Parcelable {
    private String route, bound, serviceType, stopEn, stopTc, stopSc, stopID, company, closestETA;
    private String gmbRouteID, gmbRouteSeq;
    private int stopEta1;
    private int stopEta2;
    private int stopEta3;
    private boolean isExpanded = false;
    private boolean hasRemarks = false;
    private List<String> etaData = new ArrayList<>();

    public BusRouteStopItem(String route, String bound, String serviceType,
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
    public BusRouteStopItem(String route, String bound, String serviceType,
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
        this.company = "gmb";
    }

    // Parcelable constructor
    protected BusRouteStopItem(Parcel in) {
        route = in.readString();
        bound = in.readString();
        serviceType = in.readString();
        stopEn = in.readString();
        stopTc = in.readString();
        stopSc = in.readString();
        stopID = in.readString();
        company = in.readString();
        closestETA = in.readString();
        gmbRouteID = in.readString();
        gmbRouteSeq = in.readString();
        stopEta1 = in.readInt();
        stopEta2 = in.readInt();
        stopEta3 = in.readInt();
        isExpanded = in.readByte() != 0;
        hasRemarks = in.readByte() != 0;
        etaData = new ArrayList<>();
        in.readStringList(etaData);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(route);
        dest.writeString(bound);
        dest.writeString(serviceType);
        dest.writeString(stopEn);
        dest.writeString(stopTc);
        dest.writeString(stopSc);
        dest.writeString(stopID);
        dest.writeString(company);
        dest.writeString(closestETA);
        dest.writeString(gmbRouteID);
        dest.writeString(gmbRouteSeq);
        dest.writeInt(stopEta1);
        dest.writeInt(stopEta2);
        dest.writeInt(stopEta3);
        dest.writeByte((byte) (isExpanded ? 1 : 0));
        dest.writeByte((byte) (hasRemarks ? 1 : 0));
        dest.writeStringList(etaData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BusRouteStopItem> CREATOR = new Creator<BusRouteStopItem>() {
        @Override
        public BusRouteStopItem createFromParcel(Parcel in) {
            return new BusRouteStopItem(in);
        }

        @Override
        public BusRouteStopItem[] newArray(int size) {
            return new BusRouteStopItem[size];
        }
    };

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

    public String getStopName() {
        String appLang = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");

        switch (appLang) {
            case "zh-rCN":
                return stopSc;
            case "zh-rHK":
                return stopTc;
            default: // "en" or any other case
                return stopEn;
        }
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

    public void setHasRemarks(boolean hasRemarks) {
        this.hasRemarks = hasRemarks;
    }

    public boolean hasRemarks() {
        return hasRemarks;
    }

    public String getStopSeq() {
        return gmbRouteSeq; // For GMB routes, this is the same as gmbRouteSeq
    }
}
