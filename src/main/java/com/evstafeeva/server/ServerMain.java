package com.evstafeeva.server;

import com.evstafeeva.server.net.Chat;
import com.evstafeeva.server.net.Framer;
import com.evstafeeva.server.net.NioServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerMain {
    public static void main(String[] args) throws IOException {

        Chat chat = new Chat();
        Framer framer = new Framer();
        NioServer nioServer = new NioServer(new InetSocketAddress("localhost", 5555));

        chat.linkToChannel(framer);
        framer.linkToTerminal(chat);
        framer.linkToChannel(nioServer);
        nioServer.linkToTerminal(framer);

        nioServer.startСommunication();
    }
}
