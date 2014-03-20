package com.lynbrook.pebblelin;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.lynbrook.pebblin.R;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;

/**
 * PHHomeActivity - The starting point in your own Hue App.
 * 
 * For first time use, a Bridge search (UPNP) is performed and a list of all available bridges is
 * displayed (and clicking one of them shows the PushLink dialog allowing authentication). The last
 * connected Bridge IP Address and Username are stored in SharedPreferences.
 * 
 * For subsequent usage the app automatically connects to the last connected bridge. When connected
 * the MyApplicationActivity Activity is started. This is where you should start implementing your
 * Hue App! Have fun!
 * 
 * For explanation on key concepts visit:
 * https://github.com/PhilipsHue/PhilipsHueSDK-Java-MultiPlatform-Android
 * 
 * @author SteveyO
 * 
 */
public class PHHomeActivity extends Activity implements OnItemClickListener {

  private PHHueSDK phHueSDK;
  public static final String TAG = "QuickStart";
  private HueSharedPreferences prefs;
  private AccessPointListAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bridgelistlinear);

    // Gets an instance of the Hue SDK.
    phHueSDK = PHHueSDK.create();

    // Set the Device Name (name of your app). This will be stored in your bridge whitelist entry.
    phHueSDK.setDeviceName("Pebblin");

    // Register the PHSDKListener to receive callbacks from the bridge.
    phHueSDK.getNotificationManager().registerSDKListener(listener);

    adapter = new AccessPointListAdapter(getApplicationContext(), phHueSDK.getAccessPointsFound());

    ListView accessPointList = (ListView) findViewById(R.id.bridge_list);
    accessPointList.setOnItemClickListener(this);
    accessPointList.setAdapter(adapter);

    // Try to automatically connect to the last known bridge. For first time use this will be empty
    // so a bridge search is automatically started.
    prefs = HueSharedPreferences.getInstance(getApplicationContext());
    String lastIpAddress = prefs.getLastConnectedIPAddress();
    String lastUsername = prefs.getUsername();

    // Automatically try to connect to the last connected IP Address. For multiple bridge support a
    // different implementation is required.
    if (lastIpAddress != null && !lastIpAddress.equals("")) {
      PHAccessPoint lastAccessPoint = new PHAccessPoint();
      lastAccessPoint.setIpAddress(lastIpAddress);
      lastAccessPoint.setUsername(lastUsername);

      if (!phHueSDK.isAccessPointConnected(lastAccessPoint)) {
        PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting,
                PHHomeActivity.this);
        phHueSDK.connect(lastAccessPoint);
      }
    } else { // First time use, so perform a bridge search.
      doBridgeSearch();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Log.w(TAG, "Inflating home menu");
    // Inflate the menu; this adds items to the action bar if it is present.
    // getMenuInflater().inflate(R.menu.home, menu);
    return true;
  }

  // Local SDK Listener
  private PHSDKListener listener = new PHSDKListener() {

    public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
      Log.w(TAG, "Access Points Found. " + accessPoint.size());

      PHWizardAlertDialog.getInstance().closeProgressDialog();
      if (accessPoint != null && accessPoint.size() > 0) {
        phHueSDK.getAccessPointsFound().clear();
        phHueSDK.getAccessPointsFound().addAll(accessPoint);

        runOnUiThread(new Runnable() {
          public void run() {
            adapter.updateData(phHueSDK.getAccessPointsFound());
          }
        });
      } else {
        // FallBack Mechanism. If a UPNP Search returns no results then perform an IP Scan. Of
        // course it could fail as the user has disconnected their bridge, connected to a wrong
        // network or disabled Network Discovery on their router so it is not guaranteed to work.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
          PHWizardAlertDialog.getInstance().showProgressDialog(R.string.search_progress,
                  PHHomeActivity.this);
          PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK
                  .getSDKService(PHHueSDK.SEARCH_BRIDGE);
          // Start the IP Scan Search if the UPNP and NPNP return 0 results.
          sm.search(false, false, true);
        }
      }

    }

    public void onCacheUpdated(int flags, PHBridge bridge) {
      Log.w(TAG, "On CacheUpdated");

    }

    public void onBridgeConnected(PHBridge b) {
      phHueSDK.setSelectedBridge(b);
      phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
      phHueSDK.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(),
              System.currentTimeMillis());
      prefs.setLastConnectedIPAddress(b.getResourceCache().getBridgeConfiguration().getIpAddress());
      prefs.setUsername(prefs.getUsername());
      PHWizardAlertDialog.getInstance().closeProgressDialog();
      startMainActivity();
    }

    public void onAuthenticationRequired(PHAccessPoint accessPoint) {
      Log.w(TAG, "Authentication Required.");
      phHueSDK.startPushlinkAuthentication(accessPoint);
      startActivity(new Intent(PHHomeActivity.this, PHPushlinkActivity.class));

    }

    public void onConnectionResumed(PHBridge bridge) {
      if (PHHomeActivity.this.isFinishing()) return;

      Log.v(TAG, "onConnectionResumed"
              + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
      phHueSDK.getLastHeartbeat().put(
              bridge.getResourceCache().getBridgeConfiguration().getIpAddress(),
              System.currentTimeMillis());
      for (int i = 0; i < phHueSDK.getDisconnectedAccessPoint().size(); i++) {

        if (phHueSDK.getDisconnectedAccessPoint().get(i).getIpAddress()
                .equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
          phHueSDK.getDisconnectedAccessPoint().remove(i);
        }
      }

    }

    public void onConnectionLost(PHAccessPoint accessPoint) {
      Log.v(TAG, "onConnectionLost : " + accessPoint.getIpAddress());
      if (!phHueSDK.getDisconnectedAccessPoint().contains(accessPoint)) {
        phHueSDK.getDisconnectedAccessPoint().add(accessPoint);
      }
    }

    public void onError(int code, final String message) {
      Log.e(TAG, "on Error Called : " + code + ":" + message);

      if (code == PHHueError.NO_CONNECTION) {
        Log.w(TAG, "On No Connection");
      } else if (code == PHHueError.AUTHENTICATION_FAILED || code == 1158) {
        PHWizardAlertDialog.getInstance().closeProgressDialog();
      } else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
        Log.w(TAG, "Bridge Not Responding . . . ");
        PHWizardAlertDialog.getInstance().closeProgressDialog();
        PHHomeActivity.this.runOnUiThread(new Runnable() {
          public void run() {
            PHWizardAlertDialog.showErrorDialog(PHHomeActivity.this, message, R.string.btn_ok);
          }
        });

      } else if (code == PHMessageType.BRIDGE_NOT_FOUND) {
        PHWizardAlertDialog.getInstance().closeProgressDialog();

        PHHomeActivity.this.runOnUiThread(new Runnable() {
          public void run() {
            PHWizardAlertDialog.showErrorDialog(PHHomeActivity.this, message, R.string.btn_ok);
          }
        });
      }
    }
  };

  /**
   * Called when option is selected.
   * 
   * @param item
   *          the MenuItem object.
   * @return boolean Return false to allow normal menu processing to proceed, true to consume it
   *         here.
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // switch (item.getItemId()) {
    // case R.id.find_new_bridge:
    // doBridgeSearch();
    // break;
    // }
    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (listener != null) {
      phHueSDK.getNotificationManager().unregisterSDKListener(listener);
    }
    phHueSDK.disableAllHeartbeat();
  }

  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    HueSharedPreferences prefs = HueSharedPreferences.getInstance(getApplicationContext());
    PHAccessPoint accessPoint = (PHAccessPoint) adapter.getItem(position);
    accessPoint.setUsername(prefs.getUsername());

    PHBridge connectedBridge = phHueSDK.getSelectedBridge();

    if (connectedBridge != null) {
      String connectedIP = connectedBridge.getResourceCache().getBridgeConfiguration()
              .getIpAddress();
      if (connectedIP != null) { // We are already connected here:-
        phHueSDK.disableHeartbeat(connectedBridge);
        phHueSDK.disconnect(connectedBridge);
      }
    }
    PHWizardAlertDialog.getInstance().showProgressDialog(R.string.connecting, PHHomeActivity.this);
    phHueSDK.connect(accessPoint);
  }

  public void doBridgeSearch() {
    PHWizardAlertDialog.getInstance().showProgressDialog(R.string.search_progress,
            PHHomeActivity.this);
    PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK
            .getSDKService(PHHueSDK.SEARCH_BRIDGE);
    // Start the UPNP Searching of local bridges.
    sm.search(true, true);
  }

  // Starting the main activity this way, prevents the PushLink Activity being shown when pressing
  // the back button.
  public void startMainActivity() {
    Intent intent = new Intent(getApplicationContext(), PebblinActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra("toggle",getIntent().getExtras().getBoolean("toggle"));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) intent.addFlags(0x8000); // equal to
    startActivity(intent);
  }

}