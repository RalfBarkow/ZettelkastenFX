package ch.dreyeck.zettelkastenfx;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;

public class FXBrowser extends JFrame {

    private static FXBrowser fxbrowser;
    private final JavaFXBrowser browser;
    private JPanel webViewPanel;
    private String url;

    private FXBrowser() {
        initComponents();
        setSize(1024, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setTitle("Browser");
        browser = new JavaFXBrowser();
        webViewPanel.add(browser.getComponent(), BorderLayout.CENTER);
        browser.load();
    }

    public static void showBrowser(String url) {
        if (fxbrowser == null) {
            fxbrowser = new FXBrowser();
        }
        if (fxbrowser.isVisible()) {
            return;
        }
        fxbrowser.url = url;
        fxbrowser.setVisible(true);
        fxbrowser.loadPanel();
    }

    public void loadPanel() {
        browser.setUrl(url);
    }

    private void initComponents() {
        webViewPanel = new javax.swing.JPanel();
        webViewPanel.setLayout(new java.awt.BorderLayout());
        getContentPane().add(webViewPanel, java.awt.BorderLayout.CENTER);
        pack();
    }
}

class JavaFXBrowser {

    JFXPanel fxPanel = new JFXPanel();
    WebEngine webEngine;
    WebView webView;

    public void load() {
        Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
            @Override
            public void run() {
                webView = new WebView();
                fxPanel.setScene(new Scene(webView));
                webEngine = webView.getEngine();
            }
        });
    }

    public Component getComponent() {
        return fxPanel;
    }

    public void setUrl(final String url) {
        Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
            @Override
            public void run() {
                webEngine.load(url);
            }
        });
    }
}