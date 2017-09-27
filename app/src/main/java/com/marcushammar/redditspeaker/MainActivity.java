package com.marcushammar.redditspeaker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private boolean running = false;
    private TextView seekBarValue;
    private int downloadInterval = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, BackgroundService.class);
        intent.putExtra("COMMAND", "WARMUP");
        this.startService(intent);

        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean("running");
            downloadInterval = savedInstanceState.getInt("downloadInterval");
        }

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBarValue = findViewById(R.id.seekBarValue);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarValue.setText(String.valueOf(5 + progress));
                downloadInterval = 5 + progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent intent = new Intent(MainActivity.this, BackgroundService.class);
                intent.putExtra("COMMAND", "DOWNLOAD_INTERVAL");
                intent.putExtra("DOWNLOAD_INTERVAL", downloadInterval);
                MainActivity.this.startService(intent);
            }
        });

        updateUserInterface();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("running", running);
        savedInstanceState.putInt("downloadInterval", downloadInterval);

        super.onSaveInstanceState(savedInstanceState);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        running = savedInstanceState.getBoolean("running");
        downloadInterval = savedInstanceState.getInt("downloadInterval");
        updateUserInterface();
    }

    private void updateUserInterface(){
        Button startButton = findViewById(R.id.startButton);
        startButton.setEnabled(!running);

        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setEnabled(running);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    public void startButtonTapped(View v){
        Intent intent = new Intent(this, BackgroundService.class);
        intent.putExtra("COMMAND", "START");
        this.startService(intent);

        running = true;
        updateUserInterface();
    }

    public void stopButtonTapped(View v){
        Intent mServiceIntent = new Intent(this, BackgroundService.class);
        this.stopService(mServiceIntent);

        running = false;
        updateUserInterface();
    }
}