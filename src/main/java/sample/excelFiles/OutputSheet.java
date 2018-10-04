package sample.excelFiles;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sample.elements.WellState;

import java.io.*;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputSheet {
    /**
    private static final String DEFAULT_IMPORT_PATH = "/Users/dwhite/vCheck1.1/src/main/resources/assets/";
    private static final String DEFAULT_SSRSREPORT_PATH = "/Users/dwhite/vCheck1.1/src/main/resources/assets/";
    private static final String DEFAULT_TEMPLATE_PATH = "/Users/dwhite/vCheck1.1/src/main/resources/assets/";
    private static final String DEFAULT_SAVE_PATH = "/Users/dwhite/vCheck1.1";
     **/
    private static final String DEFAULT_IMPORT_PATH = "W:\\\\Employees\\Danny\\dev\\";
    private static final String DEFAULT_SSRSREPORT_PATH = "W:\\\\Employees\\Danny\\dev\\";
    private static final String DEFAULT_TEMPLATE_PATH = "W:\\\\Employees\\Danny\\dev\\";
    private static final String DEFAULT_SAVE_PATH = "W:\\\\Employees\\Danny\\dev\\";
    private String importPath, ssrsReportPath, templatePath, savePath;
    public static Map wellMappings = new HashMap<Character, Integer>();

    private Workbook template, plateVolumeInfo, measuredWorkbook;
    private Sheet topTemplatePage, dataTemplatePage, plateData, measuredDataSheet;
    private double[][] targetVolumes = null;
    private double[][] highEnds = null;
    private double[][] lowEnds = null;
    private double[][] measuredData = null;
    private String[][] wellPositions = null;
    private WellState[][] states = null;

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
        finally {
            client.close();
        }
        return 0;
    }

    public void initializeTemplateFile() throws IOException {
        File inputFile = new File(templatePath + "template.xlsx");
        template = new XSSFWorkbook(new FileInputStream(inputFile));
        dataTemplatePage = template.getSheetAt(1);
        topTemplatePage = template.getSheetAt(0);
    }

    public int downloadSSRSReport(String barcode) throws IOException {

        String url = "http://ssrsreports.idtdna.com/REPORTServer/Pages/ReportViewer.aspx?" +
                "%2fManufacturing%2fSan+Diego%2fPlate+Volume+Information+by+Barcode+ID&rs:Command=Render&rs:Format=Excel&BarcodeID=";
        url += barcode;
        System.out.println(url);

        try {
            URL link = new URL(url);
            OutputStream bos = new BufferedOutputStream(new FileOutputStream(ssrsReportPath + "template.xlsx"));
            InputStream in = link.openStream();
            Files.copy(in, Paths.get(ssrsReportPath + "plateVol2.xlsx"), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e){
            e.printStackTrace();
        }



        /**add check here to delete output file if it is below certain size please and thank you
         * Also should probably have the function return 1 if it does ... **/
        File tmp = new File(ssrsReportPath + "plateVol2.xlsx");
        if(tmp.getTotalSpace() > 1000) {
            plateVolumeInfo = new XSSFWorkbook((new FileInputStream(new File(ssrsReportPath + "plateVol2.xlsx"))));
            plateData = plateVolumeInfo.getSheetAt(0);
        }
        else {
            System.out.println("Failed download");
            return 1;
        }
        return 0;
    }

    private void mergeTemplateWithSSRSReport(){
        dataTemplatePage.setForceFormulaRecalculation(true);
        for(int i = 2; i < plateData.getPhysicalNumberOfRows(); i++){
            for(int j = 1; j <= 4; j++) {
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

    private void loadMeasuredDataAndMerge() throws IOException {
        Reader reader = Files.newBufferedReader(Paths.get(importPath + "measuredData.csv"));
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
        List<Double> tempValList = new ArrayList<>();
        for(CSVRecord csvRecord : csvParser){
            double val = Double.parseDouble(csvRecord.get(4));
            tempValList.add(val);
        }
        for(int i = 0; i < 96; i++){
            int row = i / 12;
            int col = i % 12;
            measuredData[col][row] = tempValList.get(i);
            topTemplatePage.getRow(i+3).getCell(2).setCellValue(tempValList.get(i));
        }
        /**
        measuredWorkbook = new XSSFWorkbook(new FileInputStream(new File(path)));
        measuredDataSheet = measuredWorkbook.getSheetAt(0);
        double val;
        for(int i = 0; i < 96; i++){
            val = measuredDataSheet.getRow(i).getCell(4).getNumericCellValue();
            topTemplatePage.getRow(i+3).getCell(2).setCellValue(val);
            int row = i / 12;
            int col = i % 12;
            measuredData[col][row] = val;
        }
        measuredWorkbook.close();
         **/
    }

    public void updateWellStates(){
        for(int i = 0; i < 96; i++){
            int col = i % 12;
            int row = i / 12;
            if(targetVolumes[col][row] == 0 && measuredData[col][row] == 0){
                states[col][row] = WellState.EMPTY;
                continue;
            }
            if(measuredData[col][row] == 0){
                states[col][row] = WellState.NODATA;
            }
            else if(measuredData[col][row] <= highEnds[col][row] &&
                    measuredData[col][row] >= lowEnds[col][row]){
                states[col][row] = WellState.PASS;
            }
            else if((measuredData[col][row] > highEnds[col][row] ||
                    measuredData[col][row] < lowEnds[col][row])){
                states[col][row] = WellState.FAIL;
            }
            else
                states[col][row] = WellState.EMPTY;
        }
    }

    private void saveFinalSheet(String barcode, String customer) throws IOException {
        String fileLocation = savePath + "/" + customer + "_" + barcode + ".xlsx";
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileLocation));
        XSSFFormulaEvaluator.evaluateAllFormulaCells(template);
        template.write(outputStream);
        outputStream.close();
    }

    public void clearData(){
        targetVolumes = null;
        highEnds = null;
        lowEnds = null;
        measuredData = null;
        wellPositions = null;
        states = null;
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

    public void executePhaseOne(String barcode) throws IOException {
        initializeTemplateFile();
        if(downloadSSRSReport(barcode) == 0) {
            mergeTemplateWithSSRSReport();
        }
        updateWellStates();
    }

    public void executePhaseTwo() throws IOException {
        loadMeasuredDataAndMerge();
        updateWellStates();
    }

    public void executePhaseTwo(File file) throws IOException {
        importPath = file.getAbsolutePath();
        loadMeasuredDataAndMerge();
        updateWellStates();
    }

    public void executePhaseThree(String barcode, String customer) throws IOException {
        saveFinalSheet(barcode, customer);
        clearData();
    }

    public String getMeasuredVol(int col, int row) {
        return String.valueOf(measuredData[col][row]);
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
