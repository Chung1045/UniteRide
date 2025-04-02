package com.chung.a9rushtobus.elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BusRoute {
    private String company;
    private String route;
    private String bound;
    private String serviceType;
    private String origEn;
    private String origTc;
    private String origSc;
    private String destEn;
    private String destTc;
    private String destSc;
    private String gmbRouteID;
    private String gmbRouteRegion;
    private String gmbRouteSeq;
    private String remarksEn;
    private String remarksTc;
    private String remarksSc;
    private String descriptionEn, descriptionTc, descriptionSc;


    public BusRoute(String route, String company, String bound, String serviceType,
                    String origEn, String origTc, String origSc,
                    String destEn, String destTc, String destSc) {
        this.route = route;
        this.company = company;
        this.bound = bound;
        this.serviceType = serviceType;
        this.origEn = origEn;
        this.origTc = origTc;
        this.origSc = origSc;
        this.destEn = destEn;
        this.destTc = destTc;
        this.destSc = destSc;
    }

    // Constructor for Green mini buses
    public BusRoute(String routeID, String route, String region, String routeSeq,
                   String origEn, String origTc, String origSc,
                   String destEn, String destTc, String destSc,
                   String remarksEn, String remarksTc, String remarksSc, String descriptionEn, String descriptionTc, String descriptionSc) {
        this.route = route;
        this.company = "GMB";

        this.gmbRouteSeq = routeSeq;
        
        // For GMB, we can use the region as service type or combine with remarks if needed
        this.serviceType = region != null ? region : "";
        
        this.origEn = origEn;
        this.origTc = origTc;
        this.origSc = origSc;
        this.destEn = destEn;
        this.destTc = destTc;
        this.destSc = destSc;
        this.remarksEn = remarksEn;
        this.remarksTc = remarksTc;
        this.remarksSc = remarksSc;
        this.gmbRouteRegion = region;
        this.gmbRouteID = routeID;
        this.descriptionEn = descriptionEn;
        this.descriptionTc = descriptionTc;
        this.descriptionSc = descriptionSc;

    }

    // All getters
    public String getRoute() {
        return route;
    }

    public String getCompany() {
        return company;
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

    public String getRemarksEn() {
        return remarksEn;
    }

    public String getRemarksTc() {
        return remarksTc;
    }

    public String getRemarksSc() {
        return remarksSc;
    }

    public String getGmbRouteID() {
        return gmbRouteID;
    }

    public String getGmbRouteRegion() {
        return gmbRouteRegion;
    }

    public String getGMBRouteSeq() {
        return gmbRouteSeq;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public String getDescriptionTc() {
        return descriptionTc;
    }

    public String getDescriptionSc() {
        return descriptionSc;
    }


    /**
     * Compares two BusRoute objects for equality.
     * @param other The BusRoute to compare with
     * @return True if the
    }
    
    /**
     * Compares two route numbers for natural sorting (1, 1A, 1B, 1X, 2, 2A, etc.)
     * Ignores the company and sorts all routes together in ascending order.
     * @param other The BusRoute to compare with
     * @return Negative if this route should come before, positive if after
     */
    public int compareRouteNumber(BusRoute other) {
        if (other == null) {
            return 1; // null comes before any non-null value
        }
        
        String routeStr1 = this.getRoute();
        String routeStr2 = other.getRoute();
        
        // Handle null route strings
        if (routeStr1 == null && routeStr2 == null) {
            return 0;
        } else if (routeStr1 == null) {
            return -1;
        } else if (routeStr2 == null) {
            return 1;
        }
        
        // Normalize route strings (remove spaces, convert to uppercase)
        routeStr1 = routeStr1.trim().toUpperCase();
        routeStr2 = routeStr2.trim().toUpperCase();
        
        // Pattern to match numeric prefix and alphabetic suffix
        // This will handle routes like "1", "1A", "1B", "1X", "2", "2A", etc.
        Pattern pattern = Pattern.compile("^(\\d+)([A-Z]*)");
        Matcher matcher1 = pattern.matcher(routeStr1);
        Matcher matcher2 = pattern.matcher(routeStr2);
        
        boolean hasNumericPrefix1 = matcher1.find();
        boolean hasNumericPrefix2 = matcher2.find();
        
        // Case 1: Both have numeric prefixes (most common case)
        if (hasNumericPrefix1 && hasNumericPrefix2) {
            // Compare the numeric parts first
            int num1 = Integer.parseInt(matcher1.group(1));
            int num2 = Integer.parseInt(matcher2.group(1));
            
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
            
            // If numeric parts are equal, compare the alphabetic suffixes
            String suffix1 = matcher1.group(2);
            String suffix2 = matcher2.group(2);
            
            // If one has no suffix and the other does, the one without comes first
            if (suffix1.isEmpty() && !suffix2.isEmpty()) {
                return -1;
            } else if (!suffix1.isEmpty() && suffix2.isEmpty()) {
                return 1;
            }
            
            // Both have suffixes, compare them alphabetically
            return suffix1.compareTo(suffix2);
        }
        // Case 2: Only the first has a numeric prefix
        else if (hasNumericPrefix1) {
            return -1; // Numeric prefixes come before non-numeric
        }
        // Case 3: Only the second has a numeric prefix
        else if (hasNumericPrefix2) {
            return 1; // Numeric prefixes come before non-numeric
        }
        // Case 4: Neither has a numeric prefix, compare as strings
        else {
            return routeStr1.compareTo(routeStr2);
        }
    }
    
    /**
     * Helper method to extract leading digits from a string
     */
    private String extractLeadingDigits(String str) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {
                digits.append(str.charAt(i));
            } else {
                break;
            }
        }
        return digits.toString();
    }
}