//DEPS org.openjfx:javafx-controls:11.0.2:${os.detected.jfxname}
//DEPS org.openjfx:javafx-graphics:11.0.2:${os.detected.jfxname}
//DEPS org.openjfx:javafx-web:11.0.2:${os.detected.jfxname}

package ch.dreyeck.zettelkastenfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class WebViewExample extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX WebView Example");

        WebView webView = new WebView();

        webView.getEngine().load("http://192.168.178.36:8443/systems.dmx.webclient/");

        VBox vBox = new VBox(webView);
        Scene scene = new Scene(vBox, 960, 600);

        primaryStage.setScene(scene);
        primaryStage.show();

    }
}
