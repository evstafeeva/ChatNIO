package com.evstafeeva.server.net;

import com.evstafeeva.server.model.User;

import java.util.HashMap;
import java.util.Map;

public class Chat implements ITerminal {

    private final Map<Integer, User> users = new HashMap<>();
    private IChannel downlevel;

    @Override
    public void onNewSession(int sessionId) {
        users.put(sessionId, new User());
        downlevel.send(sessionId, "имя\n".getBytes());
        System.out.println("Зарегестрирован новый пользователь, id: " + sessionId);
    }

    @Override
    public void onDataReceived(int sessionId, byte[] data) {
        //если проходит регистрация
        User user = users.get(sessionId);
        if (user.getName() == null) {
            String name = getSentenceFromBytes(data);
            user.setName(name);
            System.out.println("Пользователь " + sessionId + " представился под именем " + name);
        } else { //если просто рядовые сообщения в чати
            //формируем сообщение
            String message = "[ " + user.getName() + " ] : "+ getSentenceFromBytes(data) + "\n";
            for (int userId : users.keySet()) {
                if (userId == sessionId)
                    continue;
                downlevel.send(userId, message.getBytes());
            }
        }
    }

    private String getSentenceFromBytes(byte[] data) {
        String str = new String(data).replaceAll("[\n\t\r]", "");
        str = str.substring(0, str.indexOf(0));
        return str;
    }

    @Override
    public void onSessionClosed(int sessionId) {
        users.remove(sessionId);
    }

    @Override
    public void linkToChannel(IChannel downlevel) {
        this.downlevel = downlevel;
    }
}
