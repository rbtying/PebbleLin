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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.levien.synthesizer.core.midi.MidiListener;
import com.levien.synthesizer.core.music.Note;
import com.lynbrook.pebblin.R;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import com.philips.lighting.model.PHLight.PHLightAlertMode;

/**
 * Activity for simply playing the piano. This version is hacked up to send MIDI to the C++ engine.
 * This needs to be refactored to make it cleaner.
 */
public class PebblinActivity extends SynthActivity implements OnSharedPreferenceChangeListener,
        OnTouchListener {

  private PHLightListener listener = new PHLightListener() {

    public void onSuccess() {
      Log.v("LIGHT", "Light written");

    }

    public void onStateUpdate(Hashtable<String, String> arg0, List<PHHueError> arg1) {}

    public void onError(int arg0, String arg1) {
      Log.v("LIGHT", arg1);

    }
  };

  private MidiListener midi = null;

  private PHHueSDK phHueSDK;

  private int v[] = new int[3];
  private int g[] = new int[3];
  private int a[] = new int[3];

  private Button[] buttons;
  private boolean buttonpressed[] = new boolean[4];
  private int p_val = 0;
  private int note_idx = 0;

  private int notes[] = { 60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84, 86, 88, 89,
          91, 93, 95, 96, 98, 100 };

  private int mary_had_a_little_lamb[] = { 2, 1, 0, 1, 2, 2, 2, 1, 1, 1, 2, 4, 4, 2, 1, 0, 1, 2, 2,
          2, 2, 1, 1, 2, 1, 0 };
  private int tutorial_ptr = 0;

  private boolean tutorial_mode = true;

  private int BOW_FLAT = 46920;
  private int BOW_TILT = 25500;

  private final static UUID PEBBLE_APP_UUID = UUID
          .fromString("ee0315aa-8439-48b6-a622-a05a0a99c640");

  private PebbleKit.PebbleDataReceiver dataReceiver;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d("synth", "activity onCreate " +  getIntent());
    tutorial_mode = getIntent().getExtras().getBoolean("toggle");
    setTitle(R.string.app_name);
    super.onCreate(savedInstanceState);
    getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.pebblelin);
    phHueSDK = PHHueSDK.create();

    presetSpinner_ = (Spinner) findViewById(R.id.presetSpinner);

    buttons = new Button[4];
    buttons[0] = (Button) findViewById(R.id.Button1);
    buttons[1] = (Button) findViewById(R.id.Button2);
    buttons[2] = (Button) findViewById(R.id.Button3);
    buttons[3] = (Button) findViewById(R.id.Button4);

    for (int i = 0; i < buttons.length; i++) {
      buttons[i].setOnTouchListener(this);
    }

    updateLightbulb();
  }

  public void setLightbulbForTutorial(int nextnote) {
    PHBridge bridge = phHueSDK.getSelectedBridge();
    boolean bow_tilt = nextnote >= 4;
    int idx = nextnote % 4;
    int i = 0;

    if (bridge != null) {
      Log.v("LIGHTS", "UPDAITING DA LIGHTSES");
      List<PHLight> allLights = bridge.getResourceCache().getAllLights();

      for (PHLight light : allLights) {
        PHLightState lightState = new PHLightState();
        lightState.setTransitionTime(0);
        if (idx == i) {
          lightState.setSaturation(254);
          lightState.setBrightness(254);
          if (bow_tilt) {
            lightState.setHue(BOW_TILT);
          } else {
            lightState.setHue(BOW_FLAT);
          }
          lightState.setOn(true);
        } else {
          lightState.setOn(false);
        }
        // lightState.setAlertMode(PHLightAlertMode.ALERT_SELECT);
        bridge.updateLightState(light, lightState, listener);
        i++;
      }
    }
  }

  public void setLightbulbForPlay() {
    PHBridge bridge = phHueSDK.getSelectedBridge();
    boolean bow_tilt = note_idx >= 4;
    int idx = note_idx % 4;
    int i = 0;

    if (bridge != null) {
      List<PHLight> allLights = bridge.getResourceCache().getAllLights();

      for (PHLight light : allLights) {
        PHLightState lightState = new PHLightState();
        lightState.setOn(true);
        lightState.setTransitionTime(1);
        if (idx == i) {
          lightState.setSaturation(254);
          lightState.setBrightness(254);
          if (bow_tilt) {
            lightState.setHue(BOW_TILT);
          } else {
            lightState.setHue(BOW_FLAT);
          }
        } else {
          lightState.setHue(0);
        }
        // lightState.setAlertMode(PHLightAlertMode.ALERT_SELECT);
        bridge.updateLightState(light, lightState, listener);
        i++;
      }
    }
  }

  public void flashLightbulb(int nextnote) {
    PHBridge bridge = phHueSDK.getSelectedBridge();
    int idx = nextnote % 4;
    if (bridge != null) {
      List<PHLight> allLights = bridge.getResourceCache().getAllLights();
      PHLightState lightState = new PHLightState();
      lightState.setAlertMode(PHLightAlertMode.ALERT_SELECT);
      lightState.setTransitionTime(0);
      // lightState.setHue(0);
      bridge.updateLightState(allLights.get(idx), lightState, listener);
    }
  }

  public void updateLightbulb() {
    if (tutorial_mode) {
      Log.v("LIGHT", note_idx + " " + mary_had_a_little_lamb[tutorial_ptr]);
      if (note_idx == mary_had_a_little_lamb[tutorial_ptr]) {
        tutorial_ptr++;
        if (tutorial_ptr >= mary_had_a_little_lamb.length) {
          tutorial_mode = false;
          return;
        }
      } else {
        flashLightbulb(mary_had_a_little_lamb[tutorial_ptr]);
      }
      setLightbulbForTutorial(mary_had_a_little_lamb[tutorial_ptr]);
    } else {
      setLightbulbForPlay();
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

  private void processSignal() {
    if (midi == null) {
      return;
    }
    int val = v[1];

    double angle = -Math.atan2(g[2], g[1]);

    if (Math.abs(val) < 5) {
      val = 0;
    }

    if (p_val * val <= 0) {
      midi.onNoteOff(0, notes[note_idx], 0);

      int k = 0;

      if (angle > Math.PI / 2 + Math.PI / 8) {
        k = 1;
      } else {
        k = 0;
      }

      boolean nobuttonpressed = true;
      for (int i = 0; i < buttonpressed.length; i++) {
        if (buttonpressed[i]) {
          if (k * buttonpressed.length + i < notes.length) {
            note_idx = k * buttonpressed.length + i;
            nobuttonpressed = false;
            break;
          }
        }
      }
      if (nobuttonpressed) {
        val = 0;
      }

      midi.onNoteOn(0, notes[note_idx], Math.abs(val) * 3);

      if (val != 0) {
        String name = Note.getName(notes[note_idx] % 12);
        Log.v("NOTE", name);

        updateLightbulb();
        ((TextView) findViewById(R.id.noteName)).setText(name);
      } else {
        ((TextView) findViewById(R.id.noteName)).setText("");
      }

    }
    p_val = val;
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
        Log.v("DATA RECEIVER", buf.toString());
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

  public boolean onTouch(View v, MotionEvent e) {
    for (int i = 0; i < buttons.length; i++) {
      if (v.getId() == buttons[i].getId()) {
        switch (e.getAction()) {
          case MotionEvent.ACTION_UP:
            buttonpressed[i] = false;
            processSignal();

            break;
          case MotionEvent.ACTION_DOWN:
            buttonpressed[i] = true;
            processSignal();
            break;
        }
      }
    }
    return false;
  }
}
