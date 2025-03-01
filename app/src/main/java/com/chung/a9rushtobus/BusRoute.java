package com.chung.a9rushtobus;

public class BusRoute {
    private String route;
    private String bound;
    private String serviceType;
    private String origEn;
    private String origTc;
    private String origSc;
    private String destEn;
    private String destTc;
    private String destSc;

    public BusRoute(String route, String bound, String serviceType,
                    String origEn, String origTc, String origSc,
                    String destEn, String destTc, String destSc) {
        this.route = route;
        this.bound = bound;
        this.serviceType = serviceType;
        this.origEn = origEn;
        this.origTc = origTc;
        this.origSc = origSc;
        this.destEn = destEn;
        this.destTc = destTc;
        this.destSc = destSc;
    }

    // All getters
    public String getRoute() {
        return route;
    }

    public String getBound() {
        return bound;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getOrigEn() {
        return origEn;
    }

    public String getOrigTc() {
        return origTc;
    }

    public String getOrigSc() {
        return origSc;
    }

    public String getDestEn() {
        return destEn;
    }

    public String getDestTc() {
        return destTc;
    }

    public String getDestSc() {
        return destSc;
    }
}