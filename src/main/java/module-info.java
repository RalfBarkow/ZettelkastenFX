module ch.dreyeck.zettelkastenfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml.bind;

    opens ch.dreyeck.zettelkastenfx to javafx.fxml;
    exports ch.dreyeck.zettelkastenfx;

    opens ch.dreyeck.zettelkasten.xml to java.xml.bind;
    exports ch.dreyeck.zettelkasten.xml;
}
