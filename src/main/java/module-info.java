module ch.dreyeck.zettelkastenfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jakarta.xml.bind;
    requires jdk.jsobject;
    requires java.desktop;
    requires sodium;

    opens ch.dreyeck.zettelkastenfx to javafx.fxml;
    exports ch.dreyeck.zettelkastenfx;

    opens ch.dreyeck.zettelkasten.xml to jakarta.xml.bind;
    exports ch.dreyeck.zettelkasten.xml;
}
