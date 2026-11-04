package personalfinance.infra;

import personalfinance.model.Operation;
import personalfinance.model.OperationType;
import personalfinance.model.Wallet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvWalletStorage {
    private static final List<String> HEADER = List.of("recordType", "operationType", "category", "amount");

    public void exportWallet(Wallet wallet, Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(String.join(",", HEADER));
            writer.newLine();
            for (Operation operation : wallet.getOperations()) {
                writeRecord(writer, List.of(
                        "OPERATION",
                        operation.type().name(),
                        operation.category(),
                        operation.amount().toPlainString()
                ));
            }
            for (Map.Entry<String, BigDecimal> entry : wallet.getBudgets().entrySet()) {
                writeRecord(writer, List.of(
                        "BUDGET", "", entry.getKey(), entry.getValue().toPlainString()
                ));
            }
        }
    }

    public Wallet importWallet(Path file) throws IOException {
        List<List<String>> records = parseRecords(Files.readString(file, StandardCharsets.UTF_8));
        if (records.isEmpty() || !records.getFirst().equals(HEADER)) {
            throw new IOException("Неверный заголовок CSV-файла");
        }

        Wallet wallet = new Wallet();
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            try {
                if (record.size() != 4) {
                    throw new IllegalArgumentException("Ожидалось 4 столбца");
                }
                String category = record.get(2);
                BigDecimal amount = new BigDecimal(record.get(3));
                switch (record.getFirst()) {
                    case "OPERATION" -> wallet.addOperation(
                            OperationType.valueOf(record.get(1)), category, amount
                    );
                    case "BUDGET" -> {
                        if (!record.get(1).isEmpty()) {
                            throw new IllegalArgumentException("Для бюджета тип операции должен быть пустым");
                        }
                        wallet.setBudget(category, amount);
                    }
                    default -> throw new IllegalArgumentException("Неизвестный тип записи: " + record.getFirst());
                }
            } catch (RuntimeException exception) {
                throw new IOException("Некорректная CSV-запись №" + (i + 1), exception);
            }
        }
        return wallet;
    }

    private void writeRecord(BufferedWriter writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write('"' + escape(values.get(i)) + '"');
        }
        writer.newLine();
    }

    private String escape(String value) {
        return value.replace("\"", "\"\"");
    }

    private List<List<String>> parseRecords(String content) throws IOException {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                record.add(field.toString());
                field.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else {
                field.append(current);
            }
        }
        if (quoted) {
            throw new IOException("В CSV-файле не закрыта кавычка");
        }
        if (field.length() > 0 || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
