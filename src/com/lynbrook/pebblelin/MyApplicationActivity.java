package com.lynbrook.pebblelin;

import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.lynbrook.pebblin.R;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

/**
 * MyApplicationActivity - The starting point for creating your own Hue App. Currently contains a
 * simple view with a button to change your lights to random colours. Remove this and add your own
 * app implementation here! Have fun!
 * 
 * @author SteveyO
 * 
 */
public class MyApplicationActivity extends Activity {
  private PHHueSDK phHueSDK;
  private static final int MAX_HUE = 65535;
  public static final String TAG = "QuickStart";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(R.string.app_name);
    setContentView(R.layout.activity_main);
    phHueSDK = PHHueSDK.create();
    Button randomButton;
    randomButton = (Button) findViewById(R.id.buttonRand);
    randomButton.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        randomLights();
      }

    });

  }

  public void randomLights() {
    PHBridge bridge = phHueSDK.getSelectedBridge();

    List<PHLight> allLights = bridge.getResourceCache().getAllLights();
    Random rand = new Random();

    for (PHLight light : allLights) {
      PHLightState lightState = new PHLightState();
      lightState.setHue(rand.nextInt(MAX_HUE));
      // To validate your lightstate is valid (before sending to the bridge) you can use:
      // String validState = lightState.validateState();
      bridge.updateLightState(light, lightState, listener);
      // bridge.updateLightState(light, lightState); // If no bridge response is required then use
      // this simpler form.
    }
  }

  // If you want to handle the response from the bridge, create a PHLightListener object.
  PHLightListener listener = new PHLightListener() {

    public void onSuccess() {}

    public void onStateUpdate(Hashtable<String, String> arg0, List<PHHueError> arg1) {
      Log.w(TAG, "Light has updated");
    }

    public void onError(int arg0, String arg1) {}
  };

  @Override
  protected void onDestroy() {
    PHBridge bridge = phHueSDK.getSelectedBridge();
    if (bridge != null) {

      if (phHueSDK.isHeartbeatEnabled(bridge)) {
        phHueSDK.disableHeartbeat(bridge);
      }

      phHueSDK.disconnect(bridge);
      super.onDestroy();
    }
  }
}
