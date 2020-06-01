package com.example.mqttconnectiontest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.*;

import org.json.JSONException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "mainActivity";

    //mobile device information
    private String productKey = "a1ajqSFTmmg";
    private String deviceName = "EiVQxvTv1ojASenNRLBR";
    private String deviceSecret = "byYSjoVMHr7FMexThXb5oyzJhlhKafWi";
    private String sensorDeviceName = "hdyO3j5KOFfNLFvWOoBk";

    //an instance of Mqtt client (asynchronous)
    private MqttAsyncClient mqttAsyncClient = null;

    //topics
    private String info_required_topic = "/a1ajqSFTmmg/" + deviceName + "/user/test";
    private String get_topic = "/shadow/get/a1ajqSFTmmg/" + sensorDeviceName;
    private String update_topic = "/shadow/update/a1ajqSFTmmg/" + sensorDeviceName;

    //views
    private ViewPager viewPager;
    private View navStatus;
    private View navAlarm;
    private View navSetting;

    private String mqttPassword = null;

    private Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager = (ViewPager) findViewById(R.id.main_container);
        navStatus = (View) findViewById(R.id.nav_status);
        navAlarm = (View) findViewById(R.id.nav_alarm);
        navSetting = (View) findViewById(R.id.nav_setting);

        setupFragments(viewPager);
        setupNavOnclickListener(navStatus, navAlarm, navSetting);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //retrieve sensor data
        Thread thread = new Thread(new initStatus());
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "In onPause");
        timer.cancel();
        disconnect();
    }

    //set navigation onClickListeners
    private void setupNavOnclickListener(View navStatus, View navAlarm, View navSetting){
        navStatus.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
               viewPager.setCurrentItem(0);
            }
        });

        navAlarm.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1);
            }
        });

        navSetting.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(2);
            }
        });
    }

    private void setupFragments(ViewPager viewPager){
        FragmentPageAdapter adapter = new FragmentPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new HomeFragment(), "HomeFragment");
        adapter.addFragment(new AlarmFragment(), "AlarmFragment");
        adapter.addFragment(new SettingFragment(), "SettingFragment");
        viewPager.setAdapter(adapter);
    }

    //exit the program automatically for testing purposes
    public void setAutoLogout(int delay){
        final int finalDelay = delay;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(finalDelay);
                    finish();
                }
                catch (Exception e) {
                    System.err.println(e);
                }
            }
        }).start();
    }

    public void addMessageToView(String message){

        final String messageContent = message;

        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RelativeLayout myLayout = findViewById(R.id.main);

                    TextView messageText = new TextView(getApplicationContext());
                    messageText.setText(messageContent);
                    messageText.setLayoutParams(new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT));

                    myLayout.addView(messageText);
                }
            });
    }

    //acquire alarm data from getDeviceShadow api
    private void getAlarmData() {
        try {
            Log.i(TAG, "get alarm data!");
            URL url = new URL("https://iot.cn-shanghai.aliyuncs.com/?Action=GetDeviceShadow" +
                "&DeviceName=hdyO3j5KOFfNLFvWOoBk&ProductKey=a1ajqSFTmmg&Format=JSON&Version=2018-01-20&AccessKeyId=LTAI4G926bMhPWSbFU5siNw1" +
                "&Signature=" + mqttPassword + "&SignatureMethod=HMAC-SHA1&Timestamp=" + Instant.now().toString() + "&SignatureVersion=1.0" +
                "&SignatureNonce=" + UUID.randomUUID()  + "&RegionId=cn-shanghai");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                result.append(line);
            }
            Log.i(TAG, "http response is: " + result);
            urlConnection.disconnect();
        } catch (IOException e){
            Log.e(TAG, "IO exceptions: " + e.toString());
        }
        }


    //acquire sensor data
    private void getSensorData(){

        try {
            JSONObject payload = new JSONObject();
            JSONObject params = new JSONObject();
            params.put("data_required", "true");
            payload.put("id", System.currentTimeMillis());
            payload.put("params", params);
            payload.put("method", "thing.event.property.post");

            //convert json payload to byte array
            byte[] payloadBytes = payload.toString().getBytes("UTF-8");

            mqttAsyncClient.publish(info_required_topic, payloadBytes, 0, false).waitForCompletion(10000);
            Log.i(TAG, "getting sensor data");
        } catch(Throwable err){
            Log.e(TAG, err.toString());
        }
    }

    private class initStatus implements Runnable {
        public void run() {

            //connect to Alibaba cloud
            try {
                //to-do: find some better way to handle the asynchronous connection
                IMqttToken token = initAliyunIoTClient();
                token.waitForCompletion();
            } catch (MqttException e){
                Log.e(TAG, "cannot connect to cloud: " + e.toString());
            }

            try {
                //subscribe to topic
                mqttAsyncClient.subscribe(info_required_topic, 0).waitForCompletion();
                mqttAsyncClient.subscribe(get_topic, 0).waitForCompletion();
                Log.i(TAG, "subscription succeeded");
            } catch (MqttException e){
                Log.e(TAG, "error occurred when subscribing to topic:  " + e.toString());
            }

            //HomeFragment: require data from sensor every 30 seconds
            timer.scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    getSensorData();
                }
            },0,20000);

            //AlarmFragment
            getAlarmData();
        }
    }

    public void messageHandler(String topic, MqttMessage message){
        JSONObject msg = new JSONObject();
        JSONObject params = new JSONObject();

        // parse JSON data
        try {
            msg = new JSONObject(message.toString());
        } catch (JSONException e){
            Log.e(TAG, "Cannot parse incoming messages: " + e.toString());
            return;
        }


        if (msg.has("params")){
            //parse the sub-object defined in params field
            try {
                params = msg.getJSONObject("params");
            } catch (JSONException e) {
                Log.e(TAG, "params field does not exist");
                return;
            }

            //if message contains returned sensor data
            if (params.has("data_returned")){
                try {
                    displaySensorData(params.getJSONObject("data_returned"));
                } catch(JSONException e) {
                    Log.e(TAG, "cannot read field: " + e.toString());
                }
            } else {
                Log.i(TAG, params.toString());
            }
        } else {
            Log.i(TAG, "message without params is" + msg);
        }
    }

    //display sensor data
    private void displaySensorData(JSONObject data) throws JSONException {
        final String temperatureData = Integer.toString(data.getInt("temperature"));
        final String smokeData = Boolean.toString(data.getBoolean("waterline"));
        final String waterlineData = Boolean.toString(data.getBoolean("waterline"));
        final TextView temperatureField = (TextView)findViewById(R.id.temperature_text);
        final TextView smokeField = (TextView)findViewById(R.id.smoke_text);
        final TextView waterlineField= (TextView)findViewById(R.id.waterline_text);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                temperatureField.setText(temperatureData);
                smokeField.setText(smokeData);
                waterlineField.setText(waterlineData);
            }
        });
    }

    //callback listener for incoming messages
    public class messageArrivedCallback implements MqttCallback {
        public void connectionLost (Throwable cause) {
            Log.e(TAG, cause.toString());
        }

        public void deliveryComplete (IMqttDeliveryToken token) {
            Log.i(TAG, token.toString());
        }

        public void messageArrived (String topic, MqttMessage message) {
            messageHandler(topic, message);
        }
    }

    //setup connections to cloud using mobile device credentials and sensor device credentials
    private IMqttToken initAliyunIoTClient() throws MqttException{

        String clientId = "FVFSRHGUH3QD";
        String targetServer = "tcp://" + productKey + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";

        //intit connect params
        Map<String, String> params = new HashMap<String, String>(16);
        params.put("productKey", productKey);
        params.put("deviceName", deviceName);
        params.put("clientId", clientId);

        String mqttclientId = clientId + "|securemode=3,signmethod=hmacsha1|";
        String mqttUsername = deviceName + "&" + productKey;
        mqttPassword = AliyunIoTSignUtil.sign(params, deviceSecret, "hmacsha1");

        IMqttToken token = connectMqtt(targetServer, mqttclientId, mqttUsername, mqttPassword);

        return token;
    }

    //connect to cloud
    private IMqttToken connectMqtt(String targetServer, String mqttclientId, String mqttUsername, String mqttPassword) throws MqttException{
        //init Mqtt clients
        MemoryPersistence persistence = new MemoryPersistence();
        mqttAsyncClient = new MqttAsyncClient(targetServer, mqttclientId, persistence);

        mqttAsyncClient.setCallback(new messageArrivedCallback());
        MqttConnectOptions connOpts = new MqttConnectOptions();

        connOpts.setMqttVersion(4);
        connOpts.setAutomaticReconnect(false);
        connOpts.setCleanSession(true);

        connOpts.setUserName(mqttUsername);
        connOpts.setPassword(mqttPassword.toCharArray());
        connOpts.setKeepAliveInterval(60);

        return mqttAsyncClient.connect(connOpts);
    }

    //disconnect mqttAsyncClient from cloud
    private void disconnect(){
        try {
            mqttAsyncClient.disconnect(100).waitForCompletion();
            Log.d(TAG, "disconnect successfully!");
        } catch (MqttException e) {
            Log.d(TAG, "cannot disconnect from cloud: " + e.toString());
        }
    }
}
