package vacuumsim;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

 @Override
    public void start(Stage stage) throws IOException{

     FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/vacuumsim/view/MainView.fxml"));

     Scene scene = new Scene(fxmlLoader.load(), 1050, 780);

     stage.setTitle("Robot Vacuum Simulation");
     stage.setScene(scene);
     stage.show();
 }
  // JavaFX runtime components are missing hatası almamak için bir launchar sınfır kurup ordan çalıştıryoruz
    public static void main(String[] args) {
        launch(args);
    }
}
