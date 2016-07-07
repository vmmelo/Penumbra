package com.example.victor.penumbra;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
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

    MediaPlayer mpBackGround = null;
    MediaPlayer mpBoom = null;
    MediaPlayer mpbip = null;

    static TextView textViewX;
    static TextView bluetoothTextView;
    static TextView textViewDetail;
    static TextView bluetoothData;
    static TextView dataX;
    static TextView dataY;
    static TextView statusGame;

    Timer timer;

    boolean started = false;

    private ListView lv;

    private boolean gameRunning = true;

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
    int vMax = 15;//m/s
    int dx = beginTrackX;//m
    int dy = endTrackY/2;//m

    int [][] localDesastres;
    int comprimento = vMax;//comprimento do evento
    int largura = 10;// alcance do acotecimento pra + ou pra -

    String outout;

    ArrayAdapter<String> arrayAdapter;

    BluetoothAdapter mBluetoothAdapter;

    ConnectionThread connectBluetooth;

    class RemindTask extends TimerTask {
        public void run() {

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
                    vv = vv -1;
                }
                else if(connectBluetooth.rightPressed){
                    vv = vv + 1;
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
        connectBluetooth.pararAmbos();
        timer.cancel();
        gameRunning = false;
        mpBoom.start();
        connectBluetooth.vibrarAmbos();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Som no background
        mpBackGround = MediaPlayer.create(this, R.raw.gameplay);
        mpBoom = MediaPlayer.create(this,R.raw.explosao);
        mpbip = MediaPlayer.create(this,R.raw.beep_curto);
        mpBackGround.setLooping(true);
        mpBackGround.start();


        localDesastres = new int[5][2];
        //porcentagens do tamanho maximo da pista indica onde começa o evento: 0 para x 1 para y
        localDesastres[0][0] = 30;//x
        localDesastres[0][1] = 70;//y

        localDesastres[1][0] = 40;//x
        localDesastres[1][1] = 0;//y

        localDesastres[2][0] = 55;//x
        localDesastres[2][1] = 50;//y

        localDesastres[3][0] = 75;//x
        localDesastres[3][1] = 60;//y

        localDesastres[4][0] = 90;//x
        localDesastres[4][1] = 40;//y

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
        connectBluetooth.pararAmbos();
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
        mpBackGround.start();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mpBackGround.pause();
        mSensorManager.unregisterListener(this);
        connectBluetooth.pararAmbos();
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

            dataY.setText("" + vy + " " + dy + " " + endTrackY/5 + " " + endTrackY*4/5);

            /*
            * pitch
            * - direita
            * + esquerda
            */

            textViewX.setText("Posição X: " + pitch);


            if (connectBluetooth.running) {

                if(dy < endTrackY/5 && gameRunning){
                    connectBluetooth.vibrarDireitaLerBotoes();
                }

                else if(dy>endTrackY*4/5 && gameRunning){
                    connectBluetooth.vibrarEsquerdaLerBotoes();
                }
                else {
                    connectBluetooth.pararAmbosLerBotoes();
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

            for(int a=0;a<5;a++){
                if(bateu(endTrackX,endTrackY,dx,dy,a)){
                    endGame("bateu");
                }
            }
        }
    }

    boolean bateu(int tamanhoPistaX,int tamanhoPistaY,int cordX,int cordY,int indiceDessastre){
        int localDesastreX = localDesastres[indiceDessastre][0]*tamanhoPistaX;
        int localDesastreY = localDesastres[indiceDessastre][1]*tamanhoPistaY;

        localDesastreX = localDesastreX/100;
        localDesastreY = localDesastreY/100;

        textViewDetail.setText("" + localDesastreX + " " + localDesastreY + " " + (localDesastreX + comprimento) + " " + (localDesastreY + largura));

        if((cordX >= localDesastreX && cordX <= (localDesastreX + comprimento)) && (cordY >= localDesastreY && cordY <= (localDesastreY + largura))){
            return true;
        }

        return false;
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

    //método para executar os audios, passando o nome do audio como parametro
    /*protected void managerOfSound(String theText) {
        if (mp != null) {
            mp.reset();
            mp.release();
        }
        if (theText == "hello")
            mp = MediaPlayer.create(this, R.raw.hello);
        else if (theText == "goodbye")
            mp = MediaPlayer.create(this, R.raw.goodbye);
        else
            mp = MediaPlayer.create(this, R.raw.what);
        mp.start();
    }*/
}

