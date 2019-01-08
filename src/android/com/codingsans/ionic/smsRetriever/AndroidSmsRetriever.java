package com.codingsans.ionic.smsRetriever;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.Context;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.common.api.CommonStatusCodes;

public class AndroidSmsRetriever extends CordovaPlugin {
    private SmsRetrieverClient smsRetrieverClient;
    private SmsBrReceiver smsReceiver;

    public static final int MAX_TIMEOUT = 300000; // 5 mins in millis
    private static final String TAG = "SmsRetriever";
    //private SensorManager mSensorManager;
    //private Sensor accelerometer;
    private CallbackContext callbackContext;
    private JSONObject data = new JSONObject();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "SmsRetriever: initialization");
        Toast.makeText(TAG,"SmsRetriever: initialization", Toast.LENGTH_SHORT).show();

        super.initialize(cordova, webView);

        if (smsReceiver == null) {
            smsReceiver = new SmsBrReceiver();
        }

        // Get an instance of SmsRetrieverClient, used to start listening for a matching
        // SMS message.
        smsRetrieverClient = SmsRetriever.getClient(this /* context */);

        //mSensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
        //accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onDestroy() {
        //mSensorManager.unregisterListener(listener);

        if (smsReceiver != null) {
            getApplicationContext().unregisterReceiver(smsReceiver);
            smsReceiver.cancelTimeout();
            smsReceiver = null;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.v(TAG, "Executing action: " + action);
        Toast.makeText(TAG,"Executing action: " + action, Toast.LENGTH_SHORT).show();

        if ("start".equals(action)) {
            // Starts SmsRetriever, which waits for ONE matching SMS message until timeout
            // (5 minutes). The matching SMS message will be sent via a Broadcast Intent with
            // action SmsRetriever#SMS_RETRIEVED_ACTION.
            Task<Void> task = smsRetrieverClient.startSmsRetriever();

            // Listen for success/failure of the start Task. If in a background thread, this
            // can be made blocking using Tasks.await(task, [timeout]);
            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // Successfully started retriever, expect broadcast intent
                    // ...
                     LOG.v(TAG, "Executing action: addOnSuccessListener");
                     Toast.makeText(TAG,"Executing action: addOnSuccessListener", Toast.LENGTH_SHORT).show();

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);
                    getApplicationContext().registerReceiver(smsReceiver, intentFilter);
                }
            });

            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Failed to start retriever, inspect Exception for more details
                    // ...
                    LOG.v(TAG, "Executing action: addOnFailureListener");
                    Toast.makeText(TAG,"Executing action: addOnFailureListener", Toast.LENGTH_SHORT).show();
                }
            });
            //mSensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        } else if ("stop".equals(action)) {
            //mSensorManager.unregisterListener(listener);

        } else if ("getCurrent".equals(action)) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, this.data);
            callbackContext.sendPluginResult(result);
            return true;
        }
        return false;  // Returning false results in a "MethodNotFound" error.
    }
}

class SmsBrReceiver extends BroadcastReceiver {
    Handler h = new Handler();
    Runnable r = new Runnable() {
        @Override
        public void run() {
            doTimeout();
        }
    };

    public void setTimeout() {
        h.postDelayed(r, MAX_TIMEOUT);
    }

    public void cancelTimeout() {
        h.removeCallbacks(r);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(action)) {
            cancelTimeout();
            // notifyStatus(STATUS_RESPONSE_RECEIVED, null);
            Bundle extras = intent.getExtras();
            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
            switch(status.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    LOG.v(TAG, "Retrieved sms");
                    Toast.makeText(TAG,"Retrieved sms", Toast.LENGTH_SHORT).show();
                    
                    String smsMessage = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                    //Log.d(TAG, "Retrieved sms code: " + smsMessage);
                    if (smsMessage != null) {
                        //verifyMessage(smsMessage);
                        data = new JSONObject();
                        try {
                            data.put("Message",smsMessage);
                        } catch(JSONException e) {}
                    }
                    break;
                case CommonStatusCodes.TIMEOUT:
                    doTimeout();
                    break;
                default:
                    break;
            }
        }
    }

    private void doTimeout() {
        LOG.v(TAG, "Waiting for sms timed out");
        Toast.makeText(TAG,"Waiting for sms timed out", Toast.LENGTH_SHORT).show();
        //Log.d(TAG, "Waiting for sms timed out.");
        //Toast.makeText(PhoneNumberVerifier.this, getString(R.string.toast_unverified), Toast.LENGTH_LONG).show();
        stopSelf();
    }
}