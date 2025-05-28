package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.List;

public class SimpleMovementGame extends JPanel implements Runnable {

    private Player player1;
    private Player2 player2;
    private ArrayList<Enemy> enemies;
    private ArrayList<Bullet> bullets;
    private Random random;
    private boolean shooting;



    public SimpleMovementGame() {
        setPreferredSize(new Dimension(900, 900));
        setBackground(Color.WHITE);

        player1 = new Player(450, 450, 5);
        player2 = new Player2(300, 300, 5);
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        random = new Random();
        shooting = false;
        spawnEnemies(5);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                player1.handleKeyPress(e.getKeyCode(), true);
                player2.handleKeyPress(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player1.handleKeyPress(e.getKeyCode(), false);
                player2.handleKeyPress(e.getKeyCode(), false);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    shooting = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    shooting = false;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                player1.setTarget(e.getX(), e.getY());
                player2.setTarget(e.getX(), e.getY());
            }
        });

        setFocusable(true);
        new Thread(this).start();
    }

    private void spawnEnemies(int count) {
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(880);
            int y = random.nextInt(880);
            enemies.add(new Enemy(x, y, 2));
        }
    }

    @Override
    public void run() {
        while (true) {
            player1.move(getWidth(), getHeight());
            player2.move(getWidth(), getHeight());

            if (shooting) {
                bullets.add(new Bullet(player1.getX(), player1.getY(), player1.getTargetX(), player1.getTargetY()));
                bullets.add(new Bullet(player2.getX(), player2.getY(), player2.getTargetX(), player2.getTargetY()));
            }

            Iterator<Bullet> bulletIterator = bullets.iterator();
            while (bulletIterator.hasNext()) {
                Bullet bullet = bulletIterator.next();
                bullet.move();

                boolean hit = false;
                Iterator<Enemy> enemyIterator = enemies.iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    if (bullet.isColliding(enemy)) {
                        enemyIterator.remove();
                        hit = true;
                        break;
                    }
                }

                if (hit || bullet.isOutOfBounds(getWidth(), getHeight())) {
                    bulletIterator.remove();
                }
            }

            // ✅ Nowe podejście - wróg wybiera najbliższego gracza
            List<Movable> players = new ArrayList<>();
            players.add(player1);
            players.add(player2);

            for (Enemy enemy : enemies) {
                enemy.moveTowardsClosest(players);
            }

            repaint();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        player1.draw(g);
        player2.draw(g);
        for (Enemy enemy : enemies) {
            enemy.draw(g);
        }
        for (Bullet bullet : bullets) {
            bullet.draw(g);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple Movement Game - Two Players");
        SimpleMovementGame game = new SimpleMovementGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}