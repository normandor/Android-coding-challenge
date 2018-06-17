package com.nriobo.voicerecognition;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class SpeechRecognitionActivity extends Activity implements RecognitionListener {

    private String theCity = "Hamburg"; // this has to be changeable
    private static final String URL_BASE_ICON = "http://openweathermap.org/img/w/";
    private static final String WEATHER_SCREEN = "wakeup";
    private static final String WEATHER_PHRASE = "weather";
    private static final String BACK_PHRASE = "go back";
    private static final String MAIN_SCREEN = "menu";

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    private String lastSelection = "";
    private Context mContext;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        mContext = this;
        captions = new HashMap<>();
        captions.put(WEATHER_SCREEN, R.string.initial_caption);
        captions.put(MAIN_SCREEN, R.string.weather_caption);
        setContentView(R.layout.layout_speech_recognition_activity);
        ((TextView) findViewById(R.id.caption_text)).setText(R.string.preparing_recogniser);

        getAndPrintWeatherInfo(theCity);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        new SetupTask(this).execute();
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<SpeechRecognitionActivity> activityReference;

        SetupTask(SpeechRecognitionActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                ((TextView) activityReference.get().findViewById(R.id.caption_text))
                        .setText(R.string.failed_to_init_recogniser);
            } else {
                activityReference.get().switchSearch(WEATHER_SCREEN);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        switch (text) {
            case WEATHER_PHRASE:
                switchSearch(MAIN_SCREEN);  // listen again for coming back to menu
                break;
            case BACK_PHRASE:
                switchSearch(WEATHER_SCREEN);
                break;
            default:
                ((TextView) findViewById(R.id.result_text)).setText(text);  // debug purposes: print what user says
                break;
        }

    }

    // called when the recognizer is stopped
    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), "you said " + text, Toast.LENGTH_SHORT).show();   // user feedback
        }
    }

    private void updateWeatherValues(String temperature, String city, String humidity, String iconFile) {
        TextView tvShowWeather = findViewById(R.id.textViewWeatherInfo);
        String weather = String.format("Temperature in %s\n%s degrees, %s humidity", city, temperature, humidity + "%");
        tvShowWeather.setText(weather);
        ImageView ivWeatherIcon = (ImageView) findViewById(R.id.imageViewWeatherIcon);
        GlideApp.with(mContext)
                .load(URL_BASE_ICON + iconFile)
                .into(ivWeatherIcon);

    }

    public void getWeatherInfo(String city) {
        FetchWeatherInfo asyncTask = new FetchWeatherInfo(new AsyncResponse() {

            @Override
            public void processFinish(Object output) {
                String city = "";
                String grados = "";
                String humidity = "";
                String icon = "";
                try {
                    JSONObject jsonObject = new JSONObject((String) output);
                    city = jsonObject.getString("name");
                    JSONObject main = jsonObject.getJSONObject("main");
                    JSONArray weather = jsonObject.getJSONArray("weather");
                    grados = main.getString("temp");
                    humidity = main.getString("humidity");
                    icon = weather.getJSONObject(0).getString("icon");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                updateWeatherValues(grados, city, humidity, icon + ".png");
            }
        });

        asyncTask.execute(new Object[]{theCity, getResources().getString(R.string.open_weather_maps_app_id)});
    }

    @Override
    public void onBeginningOfSpeech() {
    }


    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(WEATHER_SCREEN) && !recognizer.getSearchName().equals(MAIN_SCREEN)) {
            if (lastSelection.equals(""))
                switchSearch(lastSelection);
            else
                switchSearch(WEATHER_SCREEN);
        }
    }

    private void switchSearch(String searchName) {
        lastSelection = searchName;
        recognizer.stop();

        recognizer.startListening(searchName, 20000);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);

        if (searchName.equals(MAIN_SCREEN)) {
            // we show weather info
            getAndPrintWeatherInfo(theCity);
        } else
            hideWeatherInfoTextView();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                //.setRawLogDir(assetsDir)
                .getRecognizer();
        recognizer.addListener(this);

        recognizer.addKeyphraseSearch(WEATHER_SCREEN, WEATHER_PHRASE);
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MAIN_SCREEN, menuGrammar);
    }

    private void hideWeatherInfoTextView() {
        TextView tvShowWeather = findViewById(R.id.textViewWeatherInfo);
        tvShowWeather.setVisibility(View.GONE);
        ImageView ivWeather = findViewById(R.id.imageViewWeatherIcon);
        ivWeather.setVisibility(View.GONE);
    }

    private void showWeatherInfoTextView() {
        TextView tvShowWeather = findViewById(R.id.textViewWeatherInfo);
        tvShowWeather.setVisibility(View.VISIBLE);
        ImageView ivWeather = findViewById(R.id.imageViewWeatherIcon);
        ivWeather.setVisibility(View.VISIBLE);
    }
    private void getAndPrintWeatherInfo(String city) {
        showWeatherInfoTextView();
        String weather = "No information available";
        TextView tvShowWeather = (TextView) findViewById(R.id.textViewWeatherInfo);
        tvShowWeather.setText(weather );
        getWeatherInfo(theCity);
    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        if (lastSelection.equals(""))
            switchSearch(lastSelection);
        else
            switchSearch(WEATHER_SCREEN);
    }
}