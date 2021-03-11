package ch.dreyeck.zettelkastenfx;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FXBrowserExample extends JFrame {

    public FXBrowserExample() {
        JButton button = new JButton("DMX");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FXBrowser.showBrowser("https://dmx.berlin/");
            }
        });
        add(button);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new FXBrowserExample().setVisible(true);
            }
        });
    }
}