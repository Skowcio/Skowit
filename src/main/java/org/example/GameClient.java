package org.example;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class GameClient extends JPanel implements Runnable {

    private int remoteTargetX;
    private int remoteTargetY;
    private WebSocketClient socket;
    private final Player localPlayer;
    private final Player remotePlayer;
    private final Gson gson = new Gson();

    private int mouseX = 0;
    private int mouseY = 0;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Point> otherPlayers = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>(); // Dodano pociski
    private final Random random = new Random();

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

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    ShootPacket shootPacket = new ShootPacket();

                    int playerCenterX = localPlayer.getX() + 10;
                    int playerCenterY = localPlayer.getY() + 10;

                    shootPacket.sourceX = playerCenterX;
                    shootPacket.sourceY = playerCenterY;
                    shootPacket.targetX = mouseX;
                    shootPacket.targetY = mouseY;

                    if (socket != null && socket.isOpen()) {
                        socket.send(gson.toJson(shootPacket));
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                localPlayer.handleKeyPress(e.getKeyCode(), false);
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });


        connectToServer(serverIP);
        new Thread(this).start();
    }

    public Player deserializePlayer(String json) {
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
                    try {
                        var jsonObject = gson.fromJson(message, com.google.gson.JsonObject.class);

                        if (jsonObject.has("type")) {
                            String type = jsonObject.get("type").getAsString();

                            switch (type) {
                                case "enemyUpdate" -> {
                                    Enemy[] enemyArray = gson.fromJson(jsonObject.get("enemies"), Enemy[].class);
                                    synchronized (enemies) {
                                        enemies.clear();
                                        for (Enemy e : enemyArray) {
                                            enemies.add(e);
                                        }
                                    }
                                }

                                case "playerUpdate" -> {
                                    PlayerPacket packet = gson.fromJson(message, PlayerPacket.class);
                                    synchronized (otherPlayers) {
                                        otherPlayers.clear();
                                        for (PlayerPacket.PlayerData p : packet.players) {
                                            // Pomijamy lokalnego gracza
                                            if (p.x != localPlayer.getX() || p.y != localPlayer.getY()) {
                                                otherPlayers.add(new Point(p.x, p.y));
                                            }
                                        }
                                    }
                                }

                                case "projectileUpdate" -> {
                                    Projectile[] projectileArray = gson.fromJson(jsonObject.get("projectiles"), Projectile[].class);
                                    synchronized (projectiles) {
                                        projectiles.clear();
                                        for (Projectile p : projectileArray) {
                                            projectiles.add(p);
                                        }
                                    }
                                }
                            }
                        } else {
                            Player data = gson.fromJson(message, Player.class);
                            remoteTargetX = data.getX();
                            remoteTargetY = data.getY();
                        }

                    } catch (Exception e) {
                        System.out.println("Błąd przy parsowaniu wiadomości: " + e.getMessage());
                    }
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

            // Interpolacja pozycji remotePlayer
            int dx = remoteTargetX - remotePlayer.getX();
            int dy = remoteTargetY - remotePlayer.getY();
            remotePlayer.setX(remotePlayer.getX() + dx / 5);
            remotePlayer.setY(remotePlayer.getY() + dy / 5);

            List<Movable> players = new ArrayList<>();
            players.add(localPlayer);

            List<Enemy> enemiesCopy;
            synchronized (enemies) {
                enemiesCopy = new ArrayList<>(enemies);
            }
            for (Enemy enemy : enemiesCopy) {
                enemy.moveTowardsClosest(players);
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

        g.setColor(Color.MAGENTA);
        synchronized (otherPlayers) {
            for (Point p : otherPlayers) {
                g.fillRect(p.x, p.y, 20, 20);
            }
        }

        g.setColor(Color.RED);
        List<Enemy> enemiesCopy;
        synchronized (enemies) {
            enemiesCopy = new ArrayList<>(enemies);
        }
        for (Enemy enemy : enemiesCopy) {
            enemy.draw(g);
        }

        // === RYSOWANIE POCISKÓW ===
        g.setColor(Color.RED);
        synchronized (projectiles) {
            for (Projectile p : projectiles) {
                g.fillOval((int)p.x, (int)p.y, 6, 6);
            }
        }
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
    static class ShootPacket {
        String type = "shoot";
        double sourceX, sourceY;
        double targetX, targetY;
    }

    private static class PlayerPacket {
        String type;
        List<PlayerData> players;

        private static class PlayerData {
            int x, y;
        }
    }
}
