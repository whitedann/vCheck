package sample.excelFiles;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class OutputSheet {

    private FileInputStream fileStream;
    private Workbook wb;
    private Sheet resultSheet, dataSheet;
    String[][] data = null;

    public void setInputFile(String path) throws IOException {
        File file = new File("./src/main/resources/assets/template.xlsx");
        System.out.println(file.getAbsolutePath());
        fileStream = new FileInputStream(file);
        wb = new XSSFWorkbook(fileStream);
        resultSheet = wb.getSheetAt(0);
        dataSheet = wb.getSheetAt(1);
        data = new String[8][12];
        /**for(File e : file.listFiles()) {
            System.out.println(e + " " + e.canRead());
        }**/
    }

    public void parseResult(){
        for(int i = 0; i < 96; i++){
            int col = i % 12;
            int row = i / 12;
            data[row][col] = resultSheet.getRow(i+3).getCell(3).getStringCellValue();
        }
    }

    public void printResults(){
        for(int j = 0; j < data.length; j++){
            for(int i = 0; i < data[0].length; i++){
                System.out.println(data[j][i] + " row: " + (j+1) + " col: " + (i+1));
            }
        }
    }

}
