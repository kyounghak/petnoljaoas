package com.chaigene.petnolja.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.util.CommonUtil;

public class CoreService extends Service {
    public static final String TAG = "CoreService";

    private SystemBroadcastHandler systemBroadcastHandler;
    private CustomBroadcastHandler customBroadcastHandler;
    private static CoreService mInstance;

    public static boolean isServiceRunning(Context context) {
        return CommonUtil.isServiceRunning(context, "com.wonsuc.officeworker.service.CoreService");
    }

    public static CoreService getInstance() {
        return CoreService.mInstance;
    }

    public static void startService(Context context) {
        Log.i(TAG, "startService");

        Context c = context.getApplicationContext();
        if (CoreService.isServiceRunning(c)) {
            Log.d(TAG, "startService, already running...");
            return;
        }
        Intent intent = new Intent(c, CoreService.class);
        c.startService(intent);
    }

    public static void stopService(Context context) {
        Log.i(TAG, "stopService");

        Context c = context.getApplicationContext();
        if (!CoreService.isServiceRunning(c)) return;
        Intent intent = new Intent(c, CoreService.class);
        c.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        stopSystemBroadcastHandler();
        stopCustomBroadcastHandler();

        sendBroadcast(new Intent(Constants.CORE_SERVICE_ACTION_DISABLED));

        // VolleyLoader.releaseInstance();
        // ConfigManager.releaseInstance();
        // DBHelper.releaseInstance();
        super.onDestroy();
    }

    /**
     * Intent로 Activity에 의한실행 or booting or start sticky 체크해야함..
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        // if (!VolleyLoader.isRunning()) VolleyLoader.createInstance(this);

        startSystemBroadcastHandler();
        startCustomBroadcastHandler();
        sendBroadcast(new Intent(Constants.CORE_SERVICE_ACTION_ENABLED));

        // AuthManager.getInstance().initAuthStateListener();

        return Service.START_STICKY;
    }

    private void onScreenOn() {
        // AuthManager.getInstance().initAuthStateListener();
    }

    private void onScreenOff() {
        // AuthManager.getInstance().releaseAuthStateListener();
    }

    private void startCustomBroadcastHandler() {
        Log.i(TAG, "startCustomBroadcastHandler");

        if (customBroadcastHandler != null) return;
        customBroadcastHandler = new CustomBroadcastHandler();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.MOBILE_DATA_MANUAL_INSERTED_ACTION);
        registerReceiver(customBroadcastHandler, intentFilter);
    }

    private void stopCustomBroadcastHandler() {
        if (customBroadcastHandler == null) return;
        unregisterReceiver(customBroadcastHandler);
        customBroadcastHandler = null;
    }

    private void startSystemBroadcastHandler() {
        Log.i(TAG, "startSystemBroadcastHandler");

        if (systemBroadcastHandler != null) return;
        systemBroadcastHandler = new SystemBroadcastHandler();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(systemBroadcastHandler, intentFilter);
    }

    private void stopSystemBroadcastHandler() {
        Log.i(TAG, "stopSystemBroadcastHandler");

        if (systemBroadcastHandler == null) return;
        unregisterReceiver(systemBroadcastHandler);
        systemBroadcastHandler = null;
    }

    private class CustomBroadcastHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "CustomBroadcastHandler, onReceive, action:" + action);

            if (action.equals(Constants.MOBILE_DATA_MANUAL_INSERTED_ACTION)) {
            }
        }
    }

    private class SystemBroadcastHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "SystemBroadcastHandler, onReceive, action:" + action);

            if (action.equals(Intent.ACTION_SCREEN_ON)) onScreenOn();
            else if (action.equals(Intent.ACTION_SCREEN_OFF)) onScreenOff();
        }
    }
}
