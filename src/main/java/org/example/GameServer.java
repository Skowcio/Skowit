package org.example;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.gson.Gson;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GameServer extends WebSocketServer {

    private static final int PORT = 8887;
    private static final Gson gson = new Gson();
    private final Set<WebSocket> clients = Collections.synchronizedSet(new HashSet<>());

    public GameServer() {
        super(new InetSocketAddress(PORT));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        System.out.println("Nowy klient połączony: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        System.out.println("Klient rozłączony: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Broadcast do wszystkich innych klientów
        synchronized (clients) {
            for (WebSocket client : clients) {
                if (client != conn && client.isOpen()) {
                    client.send(message);
                }
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Błąd: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Serwer działa na porcie " + PORT);
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
}
