package com.opensurveys.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpreadsheetExportTest {

    @Test
    void storesAnswersAsTextCells() throws Exception {
        byte[] workbook = SpreadsheetExport.responsesWorkbook(List.of(
                new String[] {"answer"},
                new String[] {"=1+1"}
        ));

        String xml = sheetXml(workbook);
        assertFalse(xml.isBlank());
        assertTrue(xml.contains("=1+1"));
        assertFalse(xml.contains("<f>"));
    }

    private static String sheetXml(byte[] workbook) throws Exception {
        Path path = Files.createTempFile("responses", ".xlsx");
        Files.write(path, workbook);
        StringBuilder xml = new StringBuilder();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            zip.stream()
                    .filter(entry -> entry.getName().startsWith("xl/worksheets/")
                            || entry.getName().equals("xl/sharedStrings.xml"))
                    .forEach(entry -> {
                        try (var in = zip.getInputStream(entry)) {
                            xml.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
        } finally {
            Files.deleteIfExists(path);
        }
        return xml.toString();
    }
}
