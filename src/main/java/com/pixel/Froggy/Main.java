package com.pixel.Froggy;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            FileExplorer frame = new FileExplorer();
            java.net.URL iconURL = Main.class.getResource("/assets/Froggy/icon.png");
            if (iconURL != null) {
                ImageIcon img = new ImageIcon(iconURL);
                frame.setIconImage(img.getImage());
            }
            frame.setVisible(true);
        });
    }
}