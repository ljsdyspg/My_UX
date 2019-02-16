package com.test.spg.my_ux.utils;

public class myPoint {
    public double lat;
    public double lng;

    public myPoint(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    boolean isEmpty() {
        return this.lat == 0 || this.lng == 0;
    }
}
