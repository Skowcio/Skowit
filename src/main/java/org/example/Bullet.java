package org.example;

import java.awt.*;

public class Bullet {
    private int x, y, dx, dy;

    public Bullet(int startX, int startY, int targetX, int targetY) {
        x = startX;
        y = startY;
        int speed = 10;
        double angle = Math.atan2(targetY - startY, targetX - startX);
        dx = (int) (speed * Math.cos(angle));
        dy = (int) (speed * Math.sin(angle));
    }

    public void move() {
        x += dx;
        y += dy;
    }

    public boolean isColliding(Enemy enemy) {
        return Math.abs(x - enemy.getX()) < 20 && Math.abs(y - enemy.getY()) < 20;
    }

    public boolean isOutOfBounds(int width, int height) {
        return x < 0 || x > width || y < 0 || y > height;
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillOval(x, y, 10, 10);
    }
}
