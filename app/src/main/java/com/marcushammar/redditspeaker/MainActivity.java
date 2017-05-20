package com.marcushammar.redditspeaker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private String log = "Log initiated";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView logTextView = (TextView)findViewById(R.id.logTextView);
        logTextView.setText(log);
    }

    public void refreshFromRedditButtonTapped(View v){

    }
}
