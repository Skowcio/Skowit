package org.example;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class GameServer extends WebSocketServer {

    private final Set<WebSocket> clients = new HashSet<>();
    private final Gson gson = new Gson();

    public GameServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        System.out.println("Nowy klient połączony: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        System.out.println("Klient się rozłączył: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Broadcast do wszystkich klientów
        for (WebSocket client : clients) {
            if (client != conn && client.isOpen()) {
                client.send(message);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("Błąd serwera: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Serwer WebSocket działa na porcie: " + getPort());
    }

    public static void main(String[] args) {
        new GameServer(8887).start();
    }
}
