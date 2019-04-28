package com.test.spg.my_ux;

import android.app.Application;
import android.content.Context;

import com.amap.api.maps2d.model.LatLng;
import com.secneo.sdk.Helper;

import java.util.ArrayList;
import java.util.List;


public class MApplication extends Application {

    private DemoApplication demoApplication;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (demoApplication == null) {
            demoApplication = new DemoApplication();
            demoApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        demoApplication.onCreate();
    }

    private List<LatLng> pointList = new ArrayList<>();

    public List<LatLng> getPointList() {
        return pointList;
    }

    public void setPointList(List<LatLng> pointList) {
        this.pointList = pointList;
    }
}
