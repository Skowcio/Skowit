package org.example;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.*;
import org.example.Enemy;

public class GameServer extends WebSocketServer {

    private final Set<WebSocket> clients = new HashSet<>();
    private final Map<WebSocket, Player> players = new HashMap<>();
    private final Gson gson = new Gson();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random random = new Random();
    private final ProjectileManager projectileManager = new ProjectileManager(); // [DODANE]
    private static final int MAX_ENEMIES = 10;
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
            int x = random.nextInt(880);
            int y = random.nextInt(880);
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
                            int x = random.nextInt(880);
                            int y = random.nextInt(880);

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
                                enemies.add(new Enemy(x, y, 2)); // użycie klasy org.example.Enemy
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

                // Przerzuć do listy Movable
                List<Movable> movablePlayers = new ArrayList<>(playerList);

                synchronized (enemies) {
                    for (Enemy enemy : enemies) {
                        if (movablePlayers.isEmpty()) continue;

                        // Ruch przeciwnika w kierunku najbliższego gracza
                        enemy.moveTowardsClosest(movablePlayers);

                        // Ograniczenie pozycji do pola 880x880
                        int newX = Math.max(0, Math.min(880, enemy.getX()));
                        int newY = Math.max(0, Math.min(880, enemy.getY()));
                        enemy.setX(newX);
                        enemy.setY(newY);

                        // Kolizje z graczami
                        synchronized (players) {
                            for (Map.Entry<WebSocket, Player> entry : players.entrySet()) {
                                Player p = entry.getValue();
                                double dx = p.getX() - enemy.getX();
                                double dy = p.getY() - enemy.getY();
                                double dist = Math.sqrt(dx * dx + dy * dy);
                                if (dist < 20) { // kontakt
                                    playersToRemove.add(entry.getKey());
                                }
                            }
                        }
                    }
                }

                // Usunięcie graczy po kolizji
                for (WebSocket conn : playersToRemove) {
                    players.remove(conn);
                    conn.close();
                    System.out.println("Gracz usunięty po kontakcie z wrogiem: " + conn.getRemoteSocketAddress());
                }
                playersToRemove.clear();

                // Aktualizacja pocisków
                projectileManager.update(enemies);
                List<Projectile> activeProjectiles = projectileManager.getProjectiles();
                String projectileUpdate = gson.toJson(new ProjectilePacket(activeProjectiles));

                // Serializacja danych
                String update = gson.toJson(new EnemyPacket(enemies));
                String playerUpdate = gson.toJson(new PlayerPacket(playerList));

                // Wysyłanie do klientów
                for (WebSocket client : clients) {
                    if (client.isOpen()) {
                        client.send(update);
                        client.send(playerUpdate);
                        client.send(projectileUpdate);
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
