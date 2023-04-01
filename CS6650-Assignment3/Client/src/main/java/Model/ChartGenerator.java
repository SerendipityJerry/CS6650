package Model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ChartGenerator {
  public ChartGenerator(Integer[] plotData, String filePath) throws IOException {
    File csvFile = new File(filePath);
    if (csvFile.exists()) {
      csvFile.delete();
    }

    XSSFWorkbook workbook = new XSSFWorkbook();
    XSSFSheet spreadsheet = workbook.createSheet("Plot Performance");

    Row headerRow = spreadsheet.createRow(0);
    headerRow.createCell(0).setCellValue("Seconds");
    headerRow.createCell(1).setCellValue("Throughput/second");

    for (int i = 0; i < plotData.length; i++) {
      Row row = spreadsheet.createRow(i + 1);
      row.createCell(0).setCellValue(i + 1);
      row.createCell(1).setCellValue(plotData[i]);
    }

    try (FileOutputStream out = new FileOutputStream(csvFile)) {
      workbook.write(out);
    }
  }
}

