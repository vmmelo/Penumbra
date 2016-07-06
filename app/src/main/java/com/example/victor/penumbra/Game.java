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
    static TextView statusGame;

    int [][] localDesastres;

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

    int vx = 0;//m/s
    int vy = 0;//m/s
    int vv = 0;//m/s
    int vMax = 25;//m/s
    int acc = 0;//m/sˆ2
    int dx = beginTrackX;//m
    int dy = endTrackY/2;//m

    String outout;

    ArrayAdapter<String> arrayAdapter;

    BluetoothAdapter mBluetoothAdapter;

    ConnectionThread connectBluetooth;

    class RemindTask extends TimerTask {
        public void run() {
            vv = vv + acc;

            if(vv<0){
                vv = 0;
            }
            else if(vv>vMax){
                vv = vMax;
            }

            dx = dx + vx;
            dy = dy + vy;

            if(connectBluetooth != null){
                if(connectBluetooth.leftPressed){
                    acc = acc -1;
                }
                else if(connectBluetooth.rightPressed){
                    acc = acc +1;
                }
                else{
                    acc = 0;
                }
            }

            if(dx >= endTrackX){
                endGame("ganhou");
            }

            if(dy<beginTrackY || dy>endTrackY){
                endGame("morreu");
            }
        }
    }

    private void endGame(String output){
        this.outout = output;
        timer.cancel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        timer = new Timer();

        timer.schedule(new RemindTask(),0, 1000);

        localDesastres = new int[5][2];
        //implementar o resto

        //tela só na vertical
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        textViewX = (TextView) findViewById(R.id.text_view_x);
        textViewDetail = (TextView) findViewById(R.id.text_view_detail);
        bluetoothTextView = (TextView) findViewById(R.id.bluetooth_text_view);
        bluetoothData = (TextView) findViewById(R.id.bluetooth_data);
        dataX = (TextView) findViewById(R.id.Data_X);
        dataY = (TextView) findViewById(R.id.Data_Y);
        statusGame = (TextView) findViewById(R.id.status_game);

        outout = new String("vivo");

        statusGame.setText(outout);

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
            bluetoothTextView.setText("começou a procurar bluetooth");
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                arrayAdapter.add(device.getName() + "\n" + device.getAddress());

                //quando achar o volante conectar a ele->procurar pelo endereço mac do volante
                if(device.getAddress().equals("20:16:03:04:15:52")) {
                    if(!started){
                        connectBluetooth = new ConnectionThread("20:16:03:04:15:52");
                        connectBluetooth.start();
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
        vy = (int)(vv*sin)/3;


        vy = vy * (-1);

        if(connectBluetooth != null){

            dataX.setText("" + vx + " " + " " + dx + " " + connectBluetooth.rightPressed);

            dataY.setText("" + vy + " " + dy + " " + endTrackY/4 + " " + endTrackY*3/4);

            /*
            * pitch
            * - direita
            * + esquerda
            */

            textViewX.setText("Posição X: " + pitch);


            if (connectBluetooth.running) {

                if(dy < endTrackY/4){
                    connectBluetooth.vibrarDireitaLerBotoes();
                }

                else if(dy>endTrackY*3/4){
                    connectBluetooth.vibrarEsquerdaLerBotoes();
                }
                else {
                    connectBluetooth.pararAlmbosLerBotoes();
                }

                bluetoothData.setText("" + connectBluetooth.bytes);

                if (connectBluetooth.leftPressed) {
                    bluetoothData.setText("botao esquerdo apertado");
                } else if (connectBluetooth.rightPressed) {
                    bluetoothData.setText("botao direito apertado");
                } else {
                    bluetoothData.setText("nenhum");
                }
            }

            statusGame.setText(outout);
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

