package ru.just.monolithmvp.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
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

    public void writeHeader(Writer writer, List<String> header) throws IOException {
        appendCsvRow(writer, header);
    }

    public void writeRow(Writer writer, List<String> row) throws IOException {
        appendCsvRow(writer, row);
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

    private void appendCsvRow(Writer writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(';');
            }
            writer.write(escape(values.get(i)));
        }
        writer.write('\n');
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
