package com.codingsans.ionic.smsRetriever;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.Context;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.hardware.Sensor;

public class AndroidSmsRetriever extends CordovaPlugin {
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private CallbackContext callbackContext;
    private JSONObject data = new JSONObject();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        mSensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(listener);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        
        if ("start".equals(action)) {
            mSensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        } else if ("stop".equals(action)) {
            mSensorManager.unregisterListener(listener);
        } else if ("getCurrent".equals(action)) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, this.data);
            callbackContext.sendPluginResult(result);
            return true;
        }
        return false;  // Returning false results in a "MethodNotFound" error.
    }

    private SensorEventListener listener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
          if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
              data = new JSONObject();
              try {
                  data.put("x", event.values[0]);
                  data.put("y", event.values[1]);
                  data.put("z", event.values[2]);
              } catch(JSONException e) {}
          }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // unused
        }
    };
}
