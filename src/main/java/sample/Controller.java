package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import sample.elements.WellState;
import sample.excelFiles.OutputSheet;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static javafx.scene.paint.Color.BLACK;

/**    TODO:
/*  Add/Remove Menu functions
    Add difference (target-measured) to focus
    Add paste user + barcode into final sheet
    Values in bottom pane do not format nicely. trim values to 2 decimal places.
 **/

public class Controller {

    /** FXML References **/
    @FXML
    private Pane plateGrid;

    @FXML
    private VBox RightColumn;

    @FXML
    private VBox LeftColumn;

    @FXML
    private HBox userBox;

    @FXML
    private TextField barcodeField;

    @FXML
    private TextField customerField;

    @FXML
    private Button acceptButton;

    @FXML
    private Button importButton;

    /** GUI Objects **/
    private Text upThresh = new Text("Upper Threshold: ");
    private Text lowThresh = new Text("Lower Threshold: ");
    private Text wellPos = new Text("Well Position:");
    private Text targetVol = new Text("Target Volume: ");
    private Text measuredVol = new Text("Measured Volume: ");
    private Text wellStatus = new Text("Well Status: ");
    private Text sessionText = new Text("Current Session: \nNone");
    private Circle focusImage = new Circle(0,0,0);
    private Circle[][] wells = new Circle[12][8];

    /** Data **/
    private OutputSheet outputSheet;
    private String savePath;
    private String importPath;

    private boolean sessionActive = false;
    private double sessionStartTime = 0;
    private String currentUser, currentPassword;
    private int currentState;

    private static final int timeOutTimeInMillis = 10*1000;

    public Controller() {
    }

