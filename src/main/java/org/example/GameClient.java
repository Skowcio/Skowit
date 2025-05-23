package org.example;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;

public class GameClient extends JPanel implements Runnable {
    private static final String SERVER_URI = "ws://localhost:8887";

    private Player localPlayer;
    private Player remotePlayer;
    private GameWebSocketClient webSocketClient;
    private static final Gson gson = new Gson();

    public GameClient() {
        setPreferredSize(new Dimension(900, 900));
        setBackground(Color.WHITE);

        localPlayer = new Player(450, 450, 5);
        remotePlayer = new Player(300, 300, 5);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                localPlayer.handleKeyPress(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                localPlayer.handleKeyPress(e.getKeyCode(), false);
            }
        });

        setFocusable(true);
        connectToServer();
        new Thread(this).start();
    }

    private void connectToServer() {
        try {
            webSocketClient = new GameWebSocketClient(new URI(SERVER_URI), this);
            webSocketClient.connect();
        } catch (Exception e) {
            System.out.println("Nie można połączyć z serwerem WebSocket.");
        }
    }

    public void updateRemotePlayer(Player p) {
        this.remotePlayer = p;
    }

    public String serializePlayer(Player p) {
        return gson.toJson(p);
    }

    public Player deserializePlayer(String json) {
        return gson.fromJson(json, Player.class);
    }

    @Override
    public void run() {
        while (true) {
            localPlayer.move(getWidth(), getHeight());
            sendPlayerData();
            repaint();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                // Ignoruj
            }
        }
    }

    private void sendPlayerData() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            String json = serializePlayer(localPlayer);
            webSocketClient.send(json);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLUE);
        localPlayer.draw(g);
        g.setColor(Color.GREEN);
        remotePlayer.draw(g);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("WebSocket LAN Game");
        GameClient game = new GameClient();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
