package com.evstafeeva.server.net;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioServer implements IChannel {

    private final ServerSocketChannel server;
    private final Selector selector;

    private final Map<SelectionKey, ByteBuffer> buffersForWriting = new HashMap<>();

    private BiMap<Integer, SelectionKey> sessions;  //соотношение соединения с его уникаьным ключом.
    private int nextSessionId = 0;

    private ITerminal uplevel;

    public NioServer(InetSocketAddress inetSocketAddress) throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(inetSocketAddress);
        server.socket().setReuseAddress(true);
        //делаем сервер неблокирующим
        server.configureBlocking(false);

        //Когда активность ввода-вывода происходит на каком-либо из каналов, селектор уведомляет нас
        selector = Selector.open();
        //Чтобы селектор мог отслеживать любые каналы, мы должны зарегистрировать селектор на сервере
        server.register(selector, SelectionKey.OP_ACCEPT);

        sessions = HashBiMap.create();

        System.out.println("Сервер запущен");
    }

    public void startСommunication() throws IOException {
        //теперь начинаем непрервывный процесс выбора готового набора

        while (true) {
            //метод блокируемтся пока хотя бы один канал не будет готов к работе (выбрасывания какого-либо события).
            //возвращается целое число каналов, готовых к операции
            selector.select();
            //получаем набор выбранных ключей, готовых к обработке
            //каждый ключ - это канал, готовый к обработке
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            //перебираем набор с помощью иттератора
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                //проверяем, действителен ли ключ
                try {
                    if (!key.isValid()) {
                        iterator.remove();
                        continue;
                    }
                    //кто-то хочет подрубиться
                    if (key.isAcceptable()) {
                        registration();
                    }
                    //что-то все-таки пришло
                    if (key.isReadable()) {
                        readMessage(key);
                    }
                    //что-то надо в него записать(это что-то лежит у негов буффере)
                    if (key.isWritable()) {
                        writeMessage(key);
                    }
                    iterator.remove();
                } catch (CancelledKeyException e) {
                }
            }
        }
    }

    private void registration() throws IOException {
        //так как кто-то ожидает коннект, сразу принимаем
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        //подписываемся только на события прихода данных
        //и сразу сохраняем его в список присоединившихся
        //заодно прикрепляем для него свой буфер
        SelectionKey selectionKey = client.register(selector, SelectionKey.OP_READ);

        //делаем связь с uplevel
        int sessionId = nextSessionId++;
        System.out.println("подсоединился " + client.toString() + " (sessionId = " + sessionId + ")");
        sessions.put(sessionId, selectionKey);
        uplevel.onNewSession(sessionId);

    }

    private void readMessage(SelectionKey sKey) {
        //получаем канал, который хочет нам что-то сказать
        SocketChannel client = (SocketChannel) sKey.channel();
        System.out.println("читаем " + client.toString());
        //TODO сделать оптимальннее, чтобы каждый раз не выделять заново
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        //читаем, что нам пришло (пытаемся)
        int read = 0;
        try {
            read = client.read(buffer);
        } catch (IOException e) {
            //клиент отпал -> закрываем его и удаляем из списка подключенных клиентов
            close(sessions.inverse().get(sKey));
            return;
        }
        //если ничего не пришло или пришло -1(отвалился сам)
        if (read < 0) {
            close(sessions.inverse().get(sKey));
        } else {//все-таки что-то пришло и все идет по плану
            uplevel.onDataReceived(sessions.inverse().get(sKey), buffer.array());
        }
    }

    private void writeMessage(SelectionKey sKey) {
        //получаю канал, куда писать
        SocketChannel client = (SocketChannel) sKey.channel();
        System.out.println("пишем в " + client.toString());
        ByteBuffer buffer = buffersForWriting.get(sKey);
        //пытаюсь записать
        try {
            int result = client.write(buffer);
            if (result == -1) {
                //запись не получилась, так как socket properly closed
                close(sessions.inverse().get(sKey));
                return;
            }
        } catch (IOException e) {
            //если отвалился клиент, просто закрываем его
            close(sessions.inverse().get(sKey));
            return;
        }
        //ежели было запиано все, что хотелось, переводим ключ в режим ожидания ответа от него
        if (buffer.position() == buffer.limit()) {
            buffersForWriting.remove(sKey);
            sKey.interestOps(SelectionKey.OP_READ);
        }
    }

    @Override
    public void send(int sessionId, byte[] data) {
        //получаю sk, куда писать
        SelectionKey sk = sessions.get(sessionId);
        //добавляю, что писать
        buffersForWriting.put(sk, ByteBuffer.wrap(data));
        //меняю режим, чтобы sk добавился в очередь для передачи данных в канал
        sk.interestOps(SelectionKey.OP_WRITE);
    }

    @Override
    public void linkToTerminal(ITerminal uplevel) {
        this.uplevel = uplevel;
    }


    @Override
    public void close(int sessionId) {
        //TODO проверить правильность закрытия примерно в конце реализации всего остального функционала
        SelectionKey sk = sessions.get(sessionId);
        sessions.remove(sessionId);
        SocketChannel client = (SocketChannel) sk.channel();
        System.out.println("клиент отключен " + client.toString());
        try {
            if (client.isConnected())
                client.close();
        } catch (IOException e) {
            System.out.println("Клиент отключился раньше, чем это сделали мы");
        }
        sk.cancel();
        uplevel.onSessionClosed(sessionId);
    }
}
