package com.evstafeeva.server.net;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Framer implements IChannel, ITerminal {

    private final Map<Integer, ByteBuffer> buffers = new HashMap<>(); //потребуется для фреймирования, но реализуем позже
    private ITerminal uplevel;
    private IChannel downlevel;

    private static final int LENGTH_BYTES = 4;

    @Override
    public void send(int sessionId, byte[] data) {
        //вычисляем длину сообщения data
        //максимальная длина сообщения 4 294 967 295 = 1111 1111 1111 1111 1111 1111 1111 1111 -> LENGTH_BYTES = 4 bytes
        //созадем новое сообщение, которое склеиваем из LENGTH_BYTES = 4 байтов для длины + самого сообщения
        //отправляем на нижний слой
        this.downlevel.send(sessionId,
                ByteBuffer.allocate(LENGTH_BYTES + data.length)
                .putInt(data.length)
                .put(data)
                .array());
    }

    @Override
    public void close(int sessionId) {
        //на верхнем уровне что-то произошло, надо закрыть канал на нижнем уровне
        buffers.remove(sessionId);
        downlevel.close(sessionId);
    }

    @Override
    public void onDataReceived(int sessionId, byte[] data) {
        ByteBuffer incomingMessage;
        ByteBuffer bufferedMessage;
        //если сообщение еще ни разу не приходило, то есть  byte buffer еще не был создан
        if (!buffers.containsKey(sessionId)) {
            //выделяем начальную часть (LENGTH_BYTES = 4 байта), в которой написана длина предстоящего сообщения
            int length = ByteBuffer.allocate(LENGTH_BYTES).put(data, 0, LENGTH_BYTES).getInt();
            bufferedMessage = ByteBuffer.allocate(length);
            buffers.put(sessionId, bufferedMessage);
            //отрезаем часть с данными о длине сообщения
            //и далее работаем как с просто пришедшим извне сообщением
            incomingMessage = ByteBuffer.allocate(data.length-4).put(data, 4, data.length);
        }
        //сообщение уже приходило -> пришла его оставшаяся часть
        else{
            bufferedMessage = buffers.get(sessionId);
            incomingMessage = ByteBuffer.allocate(data.length).put(data);
        }
        //сообщение пришло больше, чем ожидалось для данной длины
        if(incomingMessage.capacity() > bufferedMessage.capacity()) {
            ////доделываем сначала первую часть и отправляем наверх,
            int requiredLength = bufferedMessage.capacity() - bufferedMessage.limit();
            bufferedMessage.put(incomingMessage.array(), 0, requiredLength);
            uplevel.onDataReceived(sessionId, bufferedMessage.array());
            buffers.remove(sessionId);
            ////потом делаем новое сообщение и ждем его конца(рекурсия)
            onDataReceived(sessionId, ByteBuffer
                    .wrap(incomingMessage.array(), requiredLength, incomingMessage.capacity())
                    .array());
        } //сообщение пришло не до конца или сообщение пришло полностью битик в битик
        else if(incomingMessage.capacity() <= bufferedMessage.capacity()){
            ////не вызываем метод отправки, просто добавляем в конец
            bufferedMessage.put(incomingMessage);
            //если все таки дозаполнилось, отправляем наверх для обработки
            if(bufferedMessage.limit() == bufferedMessage.capacity()) {
                uplevel.onDataReceived(sessionId, bufferedMessage.array());
                buffers.remove(sessionId);
            }
        }
    }

    @Override
    public void onNewSession(int sessionId) {
        //нижний уровень зарегестрировал новый акк
        uplevel.onNewSession(sessionId);
    }

    @Override
    public void onSessionClosed(int sessionId) {
        //какая-то сессия была закрыта
        buffers.remove(sessionId);
        uplevel.onSessionClosed(sessionId);
    }

    @Override
    public void linkToChannel(IChannel downlevel) {
        this.downlevel = downlevel;
    }

    @Override
    public void linkToTerminal(ITerminal uplevel) {
        this.uplevel = uplevel;
    }

}
