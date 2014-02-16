package com.lynbrook.pebblelin;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ToggleButton;

import com.lynbrook.pebblin.R;

public class StartActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(R.string.app_name);
    setContentView(R.layout.activity_main);

    Button b = (Button) findViewById(R.id.huebutton);
    b.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        gotohuething();
      }
    });

    Button b2 = (Button) findViewById(R.id.realbutton);
    b2.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        gotorealthing();
      }
    });
  }

  public void gotohuething() {
    Intent intent = new Intent(getApplicationContext(), PHHomeActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) intent.addFlags(0x8000); // equal to
    boolean tog = ((ToggleButton)findViewById(R.id.tutorialbutton)).isChecked();
    intent.putExtra("toggle", tog);
    startActivity(intent);
  }

  public void gotorealthing() {
    Intent intent = new Intent(getApplicationContext(), PebblinActivity.class);
    intent.putExtra("toggle",false);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) intent.addFlags(0x8000); // equal to
    startActivity(intent);
  }

}
