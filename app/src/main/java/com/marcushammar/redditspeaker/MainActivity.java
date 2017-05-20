package com.marcushammar.redditspeaker;

import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "Reddit Speaker";
    private String log = "Log initiated";
    private HashSet<String> titles = new HashSet<>();
    private TextToSpeech textToSpeech;
    private boolean firstDownloadCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView logTextView = (TextView) findViewById(R.id.logTextView);
        logTextView.setText(log);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable("set", titles);
        savedInstanceState.putString("log", log);
        savedInstanceState.putBoolean("firstDownload", firstDownloadCompleted);

        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        titles = (HashSet<String>)savedInstanceState.getSerializable("set");
        log = savedInstanceState.getString("log");
        firstDownloadCompleted = savedInstanceState.getBoolean("firstDownload");
        TextView logTextView = (TextView) findViewById(R.id.logTextView);
        logTextView.setText(log);

    }

    @Override
    protected void onPause(){
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    public void refreshFromRedditButtonTapped(View v) {
        logMessage("Download started");

        URL url = null;

        try{
            url = new URL("https://www.reddit.com/.json");
        }catch (MalformedURLException mue){
            Log.e(LOG_TAG, "MalformedURLException", mue);
        }

        new DownloadTitlesFromReddit().execute(url);
    }

    public void logMessage(String logEntry) {
        log = log + "\n" + logEntry;
        TextView logTextView = (TextView) findViewById(R.id.logTextView);
        logTextView.setText(log);
    }

    private class DownloadTitlesFromReddit extends AsyncTask<URL, Void, HashSet<String>> {
        protected HashSet<String> doInBackground(URL... urls) {
            HashSet<String> output = new HashSet<>();
            StringBuilder stringBuilder = new StringBuilder();

            try{
                HttpsURLConnection urlConnection = (HttpsURLConnection) urls[0].openConnection();
                try {
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                } finally {
                    urlConnection.disconnect();
                }

            }catch (MalformedURLException mue){
                Log.e(LOG_TAG, "MalformedURLException", mue);
            }catch (IOException ioe){
                Log.e(LOG_TAG, "IOException", ioe);
            }

            try{
                JSONArray articles = new JSONObject(stringBuilder.toString()).getJSONObject("data").getJSONArray("children");

                for (int i = 0; i < articles.length(); i++){
                    String title = articles.getJSONObject(i).getJSONObject("data").getString("title");
                    output.add(title);
                }
            }catch (JSONException je){
                Log.e(LOG_TAG, "JSONException", je);
            }

            return output;
        }

        protected void onPostExecute(HashSet<String> result) {
            HashSet<String> newTitles = new HashSet<>(result);
            newTitles.removeAll(titles);
            titles.addAll(newTitles);
            logMessage("Download completed (" + newTitles.size() + " new, " + titles.size() + " in total now)");
            if(firstDownloadCompleted){
                if (newTitles.size() > 0){
                    textToSpeech.speak("Reddit news", TextToSpeech.QUEUE_ADD, null);
                    for (String title : newTitles){
                        textToSpeech.speak(title, TextToSpeech.QUEUE_ADD, null);
                    }
                }
            }else{
                firstDownloadCompleted = true;
                textToSpeech.speak("The first download is now completed", TextToSpeech.QUEUE_ADD, null);
            }
        }
    }
}