package com.lynbrook.pebblelin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.github.sendgrid.SendGrid;
import com.lynbrook.pebblin.R;

public class SendGridActivity extends Activity {

  private EditText et;
  private ArrayList<Integer> recentNotes;

  @SuppressWarnings("unchecked")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sendgrid);

    recentNotes = (ArrayList<Integer>) (getIntent().getExtras().get("recentNotes"));

    et = (EditText) findViewById(R.id.Email);
    Button btn = (Button) findViewById(R.id.SendButton);

    btn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        new SendMessageOnClick().execute("");
        gotorealthing();
      }
    });
  }

  public void gotorealthing() {
    Intent intent = new Intent(getApplicationContext(), PebblinActivity.class);
    intent.putExtra("toggle", false);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) intent.addFlags(0x8000); // equal to
    startActivity(intent);
  }

  private class SendMessageOnClick extends AsyncTask<String, Void, Boolean> {
    @Override
    protected Boolean doInBackground(String... arg0) {
      // StringBuffer notes = new StringBuffer();
      // for (int i = 0; i < recentNotes.size(); i++) {
      // notes.append(Note.getName(recentNotes.get(i) % 12) + "\n");
      // }
      MidiFile m = new MidiFile();
      for (int i = 0; i < recentNotes.size(); i++) {
        m.noteOnOffNow(MidiFile.QUAVER, recentNotes.get(i), 127);
      }
      File outputDir = getApplicationContext().getCacheDir(); // context being the Activity pointer
      File outputFile;
      try {
        outputFile = File.createTempFile("your_recording", ".mid", outputDir);
        m.writeToFile(outputFile.getAbsolutePath());

        SendGrid sendgrid = new SendGrid("kevinheh", "pennappspebble");
        sendgrid.addTo(et.getText().toString());
        sendgrid.setFrom("no-reply@getpebblin.com");
        sendgrid.setSubject("Your Recent Music");
        sendgrid.setText("Music is attached!");
        sendgrid.addFile(outputFile);
        sendgrid.send();
      } catch (IOException e) {
        e.printStackTrace();
      }

      return false;
    }
  }

}
