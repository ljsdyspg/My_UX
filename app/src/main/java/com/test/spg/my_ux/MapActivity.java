package com.test.spg.my_ux;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.Polygon;
import com.amap.api.maps2d.model.PolygonOptions;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.amap.api.maps2d.model.Text;
import com.test.spg.my_ux.utils.BDToGPS;
import com.test.spg.my_ux.utils.RoutePlan;
import com.test.spg.my_ux.utils.myPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MapActivity extends Activity implements AMap.OnMapClickListener, View.OnClickListener , AMap.OnMarkerDragListener{

    private static final String TAG = "MapActivity";
    private MapView mapView;
    private AMap aMap;
    private TextView txt_map_tv;
    private Button btn_map_locate;
    private Button btn_map_clear;
    private Button btn_map_plan;
    private Button btn_map_config;
    private Button btn_map_finish;

    private List<Waypoint> waypointList = new ArrayList<>();//存储路径，Wayponit三个参数，经纬高
    private List<LatLng> points = new ArrayList<>();

    List<Float> distances = new ArrayList<>();
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
    private double droneLocationLat = 181, droneLocationLng = 181;//181超出180的范围，所以设置该初值
    private Marker droneMarker = null;//表示飞机位置的标记对象

    int max_index;
    int min_index;
    private float altitude = 100.0f;
    private float mSpeed = 10.0f;
    private float maxSpeed = 10.0f;

    private MarkerOptions markerOptions;
    private PolygonOptions polygonOptions;
    // 多边形
    private Polygon polygon;
    // 点
    private Marker point_marker;
    // 点集合
    private List<Marker> pointMarker_list = new ArrayList<>();
    // 线
    private Polyline polyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);//过滤器监视飞机连接状态的改变
        registerReceiver(mReceiver, filter);//注册广播接收器


        mapView = findViewById(R.id.mapview_map);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        aMap.setOnMapClickListener(this);
        aMap.setOnMarkerDragListener(this);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(30.5275277595,114.3610882759), 18));
        //addMarkersToMap();


        initUI();
        initPoint();
        // printPolygon();
        //calculateLength();
    }
    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    private void initUI() {
        txt_map_tv = findViewById(R.id.txt_map_tv);
        btn_map_clear = findViewById(R.id.btn_map_clear);
        btn_map_plan = findViewById(R.id.btn_map_plan);
        btn_map_config = findViewById(R.id.btn_map_config);
        btn_map_finish = findViewById(R.id.btn_map_finish);
        btn_map_locate = findViewById(R.id.btn_map_locate);
        btn_map_plan.setOnClickListener(this);
        btn_map_clear.setOnClickListener(this);
        btn_map_config.setOnClickListener(this);
        btn_map_finish.setOnClickListener(this);
        btn_map_locate.setOnClickListener(this);
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
    }

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
/*            String a = "lat: "+droneLocationLat+" lng: "+droneLocationLng;
            Toast.makeText(this,a,Toast.LENGTH_SHORT).show();*/
        }
    }

    //更新飞机的位置信息
    private void updateDroneLocation(){
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

    //检查经纬度数值的合法性，返回布尔值
    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    //按下location,视角切换到以飞机的位置为居中位置
    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        pos = WG2GCJ(pos);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);
    }

    // 世界坐标转为火星坐标
    private LatLng WG2GCJ(LatLng latLng){
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(latLng);
        return converter.convert();
    }
    //该方法显示设置对话框 高度、速度、任务完成后的行为、朝向？
    private void showSettingDialog(){
        View wayPointSettings = getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);
        final TextView txt_height = (TextView) wayPointSettings.findViewById(R.id.txt_altitude);
        final SeekBar skb_altitude = wayPointSettings.findViewById(R.id.skb_altitude);
        final SeekBar skb_speed = wayPointSettings.findViewById(R.id.skb_speed);
        final TextView txt_speed = wayPointSettings.findViewById(R.id.txt_speed);
        skb_altitude.setMax(1000);
        skb_altitude.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                txt_height.setText((float)i / 10 + " 米");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        skb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                txt_speed.setText((float)i / 10 + " 米/秒");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {
                        altitude = (float) skb_altitude.getProgress() / 10 ;
                        mSpeed = (float) skb_speed.getProgress() / 10;
                        configWayPointMission();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

    //将对话框中所设定的飞行参数导入waypointMissionBuilder中
    private void configWayPointMission(){
        //mFinishedAction mHeadingMode mSpeed mSpeed
        if (waypointMissionBuilder == null){
            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(maxSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(maxSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){
            //每段航程的高度都设置成一样的
            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }
        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }
    }

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }
    //上传路径飞行任务到飞机
    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                    getMApplication().setPointList(points);
                    finish();
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            default:break;
            case R.id.btn_map_locate:
                // 定位飞机的位置
                initFlightController();
                updateDroneLocation();
                cameraUpdate();
                break;
            case R.id.btn_map_clear:
                // 清除当前所有覆盖物和点规划
                clear_map();
                mMarkers.clear();
                break;
            case R.id.btn_map_plan:
                // 根据当前图形进行规划
                routePlan();
                txt_map_tv.setText("当前点数为："+points.size());
                if (points.size()>99){
                    Toast.makeText(this, "路径超过99个，需要重新规划！", Toast.LENGTH_LONG).show();
                    clear_map();
                    mMarkers.clear();
                }
                break;
            case R.id.btn_map_config:
                // 配置信息
                showSettingDialog();
                break;
            case R.id.btn_map_finish:
                // 提交任务
                uploadWayPointMission();
                break;
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        txt_map_tv.setText("开始拖动");
        for (Marker point_marker : pointMarker_list) {
            point_marker.remove();
        }
        polyline.remove();
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        txt_map_tv.setText("正在拖动"+"\nmaker: "+marker.getPosition());
        printPolygon();
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        txt_map_tv.setText("停止拖动");
        printPolygon();
    }


    private void printPolygon() {
        if (polygon != null) {
            polygon.remove();
        }
        polygonOptions = new PolygonOptions();
        for (Marker marker : mMarkers.values()) {
            polygonOptions.add(marker.getPosition());
        }
        polygonOptions.strokeWidth(15) // 多边形的边框
                .strokeColor(Color.argb(90,0x00,0xE6,0x76)) // 边框颜色
                .fillColor(Color.argb(50, 1, 1, 1));   // 多边形的填充色
        polygon = aMap.addPolygon(polygonOptions);
    }

    private void initPoint() {
        points.add(new LatLng(30.5272665493,114.3608650615));// 30.5272665493,114.3608650615
        points.add(new LatLng(30.5282255024,114.3608650615));// 30.5282255024,114.3608522415
        points.add(new LatLng(30.5272412676,114.3616032600));
        points.add(new LatLng(30.5272274051,114.3608468771));
    }

    private void calculateLength(){
        String data = "";
        /*for (int i = 0; i < points.size()-1; i++) {
            data += "距离" + (i+1) +" : "+ AMapUtils.calculateLineDistance(points.get(i),points.get(i+1))+"\n";
        }
        data += "距离" + points.size() +" : "+ AMapUtils.calculateLineDistance(points.get(points.size()-1),points.get(0))+"\n";
        txt_data.setText(data);*/
        for (int i = 0; i < points.size()-1; i++) {
            distances.add( AMapUtils.calculateLineDistance(points.get(i),points.get(i+1)));
        }
        distances.add(AMapUtils.calculateLineDistance(points.get(points.size()-1),points.get(0)));
        float max_distance = Collections.max(distances);
        float min_distance = Collections.min(distances);
        for (int i = 0; i < distances.size(); i++) {
            data += "距离" + i +" : "+ distances.get(i)+"\n";
        }


        max_index = distances.indexOf(max_distance);
        min_index = distances.indexOf(min_distance);

        data += "最大值: " + max_distance + "\n";
        data += "最小值: " + min_distance + "\n";
        data += "最大的序号" + max_index + "\n";
        data += "最小的序号" + min_index + "\n";


        float [] result = new float[3];
        Location.distanceBetween(30.5296077993,114.3553496015,30.5305827924,114.3553496015,result);
        Location.distanceBetween(30.5296077993,114.3553496015,30.5305827924,114.3553496015,result);

        data += "Google1: " + result[0];
        Toast.makeText(this, ""+result[0], Toast.LENGTH_SHORT).show();
        txt_map_tv.setText(data);
    }

    @Override
    public void onMapClick(LatLng point) {
        markWaypoint(point);//按一下，显示一个新的点
        // drawLine(point);

        txt_map_tv.append("\n"+mMarkers.size()+": "+point.latitude+point.longitude);
        Log.d(TAG, "onMapClick: "+"\n"+mMarkers.size()+": "+point.latitude+", "+point.longitude);
        if (mMarkers.size()>=3){
            printPolygon();
        }
    }

    private void markWaypoint(LatLng point){
        markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location));
        Marker marker = aMap.addMarker(markerOptions);
        marker.setDraggable(true);
        mMarkers.put(mMarkers.size(), marker);
    }

    private void clear_map(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                aMap.clear();
            }
        });
    }

    private void drawLine(List<LatLng> mapPointList){
            polyline = aMap.addPolyline(new PolylineOptions().addAll(mapPointList).width(10).color(Color.YELLOW));
    }

    private double []GCJ2WG(double lat, double lng){
        double []coor = BDToGPS.gcj2WGSExactly(lat,lng);
        return coor;
    }

    private void routePlan(){
        points.clear();
        if (mMarkers.size()<3){
            Toast.makeText(this, "请增加区域顶点", Toast.LENGTH_SHORT).show();
        }else{
            for (Marker marker: mMarkers.values()) {
                points.add(marker.getPosition());
            }
            txt_map_tv.append("\n points: " + points.size());
            /*myPoint p0 = new myPoint(points.get(0).latitude, points.get(0).longitude);
            myPoint p1 = new myPoint(points.get(1).latitude, points.get(1).longitude);
            myPoint p2 = new myPoint(points.get(2).latitude, points.get(2).longitude);
            //myPoint p3 = new myPoint(points.get(3).latitude, points.get(3).longitude);*/
            myPoint[] list = new myPoint[points.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = new myPoint(points.get(i).latitude, points.get(i).longitude);
            }
            List<myPoint> myPoints = RoutePlan.getAllPoints(list);
            points.clear();
            for (myPoint point : myPoints) {
                points.add(new LatLng(point.lat,point.lng));

                double []coord = GCJ2WG(point.lat,point.lng);
                Waypoint mWaypoint = new Waypoint(coord[0], coord[1], altitude);
                mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,-90));
                mWaypoint.addAction(new WaypointAction(WaypointActionType.STAY,2000));
                mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,3000));
                //Add Waypoints to Waypoint arraylist;
                if (waypointMissionBuilder != null) {
                    waypointList.add(mWaypoint);
                    waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
                }else {
                    waypointMissionBuilder = new WaypointMission.Builder();
                    waypointList.add(mWaypoint);
                    waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
                }
            }
            txt_map_tv.setText("points:" + points.size());
            drawPoint(points);
            drawLine(points);
        }
    }

    private void drawPoint(List<LatLng> points){
        for (LatLng point: points) {
            markerOptions = new MarkerOptions();
            markerOptions.position(point);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pic_1));
            point_marker = aMap.addMarker(markerOptions);
            pointMarker_list.add(point_marker);
        }
    }

    private MApplication getMApplication(){
        return (MApplication)getApplicationContext();
    }

    private void setResultToToast(final String string){
        MapActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MapActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
