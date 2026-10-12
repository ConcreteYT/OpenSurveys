package com.opensurveys.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormMediaControllerCsvTest {

    @Test
    void prefixesSpreadsheetFormulaCellsBeforeQuoting() {
        assertEquals("'=1+1", FormMediaController.csvCell("=1+1"));
        assertEquals("'+1", FormMediaController.csvCell("+1"));
        assertEquals("'-1", FormMediaController.csvCell("-1"));
        assertEquals("'@SUM(A1)", FormMediaController.csvCell("@SUM(A1)"));
        assertEquals("hello", FormMediaController.csvCell("hello"));
        assertEquals("\"say, hi\"", FormMediaController.csvCell("say, hi"));
        assertEquals("\"'=1,2\"", FormMediaController.csvCell("=1,2"));
        assertEquals("\"he said \"\"hi\"\"\"", FormMediaController.csvCell("he said \"hi\""));
        assertEquals("", FormMediaController.csvCell(null));
    }
}
