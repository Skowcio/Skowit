package org.example;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;


public class Enemy {
    private int x, y, speed;

    public Enemy(int x, int y, int speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void moveTowardsClosest(List<Movable> players) {
        Movable closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Movable p : players) {
            int dx = p.getX() - x;
            int dy = p.getY() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = p;
            }
        }

        if (closest != null && closestDistance > 0) {
            int dx = closest.getX() - x;
            int dy = closest.getY() - y;
            x += (int) (speed * dx / closestDistance);
            y += (int) (speed * dy / closestDistance);
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(x, y, 20, 20);
    }
}
