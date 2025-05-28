package org.example;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.Serializable;

public class Player implements Movable, Serializable {
    private int x, y, speed;
    private boolean up, down, left, right;
    private int targetX, targetY;

    // PUSTY KONSTRUKTOR do deserializacji JSON (Gson)
    public Player() {
    }

    public Player(int x, int y, int speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
    }

    public void setTarget(int x, int y) {
        this.targetX = x;
        this.targetY = y;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public void move(int width, int height) {
        if (up) y -= speed;
        if (down) y += speed;
        if (left) x -= speed;
        if (right) x += speed;

        x = Math.max(0, Math.min(width - 20, x));
        y = Math.max(0, Math.min(height - 20, y));
    }

    public void handleKeyPress(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_W -> up = pressed;
            case KeyEvent.VK_S -> down = pressed;
            case KeyEvent.VK_A -> left = pressed;
            case KeyEvent.VK_D -> right = pressed;
        }
    }

    public void draw(Graphics g) {
        g.fillRect(x, y, 20, 20);
    }

    @Override
    public int getX() { return x; }

    @Override
    public int getY() { return y; }

    @Override
    public void setX(int x) { this.x = x; }

    @Override
    public void setY(int y) { this.y = y; }
}
