package com.evstafeeva.client;

import java.io.IOException;

public class ClientMain {
    public static void main(String[] args) {
        try {
            MessageClient messageClient = new MessageClient("localhost", 5555);
            messageClient.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
