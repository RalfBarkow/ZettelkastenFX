package ch.dreyeck.zettelkasten;

import cern.extjfx.fxml.FxmlView;
import ch.dreyeck.zettelkasten.list.ZettelkastenController;
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
        FxmlView mainView = new FxmlView(ZettelkastenController.class);
        Scene scene = new Scene(mainView.getRootNode());
        stage.setTitle("Zettelkasten");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        //method is empty
    }
}