module ch.dreyeck.zettelkastenfx {
    requires javafx.controls;
    requires javafx.fxml;

    opens ch.dreyeck.zettelkastenfx to javafx.fxml;
    exports ch.dreyeck.zettelkastenfx;
}