package com.example.victor.penumbra;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import java.lang.Math;
import java.util.Timer;
import java.util.TimerTask;

import java.lang.String;



public class Game extends Activity implements SensorEventListener {

    // MAC do volante : 20:13:01:24:10:92

    static TextView textViewX;
    static TextView bluetoothTextView;
    static TextView textViewDetail;
    static TextView bluetoothData;
    static TextView dataX;
    static TextView dataY;

    Timer timer;

    boolean started = false;

    private ListView lv;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    public static int ENABLE_BLUETOOTH = 1;

    public static int beginTrackX = 0;
    public static int endTrackX = 1000;
    public static int beginTrackY = 0;
    public static int endTrackY = 50;

    int vx = 0;
    int acc = 0;
    int vy = 0;
    int dx = 0;
    int dy = 25;
    int vv = 25;//km/h

    ArrayAdapter<String> arrayAdapter;

    BluetoothAdapter mBluetoothAdapter;

    ConnectionThread connect;

    class RemindTask extends TimerTask {
        public void run() {
            vv = vv + vv*acc;

            dx = dx + vx;
            dy = dy + vy;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        timer = new Timer();

        timer.schedule(new RemindTask(),0, 1000);

        //tela só na vertical
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        textViewX = (TextView) findViewById(R.id.text_view_x);
        textViewDetail = (TextView) findViewById(R.id.text_view_detail);
        bluetoothTextView = (TextView) findViewById(R.id.bluetooth_text_view);
        bluetoothData = (TextView) findViewById(R.id.bluetooth_data);
        dataX = (TextView) findViewById(R.id.Data_X);
        dataY = (TextView) findViewById(R.id.Data_Y);

        lv = (ListView) findViewById(R.id.listView1);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        lv.setAdapter(arrayAdapter);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);//deprecatede, mas foda-se

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            bluetoothTextView.setText("Que pena! Hardware Bluetooth não está funcionando :(");
        } else {
            bluetoothTextView.setText("Ótimo! Hardware Bluetooth está funcionando :)");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH);
        }

        mBluetoothAdapter.startDiscovery();

//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//        // If there are paired devices
//        if (pairedDevices.size() > 0) {
//            // Loop through paired devices
//            for (BluetoothDevice device : pairedDevices) {
//                //fazer alguma coisa com os que já estão pareados
//                arrayAdapter.add(device.getName() + "\n" + device.getAddress());
//            }
//        }


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

//        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);//0 -> infinito
//        startActivity(discoverableIntent);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                arrayAdapter.add(device.getName() + "\n" + device.getAddress());

                //quando achar o volante conectar a ele->procurar pelo endereço mac do volante
                if(device.getAddress().equals("20:13:01:24:10:92")) {
                    if(!started){
                        connect = new ConnectionThread("20:13:01:24:10:92");
                        connect.start();
                        started = true;
                    }
                    bluetoothData.setText("mandar conexão e esperar data");
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float fpitch = event.values[2];

        if(fpitch<-60) {
            fpitch = -60.0f;
        }

        if(fpitch>60) {
            fpitch = 60.0f;
        }


        int pitch = (int)fpitch;

        double cos = Math.cos((fpitch/180)*Math.PI);
        double sin = Math.sin((fpitch/180)*Math.PI);

        vx = (int)(vv*cos);
        vy = (int)(vv*sin)/5;


        vy = vy * (-1);

        dataX.setText("" + vx + " " + beginTrackX + " " + endTrackX + " " + dx + " " + (vv*cos) );

        dataY.setText("" + vy + " " + beginTrackY + " " + endTrackY + " " + dy+ " " + (vv*sin) );

        textViewX.setText("Posição X: " + pitch);
        if(connect != null){
            if (pitch <= 30 && pitch >= -30) {
                textViewDetail.setText("mostly vertical");
                connect.pararAlmbosLerBotoes();

            } else if (pitch < -30) {
                textViewDetail.setText("mostly right side");
                if (connect.running) {
                    connect.vibrarDireitaLerBotoes();
                    //connect.vibrarDireita();
                    bluetoothData.setText("" + connect.bytes);
                }
            } else if (pitch > 30) {
                textViewDetail.setText("mostly left side");
                if (connect.running) {
                    connect.vibrarEsquerdaLerBotoes();
                    //connect.vibrarEsquerda();
                    bluetoothData.setText("" + connect.bytes);
                }
            }

            if (connect.running) {
                if (connect.leftPressed) {
                    bluetoothData.setText("botao esquerda apertado");
                } else if (connect.rightPressed) {
                    bluetoothData.setText("botao direita apertado");
                } else {
                    bluetoothData.setText("nenhum");
                }
            }
        }
    }

    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            byte[] data = bundle.getByteArray("data");
            String dataString= new String(data);

            if(dataString.equals("---N"))
                bluetoothTextView.setText("Ocorreu um erro durante a conexão D:");
            else if(dataString.equals("---S")) {
                bluetoothTextView.setText("Conectado :D");
            }
            else{
                bluetoothData.setText(dataString);
            }

        }
    };
}

