package com.evstafeeva.server.net;

import java.nio.ByteBuffer;

public interface ITerminal {

    void linkToChannel(IChannel downlevel);

    void onNewSession(int sessionId);

    void onDataReceived(int sessionId, byte[] data);

    void onSessionClosed(int sessionId);
}
