package com.jpschouten.jorik.marcodemeterapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    final static private String APP_KEY = "6ylqqa5lber1ls4";
    final static private String APP_SECRET = "so2jnzte20mr02z";
    private final static String DROPBOX_FILE_DIR = "/MeterData/";
    public static final String MY_PREFS_NAME = "marcoDeMeterPrefs";

    private DropboxAPI<AndroidAuthSession> mDBApi;
    private Button startButton;
    private Button stopButton;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //buttons
        startButton = (Button) findViewById(R.id.button_start_updating);
        startButton.setOnClickListener(onStartClickHandler);

        stopButton = (Button) findViewById(R.id.button_stop_updating);
        stopButton.setOnClickListener(onStopClickHandler);

        String accessToken = null;
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String restoredText = prefs.getString("accesToken", null);
        if (restoredText != null) {
            accessToken = prefs.getString("accesToken", null);//"No name defined" is the default value.
        }
        if(accessToken != null) {
            AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
            AndroidAuthSession session = new AndroidAuthSession(appKeys);
            session.setOAuth2AccessToken(accessToken);
            mDBApi = new DropboxAPI<AndroidAuthSession>(session);

        }
        else {
            authenticate();
        }
    }

    private void authenticate() {
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);

        mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
    }

    protected void onResume() {
        super.onResume();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                //TO-DO: STORE IN SHAREDPREFERENCE FOR LATER USE (SO THE USER DOESNT HAVE TO LOGIN EACH TIME)
                String accessToken = mDBApi.getSession().getOAuth2AccessToken();

                SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString("accesToken", accessToken);
                editor.commit();

            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }


    View.OnClickListener onStartClickHandler = new View.OnClickListener() {
        public void onClick(View v) {
            int delay = 0; // delay for 0 sec.
            int period = 10000; // repeat every 10 sec.
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask()
            {
                public void run()
                {
                    getDataOfDay();
                }
            }, delay, period);

            //show updating
            TextView t = (TextView) findViewById(R.id.textView_updating_variable);
            t.setTextColor(Color.GREEN);
            t.setText("live");

        }
    };

    View.OnClickListener onStopClickHandler = new View.OnClickListener() {
        public void onClick(View v) {
            timer.cancel();
            //show not updating
            TextView t = (TextView) findViewById(R.id.textView_updating_variable);
            t.setTextColor(Color.RED);
            t.setText("not live");
        }
    };

    private void getDataOfDay() {
        try {
//            Calendar now = Calendar.getInstance();
//            int year = now.get(Calendar.YEAR);
//            int month = now.get(Calendar.MONTH) + 1; // Note: zero based!
//            int day = now.get(Calendar.DAY_OF_MONTH);
//            String path = "/MeterData/meter_log_" + year + "-" + month + "-" + day + ".txt";
            String path = "/MeterData/last_meter_data.txt";
            ListFiles listFiles = new ListFiles(mDBApi, path, onContentGotHandler);
            listFiles.execute();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Handler onContentGotHandler = new Handler() {
        public void handleMessage(Message message) {
//            String result = message.getData().("data");

//            for(String fileName : result) {
//                TextView t = (TextView) findViewById(R.id.textfield_test);
//                t.append(fileName + " ");
//            }
            String result = message.getData().getString("data");

//            //get last measured statistics
//            String lastData = getLastStatistics(result);

            //get last values
            HashMap<String,String> lastValues = getLastValues(result);

            //show last updated timestamp
            TextView tLastUpdated = (TextView) findViewById(R.id.textView_last_updated);
            tLastUpdated.setText("Last updated: " + lastValues.get("timestamp"));
//            tLastUpdated.setText("Last updated: " +
//                          formatTimeStamp(lastValues.get("timestampYear")) +
//                    "-" + formatTimeStamp(lastValues.get("timestampMonth")) +
//                    "-" + formatTimeStamp(lastValues.get("timestampDay")) +
//                    " " + formatTimeStamp(lastValues.get("timestampHour")) +
//                    ":" + formatTimeStamp(lastValues.get("timestampMinute")) +
//                    ":" + formatTimeStamp(lastValues.get("timestampSecond"))
//            );

            //show statistics in main view
            TextView t1 = (TextView) findViewById(R.id.textField_currentElectrUsage);
            t1.setText("" + lastValues.get("electrCurrentUsage") + " kWh");

            //show tariff
            TextView hiTariffIndicator = (TextView) findViewById(R.id.textView_hiTariffIndicator);
            TextView loTariffIndicator = (TextView) findViewById(R.id.textView_loTariffIndicator);
            int currentTariff = Integer.parseInt(lastValues.get("currentTariff"));
            if(currentTariff == 1) {
                //set lo grayed out
                loTariffIndicator.setBackgroundColor(Color.parseColor("#878787"));
                //set hi normal
                hiTariffIndicator.setBackgroundColor(Color.parseColor("#ff0000"));
            }
            else {
                //set hi grayed out
                hiTariffIndicator.setBackgroundColor(Color.parseColor("#878787"));
                //set lo noraml
                loTariffIndicator.setBackgroundColor(Color.parseColor("#00ec04"));
            }

//            TextView t2 = (TextView) findViewById(R.id.textField_totalLowElectr);
//            t2.setText("" + lastValues.get("electrLowConsumed") + " kWh");
//
//            TextView t3 = (TextView) findViewById(R.id.textField_totalHighElectr);
//            t3.setText("" + lastValues.get("electrHighConsumed") + " kWh");
//
//            TextView t4 = (TextView) findViewById(R.id.textView_totalGas);
//            t4.setText("" + lastValues.get("gasTotal") + " m3");
        }
    };

//    public String formatTimeStamp(Double n) {
//        DecimalFormat format = new DecimalFormat("0.#");
//        return (n < 10.0d) ? ("0" + format.format(n)) : format.format(n);
//    }

    public HashMap<String,String> getLastValues(String dataString) {
        dataString = dataString.replaceAll("\",\"", "#");
        dataString = dataString.replaceAll("\"", "");
        dataString = dataString.replace("\n", "").replace("\r", "");
        String[] lastValueArray = dataString.split("#");
        HashMap<String,String> returnHashMap = new HashMap<>();

        returnHashMap.put("timestamp", lastValueArray[0]);
        returnHashMap.put("currentTariff",lastValueArray[1]);
        returnHashMap.put("electrCurrentUsage",lastValueArray[2]);
        returnHashMap.put("electrLowConsumed",lastValueArray[3]);
        returnHashMap.put("electrHighConsumed",lastValueArray[4]);
        returnHashMap.put("gasTotal",lastValueArray[5]);

        return returnHashMap;
    }

//    public String getLastStatistics(String data) {
//        String[] allDataArray = data.split("\n\n");
//        return allDataArray[allDataArray.length-1];
//    }

//    public HashMap<String,Double> getLastValues(String dataString) {
//        String[] dataStringLineArray = dataString.split("\n");
//        HashMap<String,Double> returnHashMap = new HashMap<>();
//
//        for(int lineIndex = 0; lineIndex < dataStringLineArray.length; lineIndex++) {
//            //FOR EACH LINE IN DATA
//            String[] currentLineSplitArray = dataStringLineArray[lineIndex].split("\\(");
//
//            String currentLineTag = currentLineSplitArray[0];
//            String currentLineValueString = currentLineSplitArray[currentLineSplitArray.length - 1].substring(0,currentLineSplitArray[currentLineSplitArray.length - 1].length()-1);
//
//            String currentLineValue = currentLineSplitArray.length > 1 ?  currentLineValueString : "";
//            String measureTimestamp = currentLineSplitArray.length > 2 ? currentLineSplitArray[1] : "";
//
//            switch (currentLineTag) {
//                case "0-0:1.0.0": //timestamp
//                    List<String> strings = new ArrayList<String>();
//                    int index = 0;
//                    while (index < currentLineValue.length()) {
//                        strings.add(currentLineValue.substring(index, Math.min(index + 2,currentLineValue.length())));
//                        index += 2;
//                    }
//                    returnHashMap.put("timestampYear",Double.parseDouble(strings.get(0)));
//                    returnHashMap.put("timestampMonth",Double.parseDouble(strings.get(1)));
//                    returnHashMap.put("timestampDay",Double.parseDouble(strings.get(2)));
//                    returnHashMap.put("timestampHour",Double.parseDouble(strings.get(3)));
//                    returnHashMap.put("timestampMinute",Double.parseDouble(strings.get(4)));
//                    returnHashMap.put("timestampSecond",Double.parseDouble(strings.get(5)));
////                    if (!firstTimestamp.HasValue)
////                    {
////                        firstTimestamp = currentTimestamp;
////                    }
//                    break;
//                case "1-0:1.8.1": // el.low consumed
//                    returnHashMap.put("electrLowConsumed",getMeasuredValue(currentLineValue)*1000);
//                    break;
//                case "1-0:1.8.2": // el.high consumed
//                    returnHashMap.put("electrHighConsumed",getMeasuredValue(currentLineValue)*1000);
//                    break;
//                case "1-0:2.8.1": // el.low sent back
//                    break;
//                case "1-0:2.8.2": // el.high sent back
//                    break;
//                case "0-0:96.14.0": // current tariff
//                    returnHashMap.put("currentTariff",getMeasuredValue(currentLineValue));
//                    break;
//                case "1-0:1.7.0": // el. current
//                    returnHashMap.put("electrCurrentUsage",getMeasuredValue
//                            (currentLineValue)*1000);
//                    break;
//
//                case "0-1:24.2.1": // gas
////                    returnHashMap.put("gasTotal",getMeasuredValue(currentLineValue));
////                    if (!string.IsNullOrEmpty(measureValue) && !string.IsNullOrEmpty(measureTimestamp))
////                    {
////                        var gasValue = GetMeasuredValue(measureValue);
////                        if (gasBaseValue==0)
////                        {
////                            gasBaseValue = gasValue;
////                        }
////                        meterChart.Series["Gas"].Points.AddXY(
////                                currentTimestamp, // GetX(firstTimestamp.Value, currentTimestamp),
////                                (gasValue - gasBaseValue) * 1000);
////                    }
//                    break;
//
//            }
//
//        }
//        return returnHashMap;
//    }

    public Double getMeasuredValue(String value) {
        String[] values = value.split("\\*");
        return Double.parseDouble(values[0]);
    }

//    private final Handler onFilesGotHandler = new Handler() {
//        public void handleMessage(Message message) {
//            ArrayList<String> result = message.getData().getStringArrayList("data");
//
//            for(String fileName : result) {
//                TextView t = (TextView) findViewById(R.id.textfield_test);
//                t.append(fileName + " ");
//            }
//        }
//    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
