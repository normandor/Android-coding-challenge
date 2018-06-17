package com.nriobo.voicerecognition;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class InstrumentedTests {
    private Context appContext;

    @Before
    public void setup() {
        appContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void useAppContext() {
       assertEquals("com.nriobo.voicerecognition", appContext.getPackageName());
    }


    // to check if the connection to API was well implemented
    @Test
    public void fetchWeatherInfo_nameIsHamburg() {

        FetchWeatherInfo asyncTask = new FetchWeatherInfo(new AsyncResponse() {

            @Override
            public void processFinish(Object output) {
                String cod= "0";
                try {
                    JSONObject jsonObject = new JSONObject((String) output);
                    cod = jsonObject.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                assertEquals("Hamburg", cod);
            }
        });

        asyncTask.execute(new Object[]{"Hamburg", InstrumentationRegistry.getTargetContext().getResources().getString(R.string.open_weather_maps_app_id)});
    }

}
