package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception{

        /** .getClassLoader().getResource() is required instead of .getResource()
         * for some reason caused by Maven. I also moved the .fxml
         * file to the resources directory, but I'm not sure that is needed. **/
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("fxml/sample.fxml"));
        primaryStage.setResizable(false);
        primaryStage.setTitle("vCheck");
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
