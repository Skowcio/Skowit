package org.example;

public class Projectile {
    public double x, y;
    public double vx, vy;
    public long creationTime;

    public Projectile(double x, double y, double angle, double speed) {
        this.x = x;
        this.y = y;
        this.vx = speed * Math.cos(angle);
        this.vy = speed * Math.sin(angle);
        this.creationTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > 2000;
    }
}
