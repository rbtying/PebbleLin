package com.example.pebblekit_logtests;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import com.github.sendgrid.SendGrid;

public class MainActivity extends Activity implements OnTouchListener {

    private Button[] buttons;
    private boolean buttonpressed[] = new boolean[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttons = new Button[4];
        buttons[0] = (Button) findViewById(R.id.Button1);
        buttons[1] = (Button) findViewById(R.id.Button2);
        buttons[2] = (Button) findViewById(R.id.Button3);
        buttons[3] = (Button) findViewById(R.id.Button4);

        System.out.println(buttons[0]);

        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setOnTouchListener(this);
        }

        Button btn = (Button) findViewById(R.id.SendButton);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendMessageOnClick().execute("");
            }
        });

    }

    private class SendMessageOnClick extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... arg0) {
            SendGrid sendgrid = new SendGrid("kevinheh",
                    "pennappspebble");

            sendgrid.addTo("am3713@columbia.edu");
            sendgrid.setFrom("aditya29@gmail.com");
            sendgrid.setSubject("Hello World - sent  from within Eclipse");
            sendgrid.setText("Hi! - My first email through SendGrid");
            sendgrid.send();
            //Log.v("YOYTAG", "HELLOabcdef");
            return false;
        }

    }

    public boolean onTouch(View v, MotionEvent e) {
        for (int i = 0; i < buttons.length; i++) {
            if (v.getId() == buttons[i].getId()) {
                switch (e.getAction()) {
                case MotionEvent.ACTION_UP:
                    buttonpressed[i] = false;
                    break;
                case MotionEvent.ACTION_DOWN:
                    buttonpressed[i] = true;
                    break;
                }
            }
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < buttonpressed.length; i++) {
            if (buttonpressed[i]) {
                buf.append(i);
            }
        }

        TextView t = (TextView) findViewById(R.id.TextView1);
        t.setText(buf.toString());

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
