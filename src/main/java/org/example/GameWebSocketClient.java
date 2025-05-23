package org.example;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class GameWebSocketClient extends WebSocketClient {

    private GameClient gameClient;

    public GameWebSocketClient(URI serverUri, GameClient gameClient) {
        super(serverUri);
        this.gameClient = gameClient;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Połączono z serwerem WebSocket");
    }

    @Override
    public void onMessage(String message) {
        Player receivedPlayer = gameClient.deserializePlayer(message);
        gameClient.updateRemotePlayer(receivedPlayer);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Połączenie WebSocket zamknięte: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Błąd WebSocket: " + ex.getMessage());
    }
}
