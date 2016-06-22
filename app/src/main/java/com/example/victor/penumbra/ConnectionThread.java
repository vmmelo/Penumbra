package com.example.victor.penumbra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import java.util.Arrays;

import java.lang.String;

import java.io.InputStream;
import java.io.OutputStream;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by vinicemanuel on 6/21/16.
 */
public class ConnectionThread extends Thread{

    BluetoothSocket btSocket = null;
    BluetoothServerSocket btServerSocket = null;
    String btDevAddress = null;
    String myUUID = "00001101-0000-1000-8000-00805F9B34FB";
    boolean server;
    boolean running = false;
    byte [] data;

    boolean leftPressed = false;
    boolean rightPressed = false;

    int bytes;

    InputStream input = null;
    OutputStream output = null;

    /*  Este construtor prepara o dispositivo para atuar como servidor.
     */
    public ConnectionThread() {

        this.server = true;
    }

    /*  Este construtor prepara o dispositivo para atuar como cliente.
        Tem como argumento uma string contendo o endereço MAC do dispositivo
    Bluetooth para o qual deve ser solicitada uma conexão.
     */
    public ConnectionThread(String btDevAddress) {

        this.server = false;
        this.btDevAddress = btDevAddress;
    }

    /*  O método run() contem as instruções que serão efetivamente realizadas
    em uma nova thread.
     */
    public void run() {

        /*  Anuncia que a thread está sendo executada.
            Pega uma referência para o adaptador Bluetooth padrão.
         */
        this.running = true;
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        /*  Determina que ações executar dependendo se a thread está configurada
        para atuar como servidor ou cliente.
         */
        if(this.server) {

            /*  Servidor.
             */
            try {

                /*  Cria um socket de servidor Bluetooth.
                    O socket servidor será usado apenas para iniciar a conexão.
                    Permanece em estado de espera até que algum cliente
                estabeleça uma conexão.
                 */
                btServerSocket = btAdapter.listenUsingRfcommWithServiceRecord("Super Bluetooth", UUID.fromString(myUUID));
                btSocket = btServerSocket.accept();

                /*  Se a conexão foi estabelecida corretamente, o socket
                servidor pode ser liberado.
                 */
                if(btSocket != null) {

                    btServerSocket.close();
                }

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }


        } else {

            /*  Cliente.
             */
            try {

                /*  Obtem uma representação do dispositivo Bluetooth com
                endereço btDevAddress.
                    Cria um socket Bluetooth.
                 */
                BluetoothDevice btDevice = btAdapter.getRemoteDevice(btDevAddress);
                btSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString(myUUID));

                /*  Envia ao sistema um comando para cancelar qualquer processo
                de descoberta em execução.
                 */
                btAdapter.cancelDiscovery();

                /*  Solicita uma conexão ao dispositivo cujo endereço é
                btDevAddress.
                    Permanece em estado de espera até que a conexão seja
                estabelecida.
                 */
                if (btSocket != null)
                    btSocket.connect();

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }

        }

        /*  Pronto, estamos conectados! Agora, só precisamos gerenciar a conexão.
            ...
        */

        if(btSocket != null) {

            /*  Envia um código para a Activity principal informando que a
            a conexão ocorreu com sucesso.
             */
            toMainActivity("---S".getBytes());

            try {

                /*  Obtem referências para os fluxos de entrada e saída do
                socket Bluetooth.
                 */
                input = btSocket.getInputStream();
                output = btSocket.getOutputStream();

                /*  Cria um byte array para armazenar temporariamente uma
                mensagem recebida.
                    O inteiro bytes representará o número de bytes lidos na
                última mensagem recebida.
                 */
                byte[] buffer = new byte[1];

                /*  Permanece em estado de espera até que uma mensagem seja
                recebida.
                    Armazena a mensagem recebida no buffer.
                    Envia a mensagem recebida para a Activity principal, do
                primeiro ao último byte lido.
                    Esta thread permanecerá em estado de escuta até que
                a variável running assuma o valor false.
                 */
                while(running) {

                    bytes = input.read(buffer);
                    toMainActivity(Arrays.copyOfRange(buffer, 0, bytes));
                }

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }
        }

    }

    /*  Utiliza um handler para enviar um byte array à Activity principal.
        O byte array é encapsulado em um Bundle e posteriormente em uma Message
    antes de ser enviado.
     */
    private void toMainActivity(byte[] data) {

        if(bytes>0) {
            if (data[0] == 0b10) {
                leftPressed = true;
                rightPressed = false;
            }

            else if (data[0] == 0b01) {
                rightPressed = true;
                leftPressed = false;
            }
            else{
                rightPressed = false;
                leftPressed = false;
            }
        }

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putByteArray("data", data);
        message.setData(bundle);
        Game.handler.sendMessage(message);
    }

    /*  Método utilizado pela Activity principal para encerrar a conexão
     */
    public void cancel() {

        try {

            running = false;
            btServerSocket.close();
            btSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
    }

    public void pararAlmbos(){
        data = new byte[1];
        data[0] = 0b000;
        //data[0] = 0;
        write(data);
    }

    public void vibrarEsquerda(){
        data = new byte[1];
        data[0] = 0b010;
        //data[0] = 2;
        write(data);
    }

    public void vibrarDireita(){
        data = new byte[1];
        data[0] = 0b001;
        //data[0] = 1;
        write(data);
    }

    public void vibrarAmbos(){
        data = new byte[1];
        data[0] = 0b011;
        //data[0] = 3;
        write(data);
    }

    public void vibrarEsquerdaLerBotoes(){
        data = new byte[1];
        data[0] = 0b110;
        //data[0] = 6;
        write(data);
    }

    public void vibrarDireitaLerBotoes(){
        data = new byte[1];
        data[0] = 0b101;
        //data[0] = 5;
        write(data);
    }

    public void vibrarAmbosLerBotoes(){
        data = new byte[1];
        data[0] = 0b111;
        //data[0] = 7;
        write(data);
    }

    public void pararAlmbosLerBotoes(){
        data = new byte[1];
        data[0] = 0b100;
        //data[0] = 4;
        write(data);
    }

    public void write(byte[] data) {

        if(output != null) {
            try {

                /*  Transmite a mensagem.
                 */
                output.write(data);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            /*  Envia à Activity principal um código de erro durante a conexão.
             */
            toMainActivity("---N".getBytes());
        }
    }
}
