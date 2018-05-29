package com.nriobo.voicerecognition;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

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
public class ExampleInstrumentedTest {
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
    public void fetchWeatherInfo_codIs200() {
        String cod= "0";
        JSONObject json = new JSONObject();
        try {
            json = FetchWeatherInfo.getJSON(appContext, "Hamburg");
            cod = json.getString("cod");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        assertEquals("200", cod);
    }

}
