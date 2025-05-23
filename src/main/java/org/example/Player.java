package org.example;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.Serializable;

public class Player implements Movable, Serializable {
    private static final long serialVersionUID = 1L;

    private int x, y, speed;
    private int targetX, targetY;
    private boolean up, down, left, right;

    public Player(int x, int y, int speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.targetX = x;
        this.targetY = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setTargetX(int x) { this.targetX = x; }
    public void setTargetY(int y) { this.targetY = y; }

    public void setTarget(int x, int y) {
        this.targetX = x;
        this.targetY = y;
    }

    public void handleKeyPress(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_W -> up = pressed;
            case KeyEvent.VK_S -> down = pressed;
            case KeyEvent.VK_A -> left = pressed;
            case KeyEvent.VK_D -> right = pressed;
        }
    }

    public void move(int width, int height) {
        if (up) y -= speed;
        if (down) y += speed;
        if (left) x -= speed;
        if (right) x += speed;
        x = Math.max(0, Math.min(x, width - 20));
        y = Math.max(0, Math.min(y, height - 20));
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, 20, 20);
    }

    @Override
    public String toString() {
        return "Player{" + "x=" + x + ", y=" + y + '}';
    }
}
