package com.evstafeeva.server.net;

import java.nio.ByteBuffer;
import java.util.Map;

public class Framer implements IChannel, ITerminal {

    //private Map<Integer, ByteBuffer> buffers; //потребуется для фреймирования, но реализуем позже
    private ITerminal uplevel;
    private IChannel downlevel;

    @Override
    public void linkToChannel(IChannel downlevel) {
        this.downlevel = downlevel;
    }

    @Override
    public void linkToTerminal(ITerminal uplevel) {
        this.uplevel = uplevel;
    }

    @Override
    public void send(int sessionId, byte[] data) {
        //int length = data.length;
        // TODO: length < 0xFFFF
        //short length = data.length;
        //length16[0] = (byte)((length & 0xFF00) >> 8);
        //length16[1] = (byte)(length & 0xFF);

        //по сути здесь должна быть нарезка на пакеты, которые на клиенте будут склеивать
        //но реализацию пока отпустим, чтобы проверить как работает все.
        this.downlevel.send(sessionId, data);
    }

    @Override
    public void close(int sessionId) {
        //на верхнем уровне что-то произошло, надо закрыть канал на нижнем уровне
        //buffers.remove(sessionId);
        downlevel.close(sessionId);
    }

    @Override
    public void onDataReceived(int sessionId, byte[] data) {
        //какие-то данные пришли с нижнего уровня, на которые надо ответить
        //ByteBuffer buffer = buffers.get(sessionId);
        //buffer.put(data);
        //так как используем TCP, необходимо проверять, все ли пакеты дошли.
        //собственно для этого и нужен framer
        //но пока поживем так, реализуем потом
        uplevel.onDataReceived(sessionId, data);
    }

    @Override
    public void onNewSession(int sessionId) {
        //нижний уровень зарегестрировал новый акк
        //buffers.put(sessionId, ByteBuffer.allocate(0xFFFF));
        uplevel.onNewSession(sessionId);
    }

    @Override
    public void onSessionClosed(int sessionId) {
        //какая-то сессия была закрыта
        //buffers.remove(sessionId);
        uplevel.onSessionClosed(sessionId);
    }
}
