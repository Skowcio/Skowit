package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class GameClient extends JPanel implements Runnable {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private Player localPlayer;
    private Player remotePlayer;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public GameClient() {
        setPreferredSize(new Dimension(900, 900));
        setBackground(Color.WHITE);

        localPlayer = new Player(450, 450, 5);
        remotePlayer = new Player(300, 300, 5);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                localPlayer.handleKeyPress(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                localPlayer.handleKeyPress(e.getKeyCode(), false);
            }
        });

        setFocusable(true);
        connectToServer();
        new Thread(this).start();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        Object data = in.readObject();
                        if (data instanceof Player) {
                            Player receivedPlayer = (Player) data;
                            remotePlayer.setX(receivedPlayer.getX());
                            remotePlayer.setY(receivedPlayer.getY());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Rozłączono z serwerem.");
                }
            }).start();
        } catch (IOException e) {
            System.out.println("Nie można połączyć z serwerem.");
        }
    }

    @Override
    public void run() {
        while (true) {
            localPlayer.move(getWidth(), getHeight());
            sendPlayerData();
            repaint();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                // Ignoruj
            }
        }
    }

    private void sendPlayerData() {
        try {
            out.writeObject(localPlayer);
        } catch (IOException e) {
            System.out.println("Błąd podczas wysyłania danych.");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLUE);
        localPlayer.draw(g);
        g.setColor(Color.GREEN);
        remotePlayer.draw(g);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("lan game chyba?");
        GameClient game = new GameClient();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