    @FXML
    protected void initialize(){
        currentState = 0;
        sessionActive = false;
        sessionStartTime = 0;
        RightColumn.setPadding(new Insets(3, 0, 0, 300));
        RightColumn.setSpacing(8);
        LeftColumn.setPadding(new Insets(3, 0, 0, 5));
        LeftColumn.setSpacing(8);
        RightColumn.getChildren().add(targetVol);
        RightColumn.getChildren().add(upThresh);
        RightColumn.getChildren().add(lowThresh);
        LeftColumn.getChildren().add(wellPos);
        LeftColumn.getChildren().add(wellStatus);
        LeftColumn.getChildren().add(measuredVol);
        userBox.getChildren().add(sessionText);
        acceptButton.setStyle(
                        "-fx-background-color: lightgrey; " +
                        "-fx-border-color: darkgrey; " +
                        "-fx-text-fill: darkgrey");
        importButton.setStyle(
                        "-fx-background-color: lightgrey;" +
                        "-fx-border-color: darkgrey;" +
                        "-fx-text-fill: darkgrey");



        /** Adds plate/well graphic and function **/
        for(int j = 0; j < wells[0].length; j++) {
            for(int i = 0; i < wells.length; i++) {
                Circle newCircle = new Circle(44 * i + 24, 44*j + 24, 21);
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

    @FXML
    private void queryLogin(){
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Authenticate");
        dialog.setHeaderText("Login to Master Control");
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(() -> username.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(usernamePassword -> {
            currentUser = usernamePassword.getKey();
            currentPassword = usernamePassword.getValue();
        });
    }

    private int validateLogin() {
        try {
            return outputSheet.downloadTemplate(currentUser, currentPassword);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private void setFocus(int row, int col) {
        if(this.outputSheet == null){
            this.targetVol.setText(String.format("Target Volume:"));
            this.wellPos.setText(String.format("Well Position: "));
            this.upThresh.setText(String.format("Upper Threshold: "));
            this.lowThresh.setText(String.format("Lower Threshold: "));
            this.measuredVol.setText(String.format("Measured Volume: "));
            this.wellStatus.setText(String.format("Status: "));
        }
        else {
            this.targetVol.setText(String.format("Target Volume: %3.2f", outputSheet.getTargetVolume(row, col)));
            this.wellPos.setText(String.format("Well Position: ", outputSheet.getWellPosition(row, col)));
            this.upThresh.setText(String.format("Upper Threshold: %3.2f", outputSheet.getUpperThreshold(row, col)));
            this.lowThresh.setText(String.format("Lower Threshold: %3.2f", outputSheet.getLowerThreshold(row, col)));
            this.measuredVol.setText(String.format("Measured Volume: %3.2f", outputSheet.getMeasuredVol(row, col)));
            this.wellStatus.setText(String.format("Status: ", outputSheet.getResultsArray()[row][col].toString()));
        }
        focusImage.setCenterY(44*(col)+24);
        focusImage.setCenterX(44*(row)+24);
        focusImage.setRadius(20);
        focusImage.setStyle("-fx-fill: darkorange");
    }

    @FXML
    private void setStatusOfWells() throws IOException {
        if(outputSheet.getResultsArray() == null){
            for(int j = 0; j < wells[0].length; j++){
                for(int i = 0; i < wells.length; i++){
                    wells[i][j].setStyle("-fx-fill: white");
                }
            }
            return;
        }
        WellState[][] states = outputSheet.getResultsArray();
        for(int j = 0; j < wells[0].length; j++){
            for(int i = 0; i < wells.length; i++){
                if(states[i][j] == null){
                    wells[i][j].setStyle("-fx-fill: white");
                }
                else if(states[i][j].equals(WellState.PASS)) {
                    wells[i][j].setStyle("-fx-fill: green");
                }
                else if(states[i][j].equals(WellState.NODATA)){
                    wells[i][j].setStyle("-fx-fill: lightsteelblue");
                }
                else if(states[i][j].equals(WellState.FAIL))
                    wells[i][j].setStyle("-fx-fill: red");
                else if(states[i][j].equals(WellState.EMPTY)) {
                    wells[i][j].setStyle("-fx-fill: white");
                }
            }
        }
    }

    @FXML
    private void phaseOne() throws IOException {
        if((System.currentTimeMillis() - sessionStartTime) > timeOutTimeInMillis){
            sessionActive = false;
            sessionText.setText("Current Session: \nNo one!");
        }
        boolean numeric = true;
        int input = 0;
        try {
            input = Integer.parseInt(barcodeField.getText());
        } catch (NumberFormatException e){
            numeric = false;
        }
        if(numeric) {
            if(!sessionActive) {
                queryLogin();
            }
            outputSheet = new OutputSheet();
            if (validateLogin() == 0) {
                sessionText.setText("Current Session: \n" +  currentUser);
                sessionActive = true;
                sessionStartTime = System.currentTimeMillis();
                outputSheet.executePhaseOne(currentUser, String.valueOf(input));
                setStatusOfWells();
                currentState = 1;
                importButton.setStyle("");
            }
            outputSheet.updateWellStates();
        }
    }

    @FXML
    private void phaseTwo() throws IOException {
        if(currentState == 1) {
            if(outputSheet.executePhaseTwo() == 0){
                currentState = 2;
                acceptButton.setStyle("");
            }
            setStatusOfWells();
        }
    }

    @FXML
    private void phaseThree() throws IOException {
        if(currentState == 2) {
            outputSheet.executePhaseThree(barcodeField.getText(), customerField.getText());
            setStatusOfWells();
            acceptButton.setStyle(
                    "-fx-background-color: lightgrey; " +
                            "-fx-border-color: darkgrey; " +
                            "-fx-text-fill: darkgrey");
            importButton.setStyle(
                    "-fx-background-color: lightgrey; " +
                            "-fx-border-color: darkgrey; " +
                            "-fx-text-fill: darkgrey");
            currentState = 0;
            resetAll();
        }
    }

    @FXML
    public void setImportPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        File newPath = chooser.showDialog(plateGrid.getScene().getWindow());
        if(newPath == null) {
            return;
        }
        else{
            importPath = newPath.getAbsolutePath() + "data.xlsx";
        }
        outputSheet.setImportPath(importPath);
    }

    @FXML
    public void setSavePath() {
        DirectoryChooser chooser = new DirectoryChooser();
        File newPath = chooser.showDialog(plateGrid.getScene().getWindow());
        if(newPath == null){
            return;
        }
        else{
            savePath = newPath.getAbsolutePath() + "final.xlsx";
        }
        outputSheet.setSavePath(savePath);
    }

    @FXML
    public void resetAll() throws IOException {
        outputSheet = new OutputSheet();
        outputSheet.updateWellStates();
        setStatusOfWells();
        currentState = 0;
        importButton.setStyle(
                "-fx-background-color: lightgrey; " +
                        "-fx-border-color: darkgrey; " +
                        "-fx-text-fill: darkgrey");
        acceptButton.setStyle(
                "-fx-background-color: lightgrey; " +
                        "-fx-border-color: darkgrey; " +
                        "-fx-text-fill: darkgrey");
    }

    @FXML
    public void manuallyImportData() throws IOException {
        if(currentState == 1) {
            FileChooser chooser = new FileChooser();
            File toImport = chooser.showOpenDialog(plateGrid.getScene().getWindow());
            if (toImport == null) {
                return;
            }
            else{
                outputSheet.executePhaseTwo(toImport);
                setStatusOfWells();
                currentState = 2;
            }
        }
    }

    @FXML
    public void close(){
        Stage stage = (Stage) plateGrid.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void logout() {
       sessionActive = false;
       sessionText.setText("Current Session: \nNone");
    }
}
