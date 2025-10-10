package zk.ui.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {
  @Override public void start(Stage stage) {
    var root = new BorderPane(new Label("ZettelkastenFX — clean slate OK"));
    stage.setScene(new Scene(root, 900, 600));
    stage.setTitle("ZettelkastenFX");
    stage.show();
  }
  public static void main(String[] args){ launch(args); }
}
