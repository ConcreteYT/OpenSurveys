package com.opensurveys.service;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Builds an xlsx workbook whose cells are strings, so a spreadsheet will not evaluate them.
 */
public final class SpreadsheetExport {

    private SpreadsheetExport() {
    }

    public static byte[] responsesWorkbook(List<String[]> rows) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = new Workbook(out, "OpenSurveys", "1.0")) {
            Worksheet sheet = workbook.newWorksheet("responses");
            for (int row = 0; row < rows.size(); row++) {
                String[] cells = rows.get(row);
                for (int column = 0; column < cells.length; column++) {
                    sheet.value(row, column, cells[column] == null ? "" : cells[column]);
                }
            }
        }
        return out.toByteArray();
    }
}
