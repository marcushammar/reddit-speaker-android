package com.marcushammar.redditspeaker;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class BackgroundService extends Service {
    private static final String LOG_TAG = "Reddit Speaker";
    private TextToSpeech textToSpeech;
    private Handler handler;
    private LocalRunnable runnable;
    private boolean running = false;
    private int downloadInterval = 15;
    private HashSet<String> titles = new HashSet<>();
    private boolean firstDownloadCompleted = false;

    private class LocalRunnable implements Runnable{
        @Override
        public void run() {
            startDownload();
            handler.postDelayed(runnable, downloadInterval * 1000);
        }
    }

    @Override
    public void onCreate() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });

        handler = new Handler();
        runnable = new LocalRunnable();
    }

    public void startDownload(){
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();

        //logMessage("Download started");

        URL url = null;

        try{
            url = new URL("https://www.reddit.com/.json");
        }catch (MalformedURLException mue){
            Log.e(LOG_TAG, "MalformedURLException", mue);
        }

        new DownloadTitlesFromReddit().execute(url);
    }


    private void speak(String text){
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = intent.getStringExtra("COMMAND");

        switch (command){
            case "WARMUP":
                break;
            case "START":
                runnable.run();
                running = true;
                break;
            case "DOWNLOAD_INTERVAL":
                int newInterval = intent.getIntExtra("DOWNLOAD_INTERVAL", 0);

                if (newInterval > 0){
                    downloadInterval = newInterval;
                }
                break;

        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "onDestroy", Toast.LENGTH_SHORT).show();

        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        handler.removeCallbacks(runnable);
        runnable = null;
        handler = null;
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
            //logMessage("Download completed (" + newTitles.size() + " new, " + titles.size() + " in total now)");
            Toast.makeText(BackgroundService.this, "Download completed (" + newTitles.size() + " new, " + titles.size() + " in total now)", Toast.LENGTH_SHORT).show();

            if(firstDownloadCompleted){
                if (newTitles.size() > 0){
                    speak("Reddit news");
                    int i = 1;
                    for (String title : newTitles){
                        speak("News number " + (i++));
                        speak(title);
                    }
                }else{
                    speak("Nothing new");
                }
            }else{
                firstDownloadCompleted = true;
                speak("The first download is now completed");
            }
        }
    }
}