package com.evstafeeva.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Server {
    private final ServerSocketChannel server;
    private final Selector selector;
    private Map<SelectionKey, ByteBuffer> selectionKeysMap;
    private byte[] buffer = new byte[2048];

    public Server(InetSocketAddress inetSocketAddress) throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(inetSocketAddress);
        server.socket().setReuseAddress(true);
        //делаем сервер неблокирующим (настравиаем режим)
        server.configureBlocking(false);

        //Когда активность ввода-вывода происходит на каком-либо из каналов, селектор уведомляет нас
        selector = Selector.open();
        //Чтобы селектор мог отслеживать любые каналы, мы должны зарегистрировать селектор на сервере
        server.register(selector, SelectionKey.OP_ACCEPT);

        //создаем мапу, где будут хранится все присоединившиеся
        selectionKeysMap = new HashMap<>();

        System.out.println("Сервер запущен");
    }

    public void startСommunication() throws IOException {
        //теперь начинаем непрервывный процесс выбора готового набора
        while (true) {
            //метод блокируемтся пока хотя бы один канал не будет готов к работе (выбрасывания какого-либо события).
            //возвращается целое число каналов, готовых к операции
            int channelCount = selector.select();
            //получаем набор выбранных ключей, готовых к обработке
            //каждый ключ - это канал, готовый к обработке
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            //перебираем набор с помощью иттератора
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                //проверяем, действителен ли ключ
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
            }
        }
    }

    private void registration() throws IOException {
        //так как кто-то ожидает коннект, сразу принимаем
        SocketChannel client = server.accept();
        //отключаем нелокирующий режим
        client.configureBlocking(false);
        //подписываемся только на события прихода данных
        //и сразу сохраняем его в список присоединившихся
        //заодно прикрепляем для него свой буфер
        selectionKeysMap.put(client.register(selector, SelectionKey.OP_READ), ByteBuffer.wrap(buffer));
        System.out.println("подсоединился " + client.toString());
    }

    private void readMessage(SelectionKey sKey) throws IOException {
        //получаем канал, который хочет нам что-то сказать
        SocketChannel client = (SocketChannel) sKey.channel();
        System.out.println("читаем " + client.toString());
        ByteBuffer buffer = selectionKeysMap.get(sKey);
        buffer.clear();
        //читаем, что нам пришло (пытаемся)
        int read = 0;
        try {
            read = client.read(buffer);
        } catch (IOException e) {
            //клиент отпал -> закрываем его и удаляем из списка подключенных клиентов
            closeChanel(sKey);
        }
        //если ничего не пришло или пришло -1(отвалился сам)
        if (read < 0) {
            closeChanel(sKey);
        } else {//все-таки что-то пришло и все идет по плану
            buffer.flip();
            final int pos = buffer.position();
            final int lim = buffer.limit();
            for (SelectionKey sk : selectionKeysMap.keySet()) {
                //переключаем в режим, хочу писать
                sk.interestOps(SelectionKey.OP_WRITE);
                ByteBuffer byteBuffer = selectionKeysMap.get(sk);
                //настраиваем, чтобы праивльно писать в сокет
                byteBuffer.position(pos);
                byteBuffer.limit(lim);
            }
        }
    }

    private void writeMessage(SelectionKey sKey) throws IOException {
        //смотрю, что в него надо записать
        ByteBuffer byteBuffer = selectionKeysMap.get(sKey);
        //получаю канал, куда писать
        SocketChannel client = (SocketChannel) sKey.channel();
        System.out.println("пишем в " + client.toString());
        //пытаюсь записать
        try{
            int result = client.write(byteBuffer);
            if(result == -1){
                //запись не получилась, так как socket properly closed
                closeChanel(sKey);
            }
        } catch (IOException e) {
            //если отвалился клиент, просто закрываем его
            closeChanel(sKey);
        }
        //ежели было запиано все, что хотелось, переводим ключ в режим ожидания ответа от него
        if(byteBuffer.position() == byteBuffer.limit()){
            sKey.interestOps(SelectionKey.OP_READ);
        }
    }

    private void closeChanel(SelectionKey sKey) throws IOException {
        selectionKeysMap.remove(sKey);
        SocketChannel client = (SocketChannel) sKey.channel();
        System.out.println("клиент отключен " + client.toString());
        if (client.isConnected()) {
            client.close();
        }
        sKey.cancel();
    }

}
