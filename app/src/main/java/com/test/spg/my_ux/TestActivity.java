package com.test.spg.my_ux;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class TestActivity extends Activity {

    Button test_btn;
    TextView test_text;
    private double droneLocationLat = 181, droneLocationLng = 181;//181超出180的范围，所以设置该初值
    private FlightController mFlightController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);//注册广播接受器,接受连接状态变化的广播


        test_text = findViewById(R.id.test_text);
        test_btn = findViewById(R.id.test_btn);
        test_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String a = "";
                test_text.setText(a);
            }
        });
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
            Toast.makeText(this,"获取遥控",Toast.LENGTH_LONG).show();
            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                        }
                    });
            String a = "lat: "+droneLocationLat+" lng: "+droneLocationLng;
            Toast.makeText(this,a,Toast.LENGTH_SHORT).show();
        }
    }


}
