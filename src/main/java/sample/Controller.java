package sample;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import sample.elements.WellState;
import sample.excelFiles.OutputSheet;

import java.io.IOException;

import static javafx.scene.paint.Color.BLACK;


public class Controller {


    @FXML
    private Pane plateGrid;

    private Circle[][] wells = new Circle[12][8];
    OutputSheet data = new OutputSheet("/assets/template.xlsx");

    public Controller() throws IOException {

    }


    @FXML
    protected void initialize(){
        for(int j = 0; j < wells[0].length; j++) {
            for(int i = 0; i < wells.length; i++) {
                Circle newCircle = new Circle(44 * i + 24, 44*j + 24, 22);
                newCircle.setStyle("-fx-fill: dodgerblue");
                newCircle.setStroke(BLACK);
                int finalI = i + 1;
                int finalJ = j + 1;
                newCircle.setOnMouseClicked(e -> {
                    System.out.println(finalI + " " + finalJ);
                });
                wells[i][j] = newCircle;
                plateGrid.getChildren().add(wells[i][j]);
            }
        }
    }

    @FXML
    private void updateStatus(){
        WellState[][] states = data.getResultsArray();
        for(int j = 0; j < wells[0].length; j++){
            for(int i = 0; i < wells.length; i++){
                if(states[i][j].equals(WellState.PASS)) {
                    wells[i][j].setStyle("-fx-fill: green");
                }
                else
                    wells[i][j].setStyle("-fx-fill: red");
            }
        }
    }

    @FXML
    private void setFocus(Node e){

    }




}
