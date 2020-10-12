package com.evstafeeva.server.net;

public interface IChannel {

    void linkToTerminal(ITerminal uplevel);

    void send(int sessionId, byte[] data);

    void close(int sessionId);

}
