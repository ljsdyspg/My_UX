package com.test.spg.my_ux;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.useraccount.UserAccountManager;

public class GetLocation extends Activity implements View.OnClickListener{
    protected static final String TAG = "Activity";
    private double aircraftLat = 181, aircraftLng = 181,aircraftHeight=181;
    private FlightController mFlightController;

    private Button getLoc,getMap;
    private TextView LocInfo;

    private void initUI(){
        getLoc = findViewById(R.id.GetLoc);
        getMap = findViewById(R.id.ShowOnMap);
        LocInfo = findViewById(R.id.LocInf);

        getLoc.setOnClickListener(this);
        getMap.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_location);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);//注册广播接受器,接受连接状态变化的广播

        initUI();
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
        loginAccount();
    }


    private void initFlightController() {

        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            Toast.makeText(this,"获取遥控",Toast.LENGTH_LONG).show();
            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            aircraftLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            aircraftLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            aircraftHeight = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                        }
                    });
            String a = "get: "+aircraftHeight;
            Toast.makeText(this,a,Toast.LENGTH_LONG).show();
        }
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"+ error.getDescription());
                    }
                });
    }
    private void setResultToToast(final String string){
        GetLocation.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GetLocation.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.GetLoc:
                initFlightController();
                String result = "经度："+ aircraftLat+"\n纬度：" + aircraftLng+"\n高度："+aircraftHeight;
                LocInfo.setText(result);
                break;
            case R.id.ShowOnMap:
                break;
            default:
                break;
        }
    }
}
