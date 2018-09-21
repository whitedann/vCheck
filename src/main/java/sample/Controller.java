package sample;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import sample.elements.WellState;
import sample.excelFiles.OutputSheet;

import java.io.IOException;

import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.ORANGE;


public class Controller {


    @FXML
    private Pane plateGrid;

    @FXML
    private HBox bottomPane;
    @FXML
    private HBox bottomBPane;

    private Text upThresh = new Text("Upper Threshold: ");
    private Text lowThresh = new Text("Lower Threshold: ");
    private Text wellPos = new Text("Well Position:");
    private Text targetVol = new Text("Target Volume: ");
    private Circle focusImage = new Circle(0,0,0);

    private Circle[][] wells = new Circle[12][8];
    OutputSheet outputSheet = new OutputSheet();

    public Controller() throws IOException {
    }

    @FXML
    protected void initialize(){
        targetVol.setText("Target Volume: ");
        bottomPane.setPadding(new Insets(0, 21, 0, 8));
        bottomPane.setSpacing(100);
        bottomBPane.setPadding(new Insets(0, 0, 0, 0));
        bottomBPane.setSpacing(100);
        bottomPane.getChildren().add(wellPos);
        bottomBPane.getChildren().add(targetVol);
        bottomBPane.getChildren().add(lowThresh);
        bottomPane.getChildren().add(upThresh);

        for(int j = 0; j < wells[0].length; j++) {
            for(int i = 0; i < wells.length; i++) {
                Circle newCircle = new Circle(44 * i + 24, 44*j + 24, 22);
                newCircle.setStyle("-fx-fill: white");
                newCircle.setStroke(BLACK);
                int finalI = i;
                int finalJ = j;
                newCircle.setOnMouseClicked(e -> {
                    System.out.println(finalI + " " + finalJ);
                    setFocus(finalI, finalJ);
                });
                wells[i][j] = newCircle;
                plateGrid.getChildren().add(wells[i][j]);
            }
        }
        plateGrid.getChildren().add(focusImage);
    }

    private void setFocus(int row, int col) {
        this.targetVol.setText(String.format("Target Volume: %3.1f",outputSheet.getTargetVolume(row, col)));
        this.wellPos.setText(String.format("Well Position: " + outputSheet.getWellPosition(row, col)));
        this.upThresh.setText(String.format("Upper Threshold: ") + outputSheet.getUpperThreshold(row, col));
        this.lowThresh.setText(String.format("Lower Threshold: " + outputSheet.getLowerThreshold(row, col)));
        focusImage.setCenterY(44*(col)+24);
        focusImage.setCenterX(44*(row)+24);
        focusImage.setRadius(21);
        focusImage.setFill(ORANGE);
    }

    @FXML
    private void updateStatus() throws IOException {
        outputSheet.parseResult();
        WellState[][] states = outputSheet.getResultsArray();
        for(int j = 0; j < wells[0].length; j++){
            for(int i = 0; i < wells.length; i++){
                if(states[i][j].equals(WellState.PASS)) {
                    wells[i][j].setStyle("-fx-fill: green");
                }
                else if(states[i][j].equals(WellState.NODATA)){
                    wells[i][j].setStyle("-fx-fill: lightgrey");
                }
                else
                    wells[i][j].setStyle("-fx-fill: red");
            }
        }
    }

    @FXML
    private void loadSheet() throws IOException {
        outputSheet.prepareDocument();
    }

    @FXML
    private void setFocus(Node e){

    }




}
