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

import java.util.List;

import android.annotation.TargetApi;
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

import com.levien.synthesizer.core.midi.MidiListener;
import com.lynbrook.pebblin.R;

/**
 * Activity for simply playing the piano. This version is hacked up to send MIDI to the C++ engine.
 * This needs to be refactored to make it cleaner.
 */
public class PebblinActivity extends SynthActivity implements OnSharedPreferenceChangeListener {

  private MidiListener midi = null;

  private boolean playing = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d("synth", "activity onCreate " + getIntent());
    super.onCreate(savedInstanceState);
    getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.pebblelin);

    // keyboard_ = (KeyboardView) findViewById(R.id.piano);
    // ScrollStripView scrollStrip_ = (ScrollStripView) findViewById(R.id.scrollstrip);
    // scrollStrip_.bindKeyboard(keyboard_);
    // cutoffKnob_ = (KnobView) findViewById(R.id.cutoffKnob);
    // resonanceKnob_ = (KnobView) findViewById(R.id.resonanceKnob);
    // overdriveKnob_ = (KnobView) findViewById(R.id.overdriveKnob);
    presetSpinner_ = (Spinner) findViewById(R.id.presetSpinner);

    Button b = (Button) findViewById(R.id.notebutton);
    b.setOnClickListener(new OnClickListener() {

      public void onClick(View arg0) {

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

  @Override
  protected void onDestroy() {
    Log.d("synth", "activity onDestroy");
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.registerOnSharedPreferenceChangeListener(this);
    onSharedPreferenceChanged(prefs, "keyboard_type");
    onSharedPreferenceChanged(prefs, "vel_sens");
  }

  @Override
  protected void onPause() {
    super.onPause();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.unregisterOnSharedPreferenceChangeListener(this);

  }

  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    // if (key.equals("keyboard_type")) {
    // String keyboardType = prefs.getString(key, "2row");
    // keyboard_.setKeyboardSpec(KeyboardSpec.make(keyboardType));
    // } else if (key.equals("vel_sens") || key.equals("vel_avg")) {
    // float velSens = prefs.getFloat("vel_sens", 0.5f);
    // float velAvg = prefs.getFloat("vel_avg", 64);
    // keyboard_.setVelocitySensitivity(velSens, velAvg);
    // }
  }

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

  // private PianoView piano_;
  // private KeyboardView keyboard_;
  // private KnobView cutoffKnob_;
  // private KnobView resonanceKnob_;
  // private KnobView overdriveKnob_;
  private Spinner presetSpinner_;
}
