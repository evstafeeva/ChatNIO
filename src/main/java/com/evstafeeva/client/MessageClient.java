package com.evstafeeva.client;

import java.io.*;
import java.net.Socket;

public class MessageClient {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader inputUser;

    public MessageClient(String hostName, int port) throws IOException {
        //создали сокет для соединения с сервером
        clientSocket = new Socket(hostName, port);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        inputUser = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() throws IOException {
        try {
//            //регистрация
//            System.out.println(in.readLine());
//            out.println(inputUser.readLine());
            //запускаем ввод и вывод соощений разными потоками=
            new MessageClientIn().start();
            new MessageClientOut().start();
        } finally {

        }
    }

    private class MessageClientIn extends Thread {
        @Override
        public void run() {
            String string;
            try {
                while (true) {
                    string = in.readLine();
                    System.out.println(string);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class MessageClientOut extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    String message =  inputUser.readLine();
                    out.println(message);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
