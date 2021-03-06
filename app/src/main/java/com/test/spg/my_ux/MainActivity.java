package com.test.spg.my_ux;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.ux.widget.FPVWidget;

public class MainActivity extends Activity implements View.OnClickListener {

    private double droneLocationLat = 181, droneLocationLng = 181;//181超出180的范围，所以设置该初值
    private Marker droneMarker = null;//表示飞机位置的标记对象
    private List<LatLng> mapPointList = new ArrayList<>();//存储地图上的点，用来画线
    private ViewGroup parentView;// 最外层RelativeLayout

    private FPVWidget fpvWidget;
    private MapView mapView;
    private AMap aMap;
    private Button btn_locate;
    private Button btn_pause;
    private Button btn_reset;
    private Button btn_resume;
    private Button btn_start;
    private Button btn_stop;
    private Button btn_config;
    private Button btn_flush;


    private WaypointMissionOperator instance;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private FlightController mFlightController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 注册广播接受器,接受连接状态变化的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        // 使用高德地图
        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);


        initMapView();
        //
        initFlightController();
        init();
        initWaypoint();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initMapView();
        initWaypoint();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initWaypoint() {
        mapPointList = getMApplication().getPointList();
        LatLng temp;
        Toast.makeText(this, "被調用：" + mapPointList.size() + "個！", Toast.LENGTH_SHORT).show();
        if (!mapPointList.isEmpty()) {
            for (int i = 0; i < mapPointList.size(); i++) {
                temp = mapPointList.get(i);
                markWaypoint(temp, i);
                drawLine();
            }
        }
    }

    /**
     * 画出路径点标记
     * @param point 当前点的坐标
     * @param pointNum 当前点的序号
     */
    private void markWaypoint(LatLng point, int pointNum) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        int pic_i;
        switch ((pointNum + 1) / 9) {
            case 1:
                pic_i = R.drawable.pic_1;
                break;
            case 2:
                pic_i = R.drawable.pic_2;
                break;
            case 3:
                pic_i = R.drawable.pic_3;
                break;
            case 4:
                pic_i = R.drawable.pic_4;
                break;
            case 5:
                pic_i = R.drawable.pic_5;
                break;
            case 6:
                pic_i = R.drawable.pic_6;
                break;
            case 7:
                pic_i = R.drawable.pic_7;
                break;
            case 8:
                pic_i = R.drawable.pic_8;
                break;
            case 9:
                pic_i = R.drawable.pic_9;
                break;
            default:
                pic_i = R.drawable.pic_1;
                break;
        }
        markerOptions.icon(BitmapDescriptorFactory.fromResource(pic_i));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    /**
     *  画出当前所有路径点按顺序的连线
     */
    private void drawLine() {
        if (mapPointList.size() > 1) {
            aMap.addPolyline(new PolylineOptions()
                    .addAll(mapPointList)
                    .width(10)
                    .color(Color.YELLOW));
        }
    }

    /**
     * 初始化控件
     */
    private void init() {

        // 匹配界面上的控件
        parentView = findViewById(R.id.root_view);
        btn_locate = findViewById(R.id.btn_locate);
        btn_pause = findViewById(R.id.btn_pause);
        btn_reset = findViewById(R.id.btn_reset);
        btn_resume = findViewById(R.id.btn_resume);
        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);
        btn_config = findViewById(R.id.btn_config);
        btn_flush = findViewById(R.id.btn_flush);

        // 添加所有按钮的点击事件监听
        btn_locate.setOnClickListener(this);
        btn_pause.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_resume.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_config.setOnClickListener(this);
        btn_flush.setOnClickListener(this);

        // 获取图传信息
        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.getVideoSource();
        fpvWidget.setSourceCameraNameVisibility(false); // 把FPV上的版本名字关掉
    }

    /**
     * 初始化高德地图
     */
    private void initMapView() {
        if (aMap == null) {
            aMap = mapView.getMap();
            // aMap.setOnMapClickListener(this);
        }

        LatLng WHU = new LatLng(30.5304782900, 114.3555023600);
        aMap.addMarker(new MarkerOptions().position(WHU).title("Marker in WHU"));// 添加标记
        aMap.moveCamera(CameraUpdateFactory.newLatLng(WHU));// 标记视野居中
        if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
            updateDroneLocation();
            cameraUpdate();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_locate:
                // 定位飞机位置
                initFlightController();
                updateDroneLocation();
                cameraUpdate();
                break;
            case R.id.btn_start:
                // 开始任务
                startWaypointMission();
                initFlightController();
                updateDroneLocation();
                cameraUpdate();
                break;
            case R.id.btn_stop:
                // 停止任务
                stopWaypointMission();
                break;
            case R.id.btn_resume:
                // 继续任务
                resumeWaypointMission();
                break;
            case R.id.btn_pause:
                // 暂停任务
                pauseWaypointMission();
                break;
            case R.id.btn_reset:
                // 重置
                mapPointList = new ArrayList<>();
                clear();
                Toast.makeText(this, "重置完成", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_config:
                PopupMenu popup = new PopupMenu(this, view);
                Menu popupMenu = popup.getMenu();
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.select_menu, popupMenu);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            default:
                                break;
                            case R.id.action_1:
                                startActivity(new Intent(MainActivity.this, ConfigActivity.class));
                                break;
                            case R.id.action_2:
                                startActivity(new Intent(MainActivity.this, MapActivity.class));
                                break;
                        }
                        return false;
                    }
                });
                popup.show();
                break;
            case R.id.btn_flush:
                parentView.removeView(fpvWidget);
                parentView.addView(fpvWidget, 0);
                //fpvWidget.getVideoSource();
                Toast.makeText(this, "刷新FPV", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    /**
     * 清除覆盖物
     */
    private void clear() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                aMap.clear();
            }
        });//把地图上的覆盖物都清除掉
        updateDroneLocation();
        cameraUpdate();
    }

    // 更新飞机的位置信息
    private void updateDroneLocation() {
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //显示飞机时，GPS坐标转换为火星坐标
        pos = WG2GCJ(pos);
        //创建一个地图上的标记用来表示当前飞机的位置，MarkerOptions为可定义的Marker选项
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);//设置位置
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));//设置图标，通过BitmapDescriptorFactory获取一个BitmapDescriptor对象
        //当updateDroneLocation()被调用时，飞机的位置发生改变，所以还需要更新UI显示飞机的位置
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);//经纬度合法，添加进地图中
                }
            }
        });
    }

    // 世界坐标转为火星坐标
    private LatLng WG2GCJ(LatLng latLng) {
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(latLng);
        return converter.convert();
    }

    // 检查经纬度数值的合法性，返回布尔值
    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // 视角切换到以飞机的位置为居中位置
    private void cameraUpdate() {
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        pos = WG2GCJ(pos);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);
    }

    /**
     * 连接状态改变，更新连接
     */
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initFlightController();
        }
    };


    /**
     * 初始化飞机控制，并获取飞机位置
     */
    private void initFlightController() {
        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {
            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            updateDroneLocation();
                        }
                    });
        }
    }

    /**
     * 获取路径点模式操作
     *
     * @return 返回WaypointMissionOperator实例
     */
    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    private void setResultToToast(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 开始执行路径飞行任务上
     */
    private void startWaypointMission() {

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    /**
     * 停止执行路径飞行任务
     */
    private void stopWaypointMission() {

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    /**
     * 暂停执行路径飞行任务
     */
    private void pauseWaypointMission() {

        getWaypointMissionOperator().pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Pause: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    /**
     * 恢复执行路径飞行任务
     */
    private void resumeWaypointMission() {

        getWaypointMissionOperator().resumeMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Resume: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private MApplication getMApplication() {
        return (MApplication) getApplicationContext();
    }
}


/*````````````````````````````````````````````````..-:-----..````````````````````````...............
````````````````````````````````````````````````.:ossysyyysoo/:-.`````````````````..................
``````````````````````````````````````````````.:osyyyhhhdhyyysss+-````````````````..................
``````````````````````````````````````````````./yyhhhhhhhhhhhhyys:````````````````..................
```````````````````````````````````````````````-yhhhhyysooshdhs+/-````````````````.```````..........
```````````````````````````````````````````````.:sysyyysoosssy-```````````````````...``````.........
````````````````````````````````````````````````./o//+//:///+/``````````````````........``..........
````````````````````````````````````````````-/ohdmho:///////:.```````````````````````...............
```````````````````````````````````````-///--odmmmmms/+ooo+sh::+:.`````````````````````.............
`````````````````````````````````````-oddddds-+mmmmmmdo++ohmmh.sdhs+-```...````````````.............
````````````````````````````````````:hmmmmmmmy:hNNmmmmmmmmNNmm:+mmmmdo.``....``````````.............
```````````````````````````````````.hmmmmmmmmm+yNNNNmmmmmmmmmh:ommmmmmh.`...........................
`````````````````````````````````.:ymmmmmmmmmmodNNNNNNNmmmmmm+omddmmmmms+-.``.......................
```````````````````````````````./ydmmmmmmmNNmyyNNNNNNNNNNNNmssmNNNNmmmmmmdyo:--.....................
`````````````````````````````.:ydmmmmNNNNNmdshmNNNNNNNNNNmhshmdydmNNNmmmmmmmdddy+-..................
````````````````````````````-sdmmmmmNNNNmhssdNNNNNNNNNNNdssdho-..:+shdmmmNmmmmmmmdhso/:.............
`````````````````````````.-odmmmmmNNNNmhoohmNNNNNNNNNNmy++/-.........-://+sydmmmmmmmmmdho-..........
````````````````````````./hmmmmmmmmNmho+ymNNNNNNNNNNmdo-..```...............-:/ohdmmmmmmmdy+-.......
``````````````````````.-sdmmmmmmmmmmhoymNNNNNNNNNNmhyo-````......................-/shdddmmmmd/``....
`````````````````````./hmmmmmdhssyyyhdmmmmmmmmmmmdh/:-..`.......................-:///+/+osdssy.`....
``````````````````..-sdmmmmho:oossyyyhhhhhhhhhhhhhhy+:.......................`./++++/:://+o/o/......
``````````.........:ymmmmy/-.-sssyyyhhyyyyhhyhhhhhhhyy-`....................`.+++++/////++++o+-.....
``````````..```..-+dmmdy/.```-yyyyyyyyyyyyyyyyyyyyyyyy:....................../oo+o+//+++o+++o+-`....
-..```````.```..:smmms:..````.syyyyyyyyyyyyyyyyyyyyyss:....................../oooo+++ooooooos+......
---..`````````./hmmmy-.....`.`+sssssyyyyyssssssssssssoo:......................ossoooossooooso:......
-----...``....-odmmm+.`....`.`/oossssyyyyysyyyyysssssooo:....................../sssssssssso/........
--------....-:::/+oo-........`/oooooosssys-+hhhyysssoooo+-......................-/+ooooo/:..........
----------..:///:.-:.........`:ssoooooosy/`./hhhyysssoooo+-`........................................
------------//::...-..........`/oooooooss:`.`/yyyyssoooooo+.........................................
------------::--...............-oooooooos-`...-syyssooooooo/........................................
-----------------...............+oooooooo:......oysssooooooo-.......................................
-------------------.............:o++++ooo+......./sssooooooo+.......................................
---------------------...........:oo+++++oo:`......-ossooooooo:......................................
------------------------........:oo++++oooo......../ssssoooooo-.....................................
--------------------------......:oooooooos/`........:sssssssss/.....................................
-------:::::---:::::::------..../oosoooss+.........../yyyyyyss/.....................................
.---:::::::::::::::::::::::::---ooosssyy/............-yyyyyyyo....................................*/