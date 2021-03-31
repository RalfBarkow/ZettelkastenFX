module zettelkasten {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jakarta.xml.bind;
    requires jdk.jsobject;
    requires java.desktop;
    requires sodium;
    requires javafx.swing;
    requires afterburner.fx;

    opens ch.dreyeck.zettelkasten.fx to javafx.fxml, afterburner.fx;
    exports ch.dreyeck.zettelkasten.fx;

    opens ch.dreyeck.zettelkasten.xml to jakarta.xml.bind;
    exports ch.dreyeck.zettelkasten.xml;
}

