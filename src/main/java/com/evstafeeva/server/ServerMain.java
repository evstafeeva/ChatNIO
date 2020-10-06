package com.evstafeeva.server;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerMain {
    public static void main(String[] args) {
        try {
            Server server = new Server(new InetSocketAddress("localhost", 5555));
            server.startСommunication();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
