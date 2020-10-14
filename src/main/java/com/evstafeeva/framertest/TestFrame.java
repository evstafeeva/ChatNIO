package com.evstafeeva.framertest;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TestFrame {
    private static BufferedReader in;
    private static PrintWriter out;
    private static final Map<Integer, ByteBuffer> buffers = new HashMap<>();
    private static final int LENGTH_BYTES = 4;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        byte[] bytes1 = "Привет".getBytes();
        byte[] bytes2 = " Алена".getBytes();
        //1 случай, когда сообщение полностью приходит в 2 пакета
        ByteBuffer byteBuffer = ByteBuffer.allocate(16).putInt(bytes1.length+bytes2.length).put(bytes1);
        onDataReceived(1, byteBuffer.array());

        scanner.nextLine();

        ByteBuffer byteBuffer1 = ByteBuffer.allocate(11).put(bytes2);
        onDataReceived(1, byteBuffer1.array());

        scanner.nextLine();

        //2 случай, приходит 2 пакета за 1 раз
        byteBuffer = ByteBuffer.allocate(43)
                .putInt(bytes1.length+bytes2.length)
                .put(bytes1)
                .put(bytes2)
                .putInt(bytes1.length)
                .put(bytes1);
        onDataReceived(1, byteBuffer.array());

        scanner.nextLine();

        //3 случай, сначала приходит пакет не весь, а потом сразу 2
        byteBuffer = ByteBuffer.allocate(16).putInt(bytes1.length+bytes2.length).put(bytes1);
        onDataReceived(1, byteBuffer.array());

        scanner.nextLine();

        byteBuffer = ByteBuffer.allocate(27)
                .put(bytes2)
                .putInt(bytes1.length+bytes2.length)
                .put(bytes1);
        onDataReceived(1, byteBuffer.array());

        scanner.nextLine();

        byteBuffer1 = ByteBuffer.allocate(11).put(bytes2);
        onDataReceived(1, byteBuffer1.array());
    }

    public static void onDataReceived(int sessionId, byte[] data) {
        ByteBuffer incomingMessage;
        ByteBuffer bufferedMessage;
        //если сообщение еще ни разу не приходило, то есть  byte buffer еще не был создан
        if (!buffers.containsKey(sessionId)) {
            //выделяем начальную часть (LENGTH_BYTES = 4 байта), в которой написана длина предстоящего сообщения
            ByteBuffer bb = ByteBuffer.allocate(LENGTH_BYTES).put(data, 0, LENGTH_BYTES);
            int length = ((0xFF & bb.array()[0]) << 24) | ((0xFF & bb.array()[1]) << 16) |
                    ((0xFF & bb.array()[2]) << 8) | (0xFF & bb.array()[3]);
            bufferedMessage = ByteBuffer.allocate(length);
            buffers.put(sessionId, bufferedMessage);
            //отрезаем часть с данными о длине сообщения
            //и далее работаем как с просто пришедшим извне сообщением
            incomingMessage = ByteBuffer.allocate(data.length-LENGTH_BYTES).put(data, LENGTH_BYTES, data.length-LENGTH_BYTES);
        }
        //сообщение уже приходило -> пришла его оставшаяся часть
        else{
            bufferedMessage = buffers.get(sessionId);
            incomingMessage = ByteBuffer.allocate(data.length).put(data);
        }
        incomingMessage.flip();
        //сообщение пришло больше, чем ожидалось для данной длины
        if(incomingMessage.capacity() > bufferedMessage.capacity()) {
            ////доделываем сначала первую часть и отправляем наверх,
            int requiredLength = bufferedMessage.capacity() - bufferedMessage.position();
            bufferedMessage.put(incomingMessage.array(), 0, requiredLength);
            //uplevel.onDataReceived(sessionId, bufferedMessage.array());
            System.out.println(new String(bufferedMessage.array()));
            buffers.remove(sessionId);
            ////потом делаем новое сообщение и ждем его конца(рекурсия)
            onDataReceived(sessionId, ByteBuffer
                    .allocate(incomingMessage.capacity()-requiredLength)
                    .put(incomingMessage.array(), requiredLength, incomingMessage.capacity()-requiredLength)
                    .array());
        } //сообщение пришло не до конца или сообщение пришло полностью битик в битик
        else if(incomingMessage.capacity() <= bufferedMessage.capacity()){
            ////не вызываем метод отправки, просто добавляем в конец
            bufferedMessage.put(incomingMessage.array());
            //если все таки дозаполнилось, отправляем наверх для обработки
            if(bufferedMessage.position() == bufferedMessage.capacity()) {
                //uplevel.onDataReceived(sessionId, bufferedMessage.array());
                System.out.println(new String(bufferedMessage.array()));
                buffers.remove(sessionId);
            }
        }
    }

    private static void printArray(byte[] newData) {
        for (int i = 0; i < newData.length; i++) {
            System.out.print(newData[i] + " ");
        }
        System.out.println("   ");
    }
}
