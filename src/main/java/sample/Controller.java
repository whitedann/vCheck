package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

public class Controller {

    @FXML
    private Pane plateGrid;

    public Controller(){
    }

    @FXML
    protected void initializeGrid(){
        Circle newCircle = new Circle(200,200,10);
        plateGrid.getChildren().add(new Button("click"));
    }

}
