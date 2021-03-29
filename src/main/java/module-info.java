module ch.dreyeck.zettelkastenfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jakarta.xml.bind;
    requires jdk.jsobject;
    requires java.desktop;
    requires sodium;
    requires javafx.swing;

    opens ch.dreyeck.zettelkasten.fx to javafx.fxml;
    exports ch.dreyeck.zettelkasten.fx;

    opens ch.dreyeck.zettelkasten.xml to jakarta.xml.bind;
    exports ch.dreyeck.zettelkasten.xml;
}
