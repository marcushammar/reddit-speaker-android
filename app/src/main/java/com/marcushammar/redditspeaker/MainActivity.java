package com.marcushammar.redditspeaker;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "Reddit Speaker";
    private String log = "Log initiated";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView logTextView = (TextView) findViewById(R.id.logTextView);
        logTextView.setText(log);
    }

    public void refreshFromRedditButtonTapped(View v) {
        logMessage("Download started");

        URL url = null;

        try{
            url = new URL("https://www.reddit.com/.json");
        }catch (MalformedURLException mue){
            Log.e(LOG_TAG, "MalformedURLException", mue);
        }

        new DownloadFromReddit().execute(url);
    }

    public void logMessage(String logEntry) {
        log = log + "\n" + logEntry;
        TextView logTextView = (TextView) findViewById(R.id.logTextView);
        logTextView.setText(log);
    }

    private class DownloadFromReddit extends AsyncTask<URL, Void, String> {

        protected String doInBackground(URL... urls) {
            String output = "";

            try{
                HttpsURLConnection urlConnection = (HttpsURLConnection) urls[0].openConnection();
                try {
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    StringBuffer stringBuffer = new StringBuffer();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuffer.append(line);
                    }

                    output = stringBuffer.toString();
                } finally {
                    urlConnection.disconnect();
                }

            }catch (MalformedURLException mue){
                Log.e(LOG_TAG, "MalformedURLException", mue);
            }catch (IOException ioe){
                Log.e(LOG_TAG, "IOException", ioe);
            }

            return output;
        }

        protected void onPostExecute(String result) {
            logMessage("Download completed (size = " + result.length() + " bytes)");
        }
    }
}