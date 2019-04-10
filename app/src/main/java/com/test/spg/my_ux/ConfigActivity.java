package com.test.spg.my_ux;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
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
import com.test.spg.my_ux.utils.BDToGPS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class ConfigActivity extends Activity implements View.OnClickListener, AMap.OnMapClickListener {

    private double droneLocationLat = 181, droneLocationLng = 181;//181超出180的范围，所以设置该初值
    private Marker droneMarker = null;//表示飞机位置的标记对象
    private AMap aMap;
    private MapView mapView;

    private Button btn_config_add;
    private Button btn_config_advanced;
    private Button btn_config_clear;
    private Button btn_config_finish;
    private Button btn_config_locate;

    private boolean isAdd = false;// 添加按钮标志
    private int pointNum = 0;
    private float altitude = 100.0f;
    private float mSpeed = 10.0f;
    private float maxSpeed = 10.0f;

    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private List<Waypoint> waypointList = new ArrayList<>();//存储路径，Wayponit三个参数，经纬高
    private List<LatLng> mapPointList = new ArrayList<>();//存储地图上的点，用来画线

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);//过滤器监视飞机连接状态的改变
        registerReceiver(mReceiver, filter);//注册广播接收器


        mapView = findViewById(R.id.config_map);
        mapView.onCreate(savedInstanceState);


        initFlightController();
        init();
        initMapView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initMapView();
        getWaypointMissionOperator().clearMission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void init(){
        btn_config_locate = findViewById(R.id.btn_config_locate);
        btn_config_add = findViewById(R.id.btn_config_add);
        btn_config_advanced = findViewById(R.id.btn_config_advanced);
        btn_config_clear = findViewById(R.id.btn_config_clear);
        btn_config_finish = findViewById(R.id.btn_config_finish);

        btn_config_add.setOnClickListener(this);
        btn_config_advanced.setOnClickListener(this);
        btn_config_clear.setOnClickListener(this);
        btn_config_finish.setOnClickListener(this);
        btn_config_locate.setOnClickListener(this);
    }


    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);
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
        switch (view.getId()){
            default:
                break;
            case R.id.btn_config_locate:
                // 定位飞机的位置
                initFlightController();
                updateDroneLocation();
                cameraUpdate();
                break;
            case R.id.btn_config_add:
                // 添加路径点
                enableDisableAdd();
                pointNum=0;
                break;
            case R.id.btn_config_clear:
                // 清除路径点
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aMap.clear();
                    }

                });//把地图上的覆盖物都清除掉
                waypointList.clear();//清除已设置的路径
                mapPointList.clear();//清楚已画出的路线
                pointNum=0;
                waypointMissionBuilder.waypointList(waypointList);
                updateDroneLocation();//然后再把飞机的位置显示出来
                break;
            case R.id.btn_config_advanced:
                // 高级配置
                showSettingDialog();
                break;
            case R.id.btn_config_finish:
                // 提交任务并返回到原界面
                uploadWayPointMission();
               /* // 跳转回主页面
                Intent intent = new Intent();
                //intent.putExtra("result","This is result message! ");
                intent.putExtra("myWaypointMissionBuilder", (Parcelable) waypointMissionBuilder);
                ConfigActivity.this.setResult(1, intent);
                ConfigActivity.this.finish();*/
     /*           try {
                    Thread.sleep(1500);
                    if (isUploadSuccess) {
                        finish();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                break;
        }
    }

    //该方法显示设置对话框 高度、速度、任务完成后的行为、朝向？
    private void showSettingDialog(){
        View wayPointSettings = getLayoutInflater().inflate(R.layout.dialog_waypointsetting_origin, null);

        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.origin_actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.origin_heading);
        final TextView txt_height = (TextView) wayPointSettings.findViewById(R.id.origin_txt_altitude);
        final SeekBar skb_altitude = wayPointSettings.findViewById(R.id.origin_skb_altitude);
        final SeekBar skb_speed = wayPointSettings.findViewById(R.id.origin_skb_speed);
        final TextView txt_speed = wayPointSettings.findViewById(R.id.origin_txt_speed);
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
                if (checkedId == R.id.origin_finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.origin_finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.origin_finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.origin_finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.origin_headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.origin_headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.origin_headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.origin_headingWP) {
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

    private String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    private boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void enableDisableAdd(){
        if (!isAdd) {
            isAdd = true;
            btn_config_add.setText("退出添加");
            btn_config_add.setTextColor(Color.RED);
        }else{
            isAdd = false;
            btn_config_add.setText("添加路径点");
            btn_config_add.setTextColor(Color.BLACK);
        }
    }

    private void markWaypoint(LatLng point){
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        int pic_i = R.drawable.pic_1;
        switch (pointNum){
            case 1 :pic_i=R.drawable.pic_1;break;
            case 2 :pic_i=R.drawable.pic_2;break;
            case 3 :pic_i=R.drawable.pic_3;break;
            case 4 :pic_i=R.drawable.pic_4;break;
            case 5 :pic_i=R.drawable.pic_5;break;
            case 6 :pic_i=R.drawable.pic_6;break;
            case 7 :pic_i=R.drawable.pic_7;break;
            case 8 :pic_i=R.drawable.pic_8;break;
            case 9 :pic_i=R.drawable.pic_9;break;
            default:pic_i=R.drawable.pic_1;break;
        }
        markerOptions.icon(BitmapDescriptorFactory.fromResource(pic_i));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    private void drawLine(LatLng point){
        mapPointList.add(point);
        if (mapPointList.size()>1){
            aMap.addPolyline(new PolylineOptions().
                    addAll(mapPointList).width(10).color(Color.YELLOW));
        }
    }

    //这里点击地图，会添加路径点
    @Override
    public void onMapClick(LatLng point) {
        if (isAdd){
            pointNum++;
            markWaypoint(point);//按一下，显示一个新的点
            drawLine(point);
            double []coord = GCJ2WG(point.latitude,point.longitude);
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
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    private void setResultToToast(final String string){
        ConfigActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ConfigActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double []GCJ2WG(double lat, double lng){
        double []coor = BDToGPS.gcj2WGSExactly(lat,lng);
        return coor;
    }



    // 世界坐标转为火星坐标
    private LatLng WG2GCJ(LatLng latLng){
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(latLng);
        return converter.convert();
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

    //上传路径飞行任务到飞机
    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                    getMApplication().setPointList(mapPointList);
                    finish();
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });
    }

    /**
     * 每一个拍照点进行拍照作业
     */
    private void takePhotoAtPoint(){

    }

    private MApplication getMApplication(){
        return (MApplication)getApplicationContext();
    }
}
