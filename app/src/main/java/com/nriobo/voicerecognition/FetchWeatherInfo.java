package com.nriobo.voicerecognition;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class FetchWeatherInfo extends AsyncTask<Object, Object, Object> {

    public AsyncResponse delegate = null;//Call back interface

    private static final String OPEN_WEATHER_MAP_API =
//           "http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&APPID=%s";
            "http://www.wichisoft.com/weather/weather.json";    // a copy of the JSON received by the API to avoid reaching
                                                                // the limit of hourly calls during test phase

    public FetchWeatherInfo(AsyncResponse asyncResponse) {
        delegate = asyncResponse;//Assigning call back interfacethrough constructor
    }

    @Override
    protected String doInBackground(Object... params) {

        try {
            URL url = new URL(String.format(OPEN_WEATHER_MAP_API, params[0], params[1]));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            StringBuilder sb = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String json;
            while ((json = bufferedReader.readLine()) != null) {
                sb.append(json + "\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Object result) {
        delegate.processFinish(result);
    }

}
