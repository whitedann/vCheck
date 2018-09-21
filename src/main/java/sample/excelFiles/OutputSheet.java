package sample.excelFiles;

import javafx.scene.shape.Circle;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sample.elements.WellState;

import java.io.*;

public class OutputSheet {

    private Workbook template, plateVolumeInfo;
    private Sheet resultSheet, dataSheet, plateData;
    double[][] targetData = null;
    double[][] highEnds = null;
    double[][] lowEnds = null;
    String[][] wellPositions = null;
    WellState[][] states = null;

    public OutputSheet() throws IOException {
    }

    private void downloadTemplate() throws IOException {
        String url = "https://idtdna.mastercontrol.com/mc/login/index.cfm?action=login";

        BasicCookieStore store = new BasicCookieStore();
        CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(store).build();

        try {
            HttpUriRequest login = RequestBuilder.post()
                    .setUri(url)
                    .addParameter("username", "dwhite")
                    .addParameter("username2", "")
                    .addParameter("initialRequest", "")
                    .addParameter("password", "Pwner4509!")
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
                System.out.println("Status: " + response3.getStatusLine());
                BufferedInputStream bis = new BufferedInputStream(entity.getContent());
                if(bis.available() == 1){
                    System.out.println("Could not connect to Master Control");
                    bis.close();
                    return;
                }
                BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(new File("./cool.xlsx")));
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
    }

    private void downloadPlateVolumeInfo() {
        /** TODO: Make this! **/
        for(int i = 3; i < 99; i++){
            resultSheet.getRow(i).getCell(2).setCellValue(100);
        }
    }

    public void prepareDocument() throws IOException {
        downloadTemplate();
        setInputFile();
        downloadPlateVolumeInfo();
        mergeTemplateWithPlateVolume();
        saveFinalSheet();
    }

    private void setInputFile() throws IOException {
        template = new XSSFWorkbook(new FileInputStream(new File("./cool.xlsx")));
        dataSheet = template.getSheetAt(1);
        resultSheet = template.getSheetAt(0);

        plateVolumeInfo = new XSSFWorkbook(new FileInputStream(new File("./src/main/resources/assets/plateVol.xlsx")));
        plateData = plateVolumeInfo.getSheetAt(0);

        states = new WellState[12][8];
        targetData = new double[12][8];
        highEnds = new double[12][8];
        lowEnds = new double[12][8];
        wellPositions = new String[12][8];
    }

    private void mergeTemplateWithPlateVolume(){
        dataSheet.setForceFormulaRecalculation(true);
        for(int i = 2; i < plateData.getPhysicalNumberOfRows(); i++){
            for(int j = 1; j <= 4; j++) {
                dataSheet.getRow(i-2).getCell(0).setCellValue(plateData.getRow(i).getCell(0).getStringCellValue());
                dataSheet.getRow(i-2).getCell(j).setCellValue(plateData.getRow(i).getCell(j).getNumericCellValue());
            }
            dataSheet.getRow(i-2).createCell(5);
            dataSheet.getRow(i-2).getCell(5).setCellValue(plateData.getRow(i).getCell(5).getNumericCellValue());
        }

    }

    private void saveFinalSheet() throws IOException {
        File currDir = new File(".");
        String path = currDir.getAbsolutePath();
        String fileLocation = path.substring(0, path.length() - 1) + "temp1.xlsx";
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileLocation));
        XSSFFormulaEvaluator.evaluateAllFormulaCells(template);
        template.write(outputStream);
        outputStream.close();
    }

    public void parseResult() throws IOException {
        for(int i = 0; i < 96; i++){
            int col = i % 12;
            int row = i / 12;
            double targetVol = resultSheet.getRow(i+3).getCell(2).getNumericCellValue();
            targetData[col][row] = dataSheet.getRow(i).getCell(3).getNumericCellValue();
            highEnds[col][row] = targetData[col][row] * 1.05 + 10;
            lowEnds[col][row] = targetData[col][row] * 0.95 - 10;
            wellPositions[col][row] = resultSheet.getRow(i+3).getCell(0).getStringCellValue();
            if(targetData[col][row] == 0){
                states[col][row] = WellState.NODATA;
            }
            else if(targetVol > highEnds[col][row] || targetVol < lowEnds[col][row]) {
                states[col][row] = WellState.FAIL;
            }
            else {
                states[col][row] = WellState.PASS;
            }
        }
    }

    public WellState[][] getResultsArray(){
        return this.states;
    }

    public String getWellPosition(int col, int row){
        return wellPositions[col][row];
    }

    public double getTargetVolume(int col, int row){
        return targetData[col][row];
    }

    public double getUpperThreshold(int col, int row){
        return highEnds[col][row];
    }

    public double getLowerThreshold(int col, int row){
        return lowEnds[col][row];
    }


}
