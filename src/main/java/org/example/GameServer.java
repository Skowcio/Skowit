package org.example;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;

public class GameServer extends WebSocketServer {

    private final Set<WebSocket> clients = new HashSet<>();
    private final Map<WebSocket, Player> players = new HashMap<>(); // <- nowość
    private final Gson gson = new Gson();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random random = new Random();

    public GameServer(int port) {
        super(new InetSocketAddress("0.0.0.0", port));
        spawnEnemies(5);
        startEnemyUpdateLoop();
    }

    private void spawnEnemies(int count) {
        enemies.clear();
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(880);
            int y = random.nextInt(880);
            enemies.add(new Enemy(x, y, 2));
        }
    }

    private void startEnemyUpdateLoop() {
        new Thread(() -> {
            while (true) {
                List<Player> playerList;
                synchronized (players) {
                    playerList = new ArrayList<>(players.values());
                }

                for (Enemy enemy : enemies) {
                    if (playerList.isEmpty()) continue;

                    // Znajdź najbliższego gracza
                    Player closest = null;
                    double minDist = Double.MAX_VALUE;
                    for (Player p : playerList) {
                        int dx = p.x - enemy.x;
                        int dy = p.y - enemy.y;
                        double dist = Math.sqrt(dx * dx + dy * dy);
                        if (dist < minDist) {
                            minDist = dist;
                            closest = p;
                        }
                    }

                    // Ruch w stronę gracza
                    if (closest != null && minDist > 0) {
                        int dx = closest.x - enemy.x;
                        int dy = closest.y - enemy.y;
                        enemy.x += (int) (enemy.speed * dx / minDist);
                        enemy.y += (int) (enemy.speed * dy / minDist);
                        enemy.x = Math.max(0, Math.min(880, enemy.x));
                        enemy.y = Math.max(0, Math.min(880, enemy.y));
                    }
                }

                String update = gson.toJson(new EnemyPacket(enemies));

                for (WebSocket client : clients) {
                    if (client.isOpen()) {
                        client.send(update);
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        System.out.println("Nowy klient połączony: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        synchronized (players) {
            players.remove(conn); // <- usuń gracza
        }
        System.out.println("Klient się rozłączył: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Player player = gson.fromJson(message, Player.class);
            synchronized (players) {
                players.put(conn, player); // <- aktualizuj pozycję gracza
            }
        } catch (Exception e) {
            System.out.println("Nie udało się sparsować wiadomości jako Player: " + message);
        }

        // Nie przesyłaj dalej - dane są do serwera
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("Błąd serwera: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Serwer WebSocket działa na porcie: " + getPort());
    }

    // --- KLASY POMOCNICZE ---

    static class Enemy {
        int x, y, speed;
        Enemy(int x, int y, int speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }
    }

    static class EnemyPacket {
        String type = "enemyUpdate";
        List<Enemy> enemies;
        EnemyPacket(List<Enemy> enemies) {
            this.enemies = enemies;
        }
    }

    static class Player {
        int x, y, speed;
        Player(int x, int y, int speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }
    }

    public static void main(String[] args) {
        new GameServer(8887).start();
    }
}
