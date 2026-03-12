package ru.just.monolithmvp.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CsvReportRenderer {
    public String render(List<String> header, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, header);
        for (List<String> row : rows) {
            appendCsvRow(csv, row);
        }
        return csv.toString();
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append('\n');
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
