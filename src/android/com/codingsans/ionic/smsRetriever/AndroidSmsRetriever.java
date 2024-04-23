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
import static android.content.Context.RECEIVER_EXPORTED;

import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class AndroidSmsRetriever extends CordovaPlugin {

  private SmsRetrieverClient smsRetrieverClient;
  //public static final int MAX_TIMEOUT = 300000; // 5 mins in millis
  private static final String TAG = AndroidSmsRetriever.class.getSimpleName();

  private static final String HASH_TYPE = "SHA-256";
  public static final int NUM_HASHED_BYTES = 9;
  public static final int NUM_BASE64_CHAR = 11;

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

  /**
   * Get all the app signatures for the current package
   * @return
   */
  public ArrayList<String> getAppSignatures() {
    ArrayList<String> appCodes = new ArrayList<>();

    try {
      // Get all package signatures for the current package
      String packageName = cordova.getActivity().getApplicationContext().getPackageName();
      PackageManager packageManager = cordova.getActivity().getApplicationContext().getPackageManager();
      Signature[] signatures = packageManager.getPackageInfo(packageName,
              PackageManager.GET_SIGNATURES).signatures;

      // For each signature create a compatible hash
      for (Signature signature : signatures) {
        String hash = hash(packageName, signature.toCharsString());
        if (hash != null) {
          appCodes.add(String.format("%s", hash));
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Unable to find package to obtain hash.", e);
    }
    return appCodes;
  }

  private static String hash(String packageName, String signature) {
    String appInfo = packageName + " " + signature;
    try {
      MessageDigest messageDigest = MessageDigest.getInstance(HASH_TYPE);
      messageDigest.update(appInfo.getBytes(StandardCharsets.UTF_8));
      byte[] hashSignature = messageDigest.digest();

      // truncated into NUM_HASHED_BYTES
      hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES);
      // encode into Base64
      String base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING | Base64.NO_WRAP);
      base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR);

      Log.d(TAG, String.format("pkg: %s -- hash: %s", packageName, base64Hash));

      return base64Hash;
    } catch (NoSuchAlgorithmException e) {
      Log.e(TAG, "hash:NoSuchAlgorithm", e);
    }
    return null;
  }

  @Override
  public void onDestroy() {
    try {
      if (mMessageReceiver != null) {
        this.cordova.getActivity().getApplicationContext().unregisterReceiver(mMessageReceiver);
        mMessageReceiver = null;
      }
    } catch(IllegalArgumentException e) {
        e.printStackTrace();
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;

    LOG.v(TAG, "Executing action: " + action);
    //Toast.makeText(this.cordova.getActivity().getApplicationContext(),"Executing action: " + action, Toast.LENGTH_SHORT).show();

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
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cordova.getActivity().getApplicationContext().registerReceiver(mMessageReceiver, intentFilter, RECEIVER_EXPORTED);
          } else {
            cordova.getActivity().getApplicationContext().registerReceiver(mMessageReceiver, intentFilter);
          }

        }
      });

      task.addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(Exception e) {
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
    else if ("hash".equals(action)) {

      ArrayList<String> strHashCodes = getAppSignatures();

      if(strHashCodes.size() == 0 || strHashCodes.get(0) == null){

        String err = "Unable to find package to obtain hash code of application";
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, err);
        callbackContext.sendPluginResult(result);

      } else {

        String strApplicationHash = strHashCodes.get(0);
        PluginResult result = new PluginResult(PluginResult.Status.OK, strApplicationHash);
        callbackContext.sendPluginResult(result);

      }
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
              if(message == null) return;

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
              break;
          }
        }
      }
    };
  };
}
