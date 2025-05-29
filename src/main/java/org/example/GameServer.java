package org.example;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.*;

public class GameServer extends WebSocketServer {

    private final Set<WebSocket> clients = new HashSet<>();
    private final Map<WebSocket, Player> players = new HashMap<>();
    private final Gson gson = new Gson();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random random = new Random();
    private final ProjectileManager projectileManager = new ProjectileManager(); // [DODANE]
    private static final int MAX_ENEMIES = 100;
    List<WebSocket> playersToRemove = new ArrayList<>();

    public GameServer(int port) {
        super(new InetSocketAddress("0.0.0.0", port));
        spawnEnemies(5);
        startEnemyUpdateLoop();
        startEnemyRespawnLoop(); // <-- dodane

    }

    private void spawnEnemies(int count) {
        enemies.clear();
        for (int i = 0; i < count; i++) {
            double x = random.nextInt(880);
            double y = random.nextInt(880);
            enemies.add(new Enemy(x, y, 2));
        }
    }

    private void startEnemyRespawnLoop() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // 10 sekund
                } catch (InterruptedException ignored) {}

                synchronized (enemies) {
                    int toSpawn = MAX_ENEMIES - enemies.size();
                    if (toSpawn > 0) {
                        int spawnCount = Math.min(10, toSpawn);
                        int spawned = 0;

                        List<Player> playerList;
                        synchronized (players) {
                            playerList = new ArrayList<>(players.values());
                        }

                        while (spawned < spawnCount) {
                            double x = random.nextInt(880);
                            double y = random.nextInt(880);

                            boolean tooClose = false;
                            for (Player p : playerList) {
                                double dx = p.getX() - x;
                                double dy = p.getY() - y;
                                double dist = Math.sqrt(dx * dx + dy * dy);
                                if (dist < 100) {
                                    tooClose = true;
                                    break;
                                }
                            }

                            if (!tooClose) {
                                enemies.add(new Enemy(x, y, 2));
                                spawned++;
                            }
                        }

                        System.out.println("Dodano " + spawned + " nowych przeciwników (poza graczami). Łącznie: " + enemies.size());
                    }
                }
            }
        }).start();
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

                    Player closest = null;
                    double minDist = Double.MAX_VALUE;
                    for (Player p : playerList) {
                        double dx = p.getX() - enemy.x;
                        double dy = p.getY() - enemy.y;
                        double dist = Math.sqrt(dx * dx + dy * dy);
                        if (dist < minDist) {
                            minDist = dist;
                            closest = p;
                        }
                    }

                    if (closest != null && minDist > 0) {
                        double dx = closest.getX() - enemy.x;
                        double dy = closest.getY() - enemy.y;
                        double dist = Math.sqrt(dx * dx + dy * dy);
                        enemy.x += enemy.speed * dx / dist;
                        enemy.y += enemy.speed * dy / dist;

                        enemy.x = Math.max(0, Math.min(880, enemy.x));
                        enemy.y = Math.max(0, Math.min(880, enemy.y));
                    }

                    // [NOWE] Sprawdź kolizje z każdym graczem
                    synchronized (players) {
                        for (Map.Entry<WebSocket, Player> entry : players.entrySet()) {
                            Player p = entry.getValue();
                            double dx = p.getX() - enemy.x;
                            double dy = p.getY() - enemy.y;
                            double dist = Math.sqrt(dx * dx + dy * dy);
                            if (dist < 20) { // dystans kontaktu
                                playersToRemove.add(entry.getKey());
                            }
                        }
                    }
                }
                for (WebSocket conn : playersToRemove) {
                    players.remove(conn);
                    conn.close(); // rozłącz klienta opcjonalnie
                    System.out.println("Gracz usunięty po kontakcie z wrogiem: " + conn.getRemoteSocketAddress());
                }

                // [DODANE] AKTUALIZACJA POCISKÓW
                projectileManager.update(enemies);
                List<Projectile> activeProjectiles = projectileManager.getProjectiles();
                String projectileUpdate = gson.toJson(new ProjectilePacket(activeProjectiles)); // [DODANE]

                String update = gson.toJson(new EnemyPacket(enemies));
                String playerUpdate = gson.toJson(new PlayerPacket(playerList));

                for (WebSocket client : clients) {
                    if (client.isOpen()) {
                        client.send(update);
                        client.send(playerUpdate);
                        client.send(projectileUpdate); // [DODANE]
                    }
                }

                try {
                    Thread.sleep(16); // ~60 FPS
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
            players.remove(conn);
        }
        System.out.println("Klient się rozłączył: " + conn.getRemoteSocketAddress());
    }

    // [ZMIENIONE] Obsługa wiadomości od klienta (gracz lub strzał)
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            if (message.startsWith("{\"type\":\"shoot\"")) { // [DODANE]
                ShootPacket packet = gson.fromJson(message, ShootPacket.class);
                Player shooter = players.get(conn);
                if (shooter != null) {
                    double dx = packet.targetX - shooter.getX();
                    double dy = packet.targetY - shooter.getY();
                    double angle = Math.atan2(dy, dx);
                    projectileManager.shoot(shooter.getX(), shooter.getY(), angle);
                }
            } else {
                Player player = gson.fromJson(message, Player.class);
                synchronized (players) {
                    players.put(conn, player);
                }
            }
        } catch (Exception e) {
            System.out.println("Błąd onMessage: " + e.getMessage());
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

    // --- KLASY POMOCNICZE ---

    static class Enemy {
        double x, y;
        int speed;

        Enemy(double x, double y, int speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }
    }

    static class PlayerPacket {
        String type = "playerUpdate";
        List<PlayerData> players;

        PlayerPacket(List<Player> playerList) {
            players = new ArrayList<>();
            for (Player p : playerList) {
                players.add(new PlayerData(p.getX(), p.getY()));
            }
        }

        static class PlayerData {
            int x, y;

            PlayerData(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }
    }

    static class EnemyPacket {
        String type = "enemyUpdate";
        List<EnemyData> enemies;

        EnemyPacket(List<Enemy> enemies) {
            this.enemies = new ArrayList<>();
            for (Enemy e : enemies) {
                this.enemies.add(new EnemyData((int) e.x, (int) e.y));
            }
        }

        static class EnemyData {
            int x, y;

            EnemyData(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }
    }

    // [DODANE] Packet przyjmujący dane od klienta do strzelania
    static class ShootPacket {
        String type = "shoot";
        double sourceX, sourceY;
        double targetX, targetY;
    }

    // [DODANE] Packet wysyłający listę aktywnych pocisków do klientów
    static class ProjectilePacket {
        String type = "projectileUpdate";
        List<ProjectileData> projectiles;

        ProjectilePacket(List<Projectile> list) {
            projectiles = new ArrayList<>();
            for (Projectile p : list) {
                projectiles.add(new ProjectileData((int) p.x, (int) p.y));
            }
        }

        static class ProjectileData {
            int x, y;

            ProjectileData(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }
    }

    public static void main(String[] args) {
        new GameServer(8887).start();
    }
}
