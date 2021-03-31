//DEPS org.openjfx:javafx-controls:11.0.2:${os.detected.jfxname}
//DEPS org.openjfx:javafx-graphics:11.0.2:${os.detected.jfxname}
//DEPS org.openjfx:javafx-fxml:11.0.2:${os.detected.jfxname}

package ch.dreyeck.zettelkasten.fx;

import ch.dreyeck.zettelkasten.fx.view.ZettelkastenView;
import com.airhacks.afterburner.injection.Injector;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        ZettelkastenView view = new ZettelkastenView();
        Scene scene = new Scene(view.getView());
        stage.setTitle("Zettelkasten");
        final String uri = getClass().getResource("App.css").toExternalForm();
        scene.getStylesheets().add(uri);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        Injector.forgetAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}