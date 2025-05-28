package org.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProjectileManager {
    private final List<Projectile> projectiles = new ArrayList<>();

    public void shoot(double x, double y, double angle) {
        projectiles.add(new Projectile(x, y, angle, 5));
    }

    public void update(List<GameServer.Enemy> enemies) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile p = iterator.next();
            p.x += p.vx;
            p.y += p.vy;

            if (p.isExpired()) {
                iterator.remove();
                continue;
            }

            Iterator<GameServer.Enemy> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                GameServer.Enemy e = enemyIterator.next();
                double dx = e.x - p.x;
                double dy = e.y - p.y;
                if (Math.sqrt(dx * dx + dy * dy) < 15) {
                    enemyIterator.remove();
                    iterator.remove();
                    break;
                }
            }
        }
    }

    public List<Projectile> getProjectiles() {
        return new ArrayList<>(projectiles);
    }
}
