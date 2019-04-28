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
import com.test.spg.my_ux.utils.BDToGPS;
import com.test.spg.my_ux.utils.GS;
import com.test.spg.my_ux.utils.RoutePlan;
import com.test.spg.my_ux.utils.myPoint;

import java.util.ArrayList;
import java.util.Collections;
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
    private Button btn_map_scale;

    // 存储路径点，Wayponit三个参数，经纬高
    private List<Waypoint> waypointList = new ArrayList<>();
    // 路径点
    private List<LatLng> points = new ArrayList<>();


    private List<Marker> mMarkers = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction;
    private WaypointMissionHeadingMode mHeadingMode;
    private double droneLocationLat = 181, droneLocationLng = 181;//181超出180的范围，所以设置该初值
    private Marker droneMarker = null;//表示飞机位置的标记对象

    // 默认高度，速度，最大速度
    private float altitude = 100.0f;
    private float mSpeed = 10.0f;
    private float maxSpeed = 10.0f;

    private MarkerOptions markerOptions;

    // 多边形
    private Polygon polygon;
    // 点
    private Marker point_marker;
    // 点集合
    private List<Marker> pointMarker_list = new ArrayList<>();
    // 线
    private Polyline polyline;
    private double overlap = 0;

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

        initUI();
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

    /**
     * 初始化界面
     */
    private void initUI() {
        txt_map_tv = findViewById(R.id.txt_map_tv);
        btn_map_clear = findViewById(R.id.btn_map_clear);
        btn_map_plan = findViewById(R.id.btn_map_plan);
        btn_map_config = findViewById(R.id.btn_map_config);
        btn_map_finish = findViewById(R.id.btn_map_finish);
        btn_map_locate = findViewById(R.id.btn_map_locate);
        btn_map_scale = findViewById(R.id.btn_map_scale);
        btn_map_scale.setOnClickListener(this);
        btn_map_plan.setOnClickListener(this);
        btn_map_clear.setOnClickListener(this);
        btn_map_config.setOnClickListener(this);
        btn_map_finish.setOnClickListener(this);
        btn_map_locate.setOnClickListener(this);
    }


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
     * 更新飞机的位置信息
     */
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

    /**
     * 按下location,视角切换到以飞机的位置为居中位置
     */
    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        pos = WG2GCJ(pos);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);
    }

    /**
     *  设置航高和重叠度
     */
    private void showScaleDialog(){
        View scaleSettings = getLayoutInflater().inflate(R.layout.dialog_scale, null);

        final TextView txt_height = scaleSettings.findViewById(R.id.txt_altitude);
        final SeekBar skb_altitude = scaleSettings.findViewById(R.id.skb_altitude);
        final TextView txt_overlap = scaleSettings.findViewById(R.id.txt_overlap);
        final SeekBar skb_overlap = scaleSettings.findViewById(R.id.skb_overlap);


        skb_altitude.setMax(3000);
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
        skb_overlap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                txt_overlap.setText(i+"%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle("")
                .setView(scaleSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {
                        altitude = (float) skb_altitude.getProgress() / 10 ;
                        overlap = skb_overlap.getProgress()/100 ;
                    }
                })
                .create()
                .show();
    }

    /**
     * 方法显示设置对话框 高度、速度、任务完成后的行为、朝向
     */
    private void showSettingDialog(){
        View wayPointSettings = getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);


        final TextView txt_speed = wayPointSettings.findViewById(R.id.txt_speed);
        final SeekBar skb_speed = wayPointSettings.findViewById(R.id.skb_speed);



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

    /**
     * 将对话框中所设定的飞行参数导入waypointMissionBuilder中
     */
    private void configWayPointMission(){
        //mFinishedAction mHeadingMode mSpeed mSpeed
        if (waypointMissionBuilder == null){
            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(maxSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }else {
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

    /**
     * 上传路径飞行任务到飞机
     */
    private void uploadWayPointMission(){
        if (mFinishedAction!=null&&mHeadingMode!=null){
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
        }else{
            Toast.makeText(this, "请配置！", Toast.LENGTH_SHORT).show();
        }
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
            case R.id.btn_map_scale:
                // 设置比例，调整规划路径点之间的距离
                showScaleDialog();
                break;
            case R.id.btn_map_plan:
                // 根据当前图形进行规划
                routePlan();
                txt_map_tv.setText("当前点数为："+points.size());
                if (points.size()>99){
                    Toast.makeText(this, "路径超过99个，需要重新规划！", Toast.LENGTH_LONG).show();
                    clear_map();
                    mMarkers.clear();
                } else if (points.size()==0){
                    Toast.makeText(this, "间距过大，需要重新调整！", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        txt_map_tv.setText("停止拖动");
        printPolygon();
    }

    /**
     *  画选定框的多边形
     */
    private void printPolygon() {
        if (polygon != null) {
            polygon.remove();
        }
        PolygonOptions polygonOptions = new PolygonOptions();
        for (Marker marker : mMarkers) {
            polygonOptions.add(marker.getPosition());
        }
        polygonOptions.strokeWidth(15) // 多边形的边框
                .strokeColor(Color.argb(90,0x00,0xE6,0x76)) // 边框颜色
                .fillColor(Color.argb(50, 1, 1, 1));   // 多边形的填充色
        polygon = aMap.addPolygon(polygonOptions);
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

    /**
     * 画选定边界点
     * @param point 边界点
     */
    private void markWaypoint(LatLng point){
        markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location));
        Marker marker = aMap.addMarker(markerOptions);
        marker.setDraggable(true);
        mMarkers.add(marker);
    }

    /**
     * 清除地图上所有覆盖物
     */
    private void clear_map(){
        if (aMap != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    aMap.clear();
                }
            });
        }else {
            Toast.makeText(this, "地图加载错误", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  根据路径点画线
     * @param mapPointList
     */
    private void drawLine(List<LatLng> mapPointList){
        if (aMap != null) {
            polyline = aMap.addPolyline(new PolylineOptions().addAll(mapPointList).width(10).color(Color.YELLOW));
        }else{
            Toast.makeText(this, "地图加载错误", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  根据边界点、航高、重叠度，规划路径
     */
    private void routePlan(){
        points.clear();
        if (mMarkers.size()<3){
            Toast.makeText(this, "请增加区域顶点", Toast.LENGTH_SHORT).show();
        }else{
            for (Marker marker: mMarkers) {
                points.add(marker.getPosition());
            }
            txt_map_tv.append("\n points: " + points.size());
            myPoint[] list = new myPoint[points.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = new myPoint(points.get(i).latitude, points.get(i).longitude);
            }

           /* System.out.println("RoutePlan.dt_lat = " + RoutePlan.dt_lat);
            System.out.println("RoutePlan.dt_lng = " + RoutePlan.dt_lng);

            System.out.println("altitude = " + altitude);
            System.out.println("overlap = " + overlap);*/

            //Log.d(TAG, "routePlan: altitude"+altitude);
            //Log.d(TAG, "routePlan: overlap"+overlap);

            RoutePlan.dt_lng = GS.cal_DeltaLng(list[0].lat, list[0].lng, GS.cal_Length(altitude,overlap));
            RoutePlan.dt_lat = GS.cal_DeltaLat(list[0].lat, list[0].lng, GS.cal_Width(altitude,overlap));

/*            System.out.println("RoutePlan.dt_lat = " + RoutePlan.dt_lat);
            System.out.println("RoutePlan.dt_lng = " + RoutePlan.dt_lng);*/

            Toast.makeText(this, "dt_lat,dt_lng: "+ RoutePlan.dt_lat+"\t"+ RoutePlan.dt_lat, Toast.LENGTH_SHORT).show();

            List<myPoint> myPoints = RoutePlan.getAllPoints(list);
            points.clear();
            waypointList.clear();

            // 添加起飞点
           /* LatLng start = WG2GCJ(new LatLng(droneLocationLat,droneLocationLng));
            myPoints.add(0,new myPoint(start.latitude,start.longitude));*/

            for (myPoint point : myPoints) {
                points.add(new LatLng(point.lat,point.lng));

                double []coord = GCJ2WG(point.lat,point.lng);
                Waypoint mWaypoint = new Waypoint(coord[0], coord[1], altitude);

                // 确保每个点都朝向北方，然后保证云台竖直向下，拍照
                mWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,0));
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
            Toast.makeText(this, "count: "+waypointList.size(), Toast.LENGTH_SHORT).show();
            txt_map_tv.setText("points:" + points.size());
            drawPoint(points);
            drawLine(points);
        }
    }

    /**
     *  画出所有路径点
     * @param points
     */
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

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    private void setResultToToast(final String string){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MapActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 世界坐标转为火星坐标
    public LatLng WG2GCJ(LatLng latLng) {
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(latLng);
        return converter.convert();
    }
    // 火星坐标转世界坐标
    public double []GCJ2WG(double lat, double lng){
        double []coor = BDToGPS.gcj2WGSExactly(lat,lng);
        return coor;
    }

    // 检查经纬度数值的合法性，返回布尔值
    public boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }
}
