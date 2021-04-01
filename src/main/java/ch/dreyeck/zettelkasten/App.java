package ch.dreyeck.zettelkasten;

import ch.dreyeck.zettelkasten.list.ZettelkastenView;
import com.airhacks.afterburner.injection.Injector;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ZettelkastenView view = new ZettelkastenView();
        Scene scene = new Scene(view.getView());
        stage.setTitle("Zettelkasten");
        final String uri = getClass().getResource("App.css").toExternalForm();
        scene.getStylesheets().add(uri);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        Injector.forgetAll();
    }
}