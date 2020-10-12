package com.evstafeeva.client;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class MessageClient {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader inputUser;

    private MessageClientIn inThread;
    private MessageClientOut outThread;

    public MessageClient(String hostName, int port) throws IOException {
        //создали сокет для соединения с сервером
        clientSocket = new Socket(hostName, port);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        inputUser = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        //запускаем ввод и вывод соощений разными потоками=
        inThread = new MessageClientIn();
        inThread.start();
        outThread = new MessageClientOut();
        outThread.start();
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
            } catch (SocketException e) {
                System.out.println("Сервер был закрыт!");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeClient();
                return;
            }
        }
    }

    private class MessageClientOut extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    String message = inputUser.readLine();
                    out.println(message);
                }
            } catch (SocketException e) {
                System.out.println("Сервер был закрыт!");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeClient();
                return;
            }
        }
    }

    private void closeClient() {
        try {
            if (inThread != null)
                inThread.interrupt();
            if (outThread != null)
                outThread.interrupt();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (inputUser != null)
                inputUser.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
