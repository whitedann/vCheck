package sample.excelFiles;

import javafx.scene.control.Alert;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.NotOLE2FileException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sample.elements.WellState;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputSheet {

    private static final String DEFAULT_IMPORT_PATH = "/Users/dwhite/vCheck1.1/src/main/resources/assets/";
    private static final String DEFAULT_SSRSREPORT_PATH = "/Users/dwhite/vCheck1.1/src/main/resources/assets/";
    private static final String DEFAULT_TEMPLATE_PATH = "/Users/dwhite/vCheck1.1/src/main/resources/assets/";
    private static final String DEFAULT_SAVE_PATH = "/Users/dwhite/vCheck1.1";

    /**
    private static final String DEFAULT_IMPORT_PATH = "W:\\\\Manufacturing\\VolumeCheck\\Results\\";
    private static final String DEFAULT_SSRSREPORT_PATH = "W:\\\\Employees\\Danny\\dev\\";
    private static final String DEFAULT_TEMPLATE_PATH = "W:\\\\Employees\\Danny\\dev\\";
    private static final String DEFAULT_SAVE_PATH = "W:\\\\Manufacturing\\VolumeCheck\\Final Excel Results\\";
     **/

    private String importPath, ssrsReportPath, templatePath, savePath;
    private static Map wellMappings = new HashMap<Character, Integer>();

    private Workbook template, plateVolumeInfo;
    private Sheet topTemplatePage, dataTemplatePage, plateData;
    private double[][] targetVolumes;
    private double[][] highEnds;
    private double[][] lowEnds;
    private double[][] measuredData;
    private String[][] wellPositions;
    private WellState[][] states;

    public OutputSheet(){
        this.importPath = DEFAULT_IMPORT_PATH;
        this.ssrsReportPath = DEFAULT_SSRSREPORT_PATH;
        this.templatePath = DEFAULT_TEMPLATE_PATH;
        this.savePath = DEFAULT_SAVE_PATH;
        wellMappings.put('A', 1);
        wellMappings.put('B', 2);
        wellMappings.put('C', 3);
        wellMappings.put('D', 4);
        wellMappings.put('E', 5);
        wellMappings.put('F', 6);
        wellMappings.put('G', 7);
        wellMappings.put('H', 8);

        states = new WellState[12][8];
        targetVolumes = new double[12][8];
        highEnds = new double[12][8];
        lowEnds = new double[12][8];
        wellPositions = new String[12][8];
        measuredData = new double[12][8];
        states = new WellState[12][8];
    }

    public int downloadTemplate(String user, String pass) throws IOException {
        String url = "https://idtdna.mastercontrol.com/mc/login/index.cfm?action=login";

        BasicCookieStore store = new BasicCookieStore();
        CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(store).build();

        try {
            HttpUriRequest login = RequestBuilder.post()
                    .setUri(url)
                    .addParameter("username", user)
                    .addParameter("username2", "")
                    .addParameter("initialRequest", "")
                    .addParameter("password", pass)
                    .build();
            CloseableHttpResponse response2 = client.execute(login);
            try{
                HttpEntity entity = response2.getEntity();
                System.out.println("Login form get: " + response2.getStatusLine());
                EntityUtils.consume(entity);
            } finally {
                response2.close();
            }

            String downloadLink = "https://idtdna.mastercontrol.com/mc/Main/MASTERControl/Organizer/view_file.cfm?id=XBCYSHLULRA4XBIBB7";
            HttpGet get2 = new HttpGet(downloadLink);
            CloseableHttpResponse response3 = client.execute(get2);

            try{
                HttpEntity entity = response3.getEntity();
                System.out.println("Status of MC Login: " + response3.getStatusLine());
                BufferedInputStream bis = new BufferedInputStream(entity.getContent());
                if(bis.available() == 1){
                    System.out.println("Could not connect to Master Control");
                    bis.close();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Could not reach Master Control");
                    alert.setContentText("Master Control could not be reached \nVerify your login info & verify master control is online (idtdna.mastercontrol.com)");
                    alert.showAndWait();
                    return 1;
                }
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(templatePath + "template.xlsx")));
                int inByte;
                while((inByte = bis.read()) != -1)
                    bos.write(inByte);
                EntityUtils.consume(entity);
                bos.close();
                bis.close();
            }
            finally {
                response3.close();
            }
        }
        catch (UnknownHostException e){
            System.out.println("Could not connect to Master Control");
            return 1;
        }
        finally {
            client.close();
        }
        System.out.println("No errors in downloading vCheck template");
        return 0;
    }

    private int initializeTemplateFile(String user, String barcode) throws IOException {
        File inputFile = new File(templatePath + "template.xlsx");
        try {
            template = new XSSFWorkbook(new FileInputStream(inputFile));
        } catch (FileNotFoundException e){
            System.out.println("Could not read template file");
            return 1;
        }
        System.out.println("Template Successfully Loaded");
        dataTemplatePage = template.getSheetAt(1);
        topTemplatePage = template.getSheetAt(0);
        topTemplatePage.getRow(0).getCell(1).setCellValue(barcode);
        topTemplatePage.getRow(0).getCell(8).setCellValue(user);
        return 0;
    }

    private int downloadSSRSReport(String barcode) throws IOException {
        /**
        try {
            URL link = new URL(
                    "http",
                    "ssrsreports.idtdna.com",
                    80,
                    "/REPORTServer/Pages/ReportViewer.aspx?%2fManufacturing%2fSan+Diego%2fPlate+Volume+Information+by+Barcode+ID&rs:Command=Render&rs:Format=Excel&BarcodeID=" + barcode);
            BufferedInputStream in = new BufferedInputStream(link.openStream());
            Files.copy(in, Paths.get(ssrsReportPath + "plateVol2.xls"), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (UnknownHostException | ConnectException e){
            System.out.println("Failed to download SSRS report for barcde: " + barcode);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Failed to download volume info from SSRS \nCheck the barcode maybe or something");
            alert.showAndWait();
            return 1;
        }
         **/

        /**add check here to delete output file if it is below certain size please and thank you
         * Also should probably have the function return 1 if it does ... **/
        try {
            plateVolumeInfo = new XSSFWorkbook(((new FileInputStream(new File(ssrsReportPath + "plateVol2.xlsx")))));
            plateData = plateVolumeInfo.getSheetAt(0);
        } catch (NotOLE2FileException e) {
            System.out.println("Failed to download SSRS report OR File type is .xlsx instead of .xls");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Something went wrong in downloading SSRS report.\n Either bad barcode or bad connection.");
            alert.showAndWait();
            return 1;
        }
        System.out.println("SSRS report download successful");
        return 0;
    }

    private void mergeTemplateWithSSRSReport(){
        dataTemplatePage.setForceFormulaRecalculation(true);
        for(int i = 2; i < plateData.getPhysicalNumberOfRows(); i++){
            if(i >= 99){
                dataTemplatePage.createRow(i-2);
            }
            for(int j = 1; j <= 4; j++) {
                /** Creates cells in template if they do not exist **/
                if(i >= 99){
                    dataTemplatePage.getRow(i-2).createCell(0);
                    dataTemplatePage.getRow(i-2).createCell(j);
                }
                dataTemplatePage.getRow(i-2).getCell(0).setCellValue(plateData.getRow(i).getCell(0).getStringCellValue());
                dataTemplatePage.getRow(i-2).getCell(j).setCellValue(plateData.getRow(i).getCell(j).getNumericCellValue());
            }
            /** I dont know why, but we have to make a new column of cells to paste into the last column.
             * getCell(5) returns null otherwise... **/
            dataTemplatePage.getRow(i-2).createCell(5);
            dataTemplatePage.getRow(i-2).getCell(5).setCellValue(plateData.getRow(i).getCell(5).getNumericCellValue());
        }
        for(int i = 0; i < plateData.getPhysicalNumberOfRows() - 2; i++) {
            int col = Integer.parseInt(plateData.getRow(i + 2).getCell(0).getStringCellValue().substring(1)) - 1;
            int row = (int) wellMappings.get(plateData.getRow(i + 2).getCell(0).getStringCellValue().charAt(0)) - 1;
            targetVolumes[col][row] = dataTemplatePage.getRow(i).getCell(3).getNumericCellValue();
            highEnds[col][row] = targetVolumes[col][row] * 1.05 + 10;
            lowEnds[col][row] = targetVolumes[col][row] * 0.95 - 10;
            wellPositions[col][row] = plateData.getRow(i + 2).getCell(0).getStringCellValue();
        }
    }

    private int loadMeasuredDataAndMerge() throws IOException {
        Reader reader;
        try {
            reader = Files.newBufferedReader(Paths.get(importPath));
        }catch (NoSuchFileException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Could not find specified data file");
            alert.setTitle("Invalid file");
            alert.showAndWait();
            System.out.println("Could not find that import file");
            return 1;
        }
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
        List<Double> tempValList = new ArrayList<>();
        for(CSVRecord csvRecord : csvParser){
            if(csvRecord.getRecordNumber() > 4) {
                double val = Double.parseDouble(csvRecord.get(4));
                tempValList.add(val);
            }
        }
        for(int i = 0; i < tempValList.size(); i++){
            int row = i / 12;
            int col = i % 12;
            double finalValue = tempValList.get(i);
            if(Math.abs(finalValue) < 50)
                finalValue = 0;
            measuredData[col][row] = finalValue;
            topTemplatePage.getRow(i+3).getCell(2).setCellValue(finalValue);
        }
        System.out.println("Successfully loaded data from vCheck instrument");
        return 0;
    }

    public void initializeWellStates(){
        for(int i = 0; i < 96; i++){
            int col = i % 12;
            int row = i / 12;
            if(targetVolumes[col][row] > 30)
                states[col][row] = WellState.NODATA;
            else
                states[col][row] = WellState.EMPTY;
        }
    }

    public void updateWellStates(){
        for(int i = 0; i < 96; i++){
            int col = i % 12;
            int row = i / 12;
            if(measuredData[col][row] <= highEnds[col][row] &&
                    measuredData[col][row] >= lowEnds[col][row] &&
                    measuredData[col][row] > 30){
                states[col][row] = WellState.PASS;
            }
            else if((measuredData[col][row] > highEnds[col][row] ||
                    measuredData[col][row] < lowEnds[col][row])){
                states[col][row] = WellState.FAIL;
            }
        }
    }

    public void primerMixAdjust(){
        System.out.println("adjusting for invitae");
        for(int i = 0; i < 96; i++){
            int col = i % 12;
            int row = 1 / 12;
            if(targetVolumes[col][row] != 0){
                targetVolumes[col][row] *= 2;
                highEnds[col][row] = targetVolumes[col][row] * 1.05 + 10;
                lowEnds[col][row] = targetVolumes[col][row] * 0.95 - 10;
            }
        }
    }

    private void saveFinalSheet(String barcode, String customer) throws IOException {
        if(customer == ""){
            customer = "NoCustomerSpecified";
        }
        String fileLocation = savePath + "/" + customer + "_" + barcode + ".xlsx";
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileLocation));
        XSSFFormulaEvaluator.evaluateAllFormulaCells(template);
        template.write(outputStream);
        outputStream.close();
    }

    public void clearData(){
        states = new WellState[12][8];
        targetVolumes = new double[12][8];
        highEnds = new double[12][8];
        lowEnds = new double[12][8];
        wellPositions = new String[12][8];
        measuredData = new double[12][8];
        states = new WellState[12][8];
        System.out.println("Data has been reset");
    }

    public WellState[][] getResultsArray(){
        return this.states;
    }

    public String getWellPosition(int col, int row){
        return wellPositions[col][row];
    }

    public double getTargetVolume(int col, int row){
        return targetVolumes[col][row];
    }

    public double getUpperThreshold(int col, int row){
        return highEnds[col][row];
    }

    public double getLowerThreshold(int col, int row){
        return lowEnds[col][row];
    }

    public void executePhaseOne(String user, String barcode) throws IOException {
        initializeTemplateFile(user, barcode);
        if(downloadSSRSReport(barcode) == 0) {
            mergeTemplateWithSSRSReport();
        }
        initializeWellStates();
    }

    public int executePhaseTwo(File file, int primerMixFlag) throws IOException {
        importPath = file.getAbsolutePath();
        if(loadMeasuredDataAndMerge() == 0){
            if(primerMixFlag == 2)
                primerMixAdjust();
            updateWellStates();
            return 0;
        }
        else{
            updateWellStates();
            return 1;
        }
    }

    public void executePhaseThree(String barcode, String customer) throws IOException {
        saveFinalSheet(barcode, customer);
        clearData();
    }

    public Double getMeasuredVol(int col, int row) {
        return measuredData[col][row];
    }

    public void setImportPath(String importPath) {
        this.importPath = importPath;
    }

    public void setSsrsReportPath(String ssrsReportPath){
        this.ssrsReportPath = ssrsReportPath;
    }

    public void setTemplatePath(String templatePath){
        this.templatePath = templatePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }
}
