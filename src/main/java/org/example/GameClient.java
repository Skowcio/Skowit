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

    private WebSocketClient socket;
    private final Player localPlayer;
    private final Player remotePlayer;
    private final Gson gson = new Gson();



    public GameClient(String serverIP) {
        setPreferredSize(new Dimension(900, 900));
        setBackground(Color.WHITE);

        localPlayer = new Player(450, 450, 5);
        remotePlayer = new Player(300, 300, 5);

        setFocusable(true);
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

        connectToServer(serverIP);
        new Thread(this).start();
    }

    public Player deserializePlayer(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Player.class);
    }

    public void updateRemotePlayer(Player player) {
        remotePlayer.setX(player.getX());
        remotePlayer.setY(player.getY());
    }

    private void connectToServer(String serverIP) {
        try {
            socket = new WebSocketClient(new URI("ws://" + serverIP + ":8887")) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Połączono z serwerem.");
                }

                @Override
                public void onMessage(String message) {
                    Player data = gson.fromJson(message, Player.class);
                    remotePlayer.setX(data.getX());
                    remotePlayer.setY(data.getY());
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Rozłączono z serwerem.");
                }

                @Override
                public void onError(Exception ex) {
                    System.out.println("Błąd klienta: " + ex.getMessage());
                }
            };
            socket.connect();
        } catch (Exception e) {
            System.out.println("Nie udało się połączyć: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        while (true) {
            localPlayer.move(getWidth(), getHeight());

            if (socket != null && socket.isOpen()) {
                socket.send(gson.toJson(localPlayer));
            }

            repaint();
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException ignored) {}
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
        String ip = JOptionPane.showInputDialog("Podaj IP serwera (np. 192.168.0.101):");
        JFrame frame = new JFrame("WebSocket Multiplayer");
        GameClient game = new GameClient(ip);
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
