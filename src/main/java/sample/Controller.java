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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static javafx.scene.paint.Color.BLACK;


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
    private Text wellStatus = new Text("Status: ");
    private Text sessionText = new Text("Current Session: \nNone");
    private Circle focusImage = new Circle(0,0,0);
    private Circle[][] wells = new Circle[12][8];
    private static Map<Integer, Character> wellRows = new HashMap<>();

    /** Data **/
    private OutputSheet outputSheet;
    private String savePath;
    private String importPath = "W:\\\\Manufacturing\\VolumeCheck\\Results\\";

    private boolean sessionActive = false;
    private double sessionStartTime = 0;
    private String currentUser, currentPassword;
    private int currentState, primerMixFlag;

    /** Session time out interval **/
    private static final int timeOutTimeInMillis = 10*10000;

    @FXML
    private void initialize(){
        primerMixFlag = 1;
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
        wellRows.put(0, 'A');
        wellRows.put(1, 'B');
        wellRows.put(2, 'C');
        wellRows.put(3, 'D');
        wellRows.put(4, 'E');
        wellRows.put(5, 'F');
        wellRows.put(6, 'G');
        wellRows.put(7, 'H');



        /** Adds plate/well graphic and function **/

        /**Well Labels **/
        for(int i = 0; i < wells[0].length; i++){
            Text text = new Text();
            text.setStyle("-fx-translate-y:" + (i*44 + 30) + ";" +
                    "-fx-translate-x: -15");
            text.setText(String.valueOf(wellRows.get(i)));
            plateGrid.getChildren().add(text);
        }
        for(int i = 0; i < wells.length; i++){
            Text text = new Text();
            text.setStyle("-fx-translate-x:" + (i*44 + 17) + ";" +
                    "-fx-translate-y: -5");
            text.setText(String.valueOf(i+1));
            plateGrid.getChildren().add(text);
        }

        /**Well graphics **/
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

        /** Add focus **/
        plateGrid.getChildren().add(focusImage);
    }

    /** Prompts users for login credentials **/
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

    /** Validates log in by checking to see if a template file was successfully downloaded **/
    /** @return int (0 success, 1 fail) **/
    private int validateLogin() {
        try {
            return outputSheet.downloadTemplate(currentUser, currentPassword);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /** Sets the clicked well as the focus, and updates shown data **/
    /** @param row
     * @param col index of well
     */
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
            this.wellPos.setText(String.format("Well Position: %s", outputSheet.getWellPosition(row, col)));
            this.upThresh.setText(String.format("Upper Threshold: %3.2f", outputSheet.getUpperThreshold(row, col)));
            this.lowThresh.setText(String.format("Lower Threshold: %3.2f", outputSheet.getLowerThreshold(row, col)));
            this.measuredVol.setText(String.format("Measured Volume: %3.2f", outputSheet.getMeasuredVol(row, col)));
            this.wellStatus.setText(String.format("Status: %s", outputSheet.getResultsArray()[row][col].toString()));
        }
        focusImage.setCenterY(44*(col)+24);
        focusImage.setCenterX(44*(row)+24);
        focusImage.setRadius(20);
        focusImage.setStyle("-fx-fill: darkorange");
    }

    /** Updates the visual status of the wells, after the data has been imported **/
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

    /** Called after the target data has been imported, to show the user that
     * it is awaiting an import of measured data.
     * @throws IOException
     */
    @FXML
    private void loadPlateButton() throws IOException {
        if((System.currentTimeMillis() - sessionStartTime) > timeOutTimeInMillis){
            sessionActive = false;
            sessionText.setText("Current Session: \nNone");
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
                if(customerField.getText().contains("invitae") || this.customerField.getText().contains("Invitae")){
                    primerMixFlag = 2;
                }
                outputSheet.executePhaseOne(currentUser, String.valueOf(input), primerMixFlag);
                setStatusOfWells();
                currentState = 1;
                importButton.setStyle("");
            }
        }
    }

    /** Auto-imports the most recently saved measured data by calling
     *  findMostRecentMeasuredData()
     * @throws IOException
     */
    @FXML
    private void autoImportButton() throws IOException {
        if(currentState == 1 || currentState == 2) {
            File toImport = findMostRecentMeasuredData(importPath);
            if(outputSheet.executePhaseTwo(toImport) == 0){
                currentState = 2;
                acceptButton.setStyle("");
                setStatusOfWells();
            }
        }
    }

    /** Finds the most recently saved measured data file and verifies that it is okay to use
     * @param dir import directory to search in
     * @return File to import
     * @throws IOException
     */
    private File findMostRecentMeasuredData(String dir) throws IOException {
        Path directory = Paths.get(dir);

        Optional<Path> lastFilePath = Files.list(directory)
                .filter(f -> !Files.isDirectory(f))
                .filter(f -> f.toString().endsWith(".CSV"))
                .max(Comparator.comparingLong(f -> f.toFile().lastModified()));

        if(lastFilePath.isPresent()) {
            System.out.println("Using File: " + lastFilePath.toString());
            File file = new File(String.valueOf(lastFilePath.get()));
            if(System.currentTimeMillis() - file.lastModified() > 1000*180){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Warning! Most recent scan data is from more than 3 minutes ago! \n" +
                        "Are you sure you saved the most recent scan?");
                alert.setTitle("Old Data Warning");
                alert.showAndWait();
            }
            return file;
        }
        else
            return null;
    }

    /** Accepts the output and save its to output directory
     * @throws IOException
     */
    @FXML
    private void acceptAndSaveButton() throws IOException {
        if(currentState == 2) {
            outputSheet.executePhaseThree(barcodeField.getText(), customerField.getText());
            acceptButton.setStyle(
                    "-fx-background-color: lightgrey; " +
                            "-fx-border-color: darkgrey; " +
                            "-fx-text-fill: darkgrey");
            importButton.setStyle(
                    "-fx-background-color: lightgrey; " +
                            "-fx-border-color: darkgrey; " +
                            "-fx-text-fill: darkgrey");
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

    /** resets the GUI and the OutputSheet object
     * @throws IOException
     */
    @FXML
    public void resetAll() throws IOException {
        primerMixFlag = 1;
        outputSheet = new OutputSheet();
        outputSheet.initializeWellStates();
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
        barcodeField.setText("");
        customerField.setText("");
    }

    /** Imports measured data from specified directory
     * @throws IOException
     */
    @FXML
    public void manuallyImportData() throws IOException {
        if(currentState == 1 || currentState == 2) {
            FileChooser chooser = new FileChooser();
            File toImport = chooser.showOpenDialog(plateGrid.getScene().getWindow());
            if (toImport == null) {
                return;
            }
            else{
                if(outputSheet.executePhaseTwo(toImport) == 0) {
                    acceptButton.setStyle("");
                    setStatusOfWells();
                    currentState = 2;
                }
            }
        }
    }

    @FXML
    public void close(){
        Stage stage = (Stage) plateGrid.getScene().getWindow();
        stage.close();
    }

    /** Destroys current session **/
    @FXML
    public void logout() {
       sessionActive = false;
       sessionText.setText("Current Session: \nNone");
    }
}
