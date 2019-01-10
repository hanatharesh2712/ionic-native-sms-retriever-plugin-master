package com.codingsans.ionic.smsRetriever;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.Context;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import static com.google.android.gms.common.api.CommonStatusCodes.*;

public class AndroidSmsRetriever extends CordovaPlugin {

    private SmsRetrieverClient smsRetrieverClient;
    //private SmsBrReceiver smsReceiver;
    //Context context = null;
    public static final int MAX_TIMEOUT = 300000; // 5 mins in millis
    private static final String TAG = "SmsRetriever";
    //private SensorManager mSensorManager;
    //private Sensor accelerometer;
    private CallbackContext callbackContext;
    private JSONObject data = new JSONObject();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "SmsRetriever: initialization");
        //Toast.makeText(this.cordova.getActivity().getApplicationContext(),"SmsRetriever: initialization", Toast.LENGTH_SHORT).show();

        super.initialize(cordova, webView);

        // Get an instance of SmsRetrieverClient, used to start listening for a matching
        // SMS message.
        smsRetrieverClient = SmsRetriever.getClient(this.cordova.getActivity().getApplicationContext());
    }

    @Override
    public void onDestroy() {
        //mSensorManager.unregisterListener(listener);

        if (mMessageReceiver != null) {
            this.cordova.getActivity().getApplicationContext().unregisterReceiver(mMessageReceiver);
            mMessageReceiver = null;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        LOG.v(TAG, "Executing action: " + action);
        //Toast.makeText(this.cordova.getActivity().getApplicationContext(),"Executing action: " + action, Toast.LENGTH_SHORT).show();

        AppSignatureHelper appSignatureHelper = new AppSignatureHelper(this.cordova.getActivity().getApplicationContext());
        ArrayList<String> sList = appSignatureHelper.getAppSignatures();

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
                     LOG.v(TAG, "Executing action: addOnSuccessListener");
                     //Toast.makeText(cordova.getActivity().getApplicationContext(),"Executing action: addOnSuccessListener", Toast.LENGTH_SHORT).show();

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);
                    cordova.getActivity().getApplicationContext().registerReceiver(mMessageReceiver, intentFilter);
                }
            });

            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Failed to start retriever, inspect Exception for more details
                    LOG.v(TAG, "Executing action: addOnFailureListener");
                    //Toast.makeText(cordova.getActivity().getApplicationContext(),"Executing action: addOnFailureListener", Toast.LENGTH_SHORT).show();
                }
            });

          PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
          r.setKeepCallback(true);
          callbackContext.sendPluginResult(r);

          return true;


        }
        // Returning false results in a "MethodNotFound" error.
        return false;
    }

  // Our handler for received Intents. This will be called whenever an Intent
  // with an action named "custom-event-name" is broadcasted.
  private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      if (intent.getAction().equals(SmsRetriever.SMS_RETRIEVED_ACTION)) {
        final Bundle extra = intent.getExtras();
        if (extra != null && extra.containsKey(SmsRetriever.EXTRA_STATUS)) {
          Status status = (Status) extra.get(SmsRetriever.EXTRA_STATUS);
          switch (status.getStatusCode()) {
            case CommonStatusCodes.SUCCESS:
              final String message = extra.getString(SmsRetriever.EXTRA_SMS_MESSAGE);
              //if (!StringUtils.hasContent(message)) return;

              Log.d(TAG, message);

              data = new JSONObject();
              try {
              data.put("Message",message);
              } catch(JSONException e) {}

              //Toast.makeText(cordova.getActivity().getApplicationContext(),"Message: "+ message, Toast.LENGTH_LONG).show();
              PluginResult result = new PluginResult(PluginResult.Status.OK, data);
              callbackContext.sendPluginResult(result);

              break;
            case CommonStatusCodes.TIMEOUT:

              PluginResult resultTimeout = new PluginResult(PluginResult.Status.ERROR, "TIMEOUT");
              callbackContext.sendPluginResult(resultTimeout);
              //if (mListener != null) mListener.timeOut();
              break;
          }
        }
      }
    };
  };
}
