module ch.dreyeck.zettelkastenfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml.bind;

    opens ch.dreyeck.zettelkastenfx to javafx.fxml, java.xml.bind;
    exports ch.dreyeck.zettelkastenfx;

}
