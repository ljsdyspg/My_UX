package com.test.spg.my_ux;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class LoginActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "LoginActivity";
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    // 程序是否启动
    private static boolean isAppStarted = false;

    private Button btn_login;
    private Button btn_jump;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();
        isAppStarted = true;
        checkAndRequestPermissions();
    }

    private DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError error) {
            isRegistrationInProgress.set(false);
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();

                Toast.makeText(getApplicationContext(), "SDK registration succeeded!", Toast.LENGTH_LONG).show();
            } else {

                Toast.makeText(getApplicationContext(),
                        "SDK registration failed, check network and retry!",
                        Toast.LENGTH_LONG).show();
            }
        }
        @Override
        public void onProductDisconnect() {
            Toast.makeText(getApplicationContext(),
                    "product disconnect!",
                    Toast.LENGTH_LONG).show();
        }
        @Override
        public void onProductConnect(BaseProduct product) {
            Toast.makeText(getApplicationContext(),
                    "product connect!",
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key,
                                      BaseComponent oldComponent,
                                      BaseComponent newComponent) {
            Toast.makeText(getApplicationContext(),
                    key.toString() + " changed",
                    Toast.LENGTH_LONG).show();

        }
    };

    // 程序需要的所有控件
    private static final String[] REQUIRED_PERMISSION_LIST = new String[] {
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;
    // 保存未获取到的权限
    private List<String> missingPermission = new ArrayList<>();

    public static boolean isStarted() {
        return isAppStarted;
    }

    /**
     *  初始化控件
     */
    private void init(){
        btn_login = findViewById(R.id.btn_login);
        btn_jump = findViewById(R.id.btn_jump);
        btn_login.setOnClickListener(this);
        btn_jump.setOnClickListener(this);
    }

    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            Toast.makeText(getApplicationContext(), "Missing permissions! Will not register SDK to connect to aircraft.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 注册SDK
     */
    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().registerApp(LoginActivity.this, registrationCallback);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        DJISDKManager.getInstance().destroy();
        isAppStarted = false;
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            default:break;
            case R.id.btn_login:
                loginAccount();
                //startActivity(new Intent(this, GetLocation.class));
                break;
            case R.id.btn_jump:
                Intent intent = new Intent(this,MainActivity.class);
                startActivity(intent);
                break;
        }
    }

    // 登录账号
    private void loginAccount(){
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Toast.makeText(LoginActivity.this, "Login Success", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        Toast.makeText(LoginActivity.this, "Login Error:" + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
