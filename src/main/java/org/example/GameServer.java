package org.example;

import com.google.gson.Gson;


import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GameServer extends WebSocketServer {

    private Set<WebSocket> connections = Collections.synchronizedSet(new HashSet<>());
    private Gson gson = new Gson();

    public GameServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("Połączono: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("Rozłączono: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Broadcast to others
        for (WebSocket client : connections) {
            if (client != conn) {
                client.send(message);
            }
        }

        // Debug: wyświetl dane gracza
        Player p = gson.fromJson(message, Player.class);
        System.out.println("Dane gracza: " + p);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Błąd: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Serwer działa na porcie " + getPort());
    }

    public static void main(String[] args) {
        GameServer server = new GameServer(8887);
        server.start();
    }
}
