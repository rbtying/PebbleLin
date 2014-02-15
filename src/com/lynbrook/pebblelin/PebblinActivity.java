/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lynbrook.pebblelin;

import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.levien.synthesizer.core.midi.MidiListener;
import com.lynbrook.pebblin.R;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

/**
 * Activity for simply playing the piano. This version is hacked up to send MIDI to the C++ engine.
 * This needs to be refactored to make it cleaner.
 */
public class PebblinActivity extends SynthActivity implements OnSharedPreferenceChangeListener {

  private MidiListener midi = null;

  private boolean playing = false;
  private boolean notrunning = true;

  private PHHueSDK phHueSDK;
  private static final int MAX_HUE = 65535;

  private int v[] = new int[3];
  private int g[] = new int[3];
  private int a[] = new int[3];

  private final static UUID PEBBLE_APP_UUID = UUID
          .fromString("ee0315aa-8439-48b6-a622-a05a0a99c640");

  private PebbleKit.PebbleDataReceiver dataReceiver;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d("synth", "activity onCreate " + getIntent());
    super.onCreate(savedInstanceState);
    getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.pebblelin);
    phHueSDK = PHHueSDK.create();

    presetSpinner_ = (Spinner) findViewById(R.id.presetSpinner);

    Button b = (Button) findViewById(R.id.notebutton);
    b.setOnClickListener(new OnClickListener() {

      public void onClick(View arg0) {
        randomLights();
        Log.v("Pebblin", "click");
        if (midi != null) {
          if (playing) {
            midi.onNoteOff(0, 69, 0);
            playing = false;
          } else {
            midi.onNoteOn(0, 69, 127);
            playing = true;
          }
        }
      }
    });
  }

  // If you want to handle the response from the bridge, create a PHLightListener object.
  PHLightListener listener = new PHLightListener() {

    public void onSuccess() {}

    public void onStateUpdate(Hashtable<String, String> arg0, List<PHHueError> arg1) {
      Log.w("HI", "Light has updated");
      notrunning = true;
    }

    public void onError(int arg0, String arg1) {}
  };

  public void randomLights() {
    notrunning = false;
    PHBridge bridge = phHueSDK.getSelectedBridge();

    if (bridge != null) {
      List<PHLight> allLights = bridge.getResourceCache().getAllLights();
      Random rand = new Random();

      PHLightState lightState = new PHLightState();
      lightState.setHue(rand.nextInt(MAX_HUE));
      lightState.setTransitionTime(1);
      // To validate your lightstate is valid (before sending to the bridge) you can use:
      // String validState = lightState.validateState();
      bridge.updateLightState(allLights.get(0), lightState, listener);
      // bridge.updateLightState(light, lightState); // If no bridge response is required then use
      // this simpler form.
    }
  }

  @Override
  protected void onDestroy() {
    Log.d("synth", "activity onDestroy");
    PHBridge bridge = phHueSDK.getSelectedBridge();
    if (bridge != null) {

      if (phHueSDK.isHeartbeatEnabled(bridge)) {
        phHueSDK.disableHeartbeat(bridge);
      }

      phHueSDK.disconnect(bridge);
      super.onDestroy();
    }
    super.onDestroy();
  }

  private int pval = 0;
  private int note = 62;

  private void processSignal() {
    int val = v[1];

    double angle = -Math.atan2(g[2], g[1]);

    if (Math.abs(val) < 5) {
      val = 0;
    }
    if (pval * val <= 0) {
      midi.onNoteOff(0, note, 0);

      if (angle < 0) {
        return;
      } else if (angle > Math.PI / 2 + Math.PI / 6) {
        note = 66;
      } else if (angle < Math.PI / 2 - Math.PI / 6) {
        note = 62;
      } else {
        note = 64;
      }

      // if (pval * val < 0) {
      midi.onNoteOn(0, note, Math.abs(val) * 2);
      if (notrunning) {
        randomLights();
      }
      // }
    }
    pval = val;

    Log.v("ANGLE", angle + "");
  }

  @Override
  protected void onResume() {
    super.onResume();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.registerOnSharedPreferenceChangeListener(this);
    onSharedPreferenceChanged(prefs, "keyboard_type");
    onSharedPreferenceChanged(prefs, "vel_sens");

    dataReceiver = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
      @Override
      public void receiveData(final Context context, final int transactionID,
              final PebbleDictionary data) {
        PebbleKit.sendAckToPebble(context, transactionID);
        int[] arr = new int[9];
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 9; i++) {
          Long v = data.getInteger(i);
          if (v != null) {
            arr[i] = v.intValue();
          }
          buf.append(arr[i] + ",");

        }
        for (int i = 0; i < 3; i++) {
          v[i] = arr[i];
          a[i] = arr[i + 3];
          g[i] = arr[i + 6];
        }
        processSignal();
        Log.v("YAYTAG", buf.toString());
      }
    };
    PebbleKit.registerReceivedDataHandler(this, dataReceiver);
    startWatchApp(null);
  }

  public void startWatchApp(View view) {
    PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
  }

  public void stopWatchApp(View view) {
    PebbleKit.closeAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
  }

  @Override
  protected void onPause() {
    super.onPause();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.unregisterOnSharedPreferenceChangeListener(this);
    if (dataReceiver != null) {
      unregisterReceiver(dataReceiver);
      dataReceiver = null;
    }
  }

  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {}

  @Override
  protected void onNewIntent(Intent intent) {
    Log.d("synth", "activity onNewIntent " + intent);
  }

  public void sendMidiBytes(byte[] buf) {
    // TODO: in future we'll want to reflect MIDI to UI (knobs turn, keys press)
    if (synthesizerService_ != null) {
      synthesizerService_.sendRawMidi(buf);
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  protected void onSynthConnected() {
    midi = synthesizerService_.getMidiListener();

    // Populate patch names (note: we could update an existing list rather than
    // creating a new adapter, but it probably wouldn't save all that much).
    if (presetSpinner_.getAdapter() == null) {
      // Only set it once, which is a workaround that allows the preset
      // selection to persist for onCreate lifetime. Of course, it should
      // be persisted for real, instead.
      List<String> patchNames = synthesizerService_.getPatchNames();

      ArrayAdapter<String> adapter = new ArrayAdapter<String>(PebblinActivity.this,
              android.R.layout.simple_spinner_item, patchNames);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      presetSpinner_.setAdapter(adapter);

      int i = 0;
      for (String s : patchNames) {
        if (s.contains("STRINGS")) {
          break;
        }
        i++;
      }
      presetSpinner_.setSelection(i);
    }

    presetSpinner_.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        synthesizerService_.getMidiListener().onProgramChange(0, position);
      }

      public void onNothingSelected(AdapterView<?> parent) {}
    });

  }

  protected void onSynthDisconnected() {
    synthesizerService_.setMidiListener(null);
  }

  private Spinner presetSpinner_;
}
