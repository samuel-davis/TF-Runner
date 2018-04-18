package com.davis.tensorflow.utils;

/*import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;*/

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ExcelUtil {
   /* public static List<List<String>> readFromXls(InputStream in) throws IOException {
		HSSFWorkbook wb = new HSSFWorkbook(in);
		HSSFSheet sheet = wb.getSheetAt(0);
		Iterator rows = sheet.rowIterator();
		List<List<String>> dataSet = new LinkedList<List<String>>();
		while (rows.hasNext()) {
			HSSFRow row = (HSSFRow) rows.next();
			Iterator cells = row.cellIterator();
			List<String> data = new ArrayList<String>();
			while (cells.hasNext()) {
				HSSFCell cell = (HSSFCell) cells.next();
				data.add(cell.getStringCellValue());
			}
			dataSet.add(data);
		}
		return dataSet;
	}

	public static void writeToXls(OutputStream out, List<List<String>> dataSet) throws IOException {
		HSSFWorkbook wb = new HSSFWorkbook();

		final int maxRow = 65535;
		List<String> title = dataSet.remove(0);
		HSSFSheet sheet = null;
		HSSFRow row = null;
		HSSFCell cell = null;
		for (int i = 0; i < dataSet.size(); i++) {
			if (i % maxRow == 0) {
				int sheetNum = (i / maxRow) + 1;
				String sheetName = "Sheet" + sheetNum; // name of sheet
				sheet = wb.createSheet(sheetName);
				row = sheet.createRow(0);
				for (int j = 0; j < title.size(); j++) {
					cell = row.createCell(j);
					cell.setCellValue(title.get(j));
				}
			}

			int rowNum = (i % maxRow) + 1;
			List<String> data = dataSet.get(i);
			row = sheet.createRow(rowNum);
			for (int j = 0; j < data.size(); j++) {
				cell = row.createCell(j);
				cell.setCellValue(data.get(j));
			}
		}

		// write this workbook to an Outputstream.
		wb.write(out);
	}

	public static List<List<String>> readFromXlsx(InputStream in) throws IOException {
		XSSFWorkbook wb = new XSSFWorkbook(in);
		XSSFSheet sheet = wb.getSheetAt(0);
		Iterator rows = sheet.rowIterator();
		List<List<String>> dataSet = new LinkedList<List<String>>();
		while (rows.hasNext()) {
			XSSFRow row = (XSSFRow) rows.next();
			Iterator cells = row.cellIterator();
			List<String> data = new ArrayList<String>();
			while (cells.hasNext()) {
				XSSFCell cell = (XSSFCell) cells.next();
				data.add(cell.getStringCellValue());
			}
			dataSet.add(data);
		}
		return dataSet;
	}

	public static void writeToXlsx(OutputStream out, List<List<String>> dataSet) throws IOException {
		String sheetName = "Sheet1"; // name of sheet
		XSSFWorkbook wb = new XSSFWorkbook();
		XSSFSheet sheet = wb.createSheet(sheetName);

		// iterating rows
		for (int i = 0; i < dataSet.size(); i++) {
			XSSFRow row = sheet.createRow(i);
			List<String> data = dataSet.get(i);
			// iterating columns
			for (int j = 0; j < data.size(); j++) {
				XSSFCell cell = row.createCell(j);
				cell.setCellValue(data.get(j));
			}
		}

		// write this workbook to an Outputstream.
		wb.write(out);
	}*/
}