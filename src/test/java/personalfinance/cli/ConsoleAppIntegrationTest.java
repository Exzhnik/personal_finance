package personalfinance.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleAppIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void persistsWalletsAndTransfersBetweenRuns() throws IOException, InterruptedException {
        String firstRun = runConsole("""
                register
                alice
                secret
                register
                bob
                secret
                login
                alice
                secret
                income
                Зарплата
                60000
                expense
                Еда
                800
                budget
                Еда
                4000
                transfer
                bob
                250
                logout
                exit
                """);
        String secondRun = runConsole("""
                login
                alice
                secret
                report
                logout
                login
                bob
                secret
                report
                exit
                """);

        assertTrue(firstRun.contains("Перевод выполнен."));
        assertTrue(secondRun.contains("Общий доход: 60000"));
        assertTrue(secondRun.contains("Общие расходы: 1050"));
        assertTrue(secondRun.contains("Текущий баланс: 58950"));
        assertTrue(secondRun.contains("Еда"));
        assertTrue(secondRun.contains("3200"));
        assertTrue(secondRun.contains("Общий доход: 250"));
        assertTrue(secondRun.contains("Перевод от alice"));
    }

    @Test
    void reportsInvalidInputWithoutTerminating() throws IOException, InterruptedException {
        String output = runConsole("""
                register
                alice
                secret
                login
                alice
                secret
                expense
                Еда
                not-a-number
                expense
                Еда
                -10
                income
                Зарплата
                1000
                report
                exit
                """);

        assertTrue(output.contains("Ошибка: Сумма должна быть числом"));
        assertTrue(output.contains("Ошибка: Сумма операции должна быть больше нуля"));
        assertTrue(output.contains("Общий доход: 1000"));
        assertTrue(output.contains("Данные сохранены. Работа завершена."));
    }

    @Test
    void savesCompletedOperationsWhenInputEndsInsideCommand() throws IOException, InterruptedException {
        String[] incompleteCommands = {
                "", "expense\n", "expense\nЕда\n", "budget\nЕда\n",
                "transfer\nbob\n", "summary\nexpense\n", "rename-category\nЕда\n",
                "import\n", "export\n", "logout\nlogin\nalice\n"
        };
        for (int i = 0; i < incompleteCommands.length; i++) {
            String login = "user" + i;
            String output = runConsole("register\n" + login + "\nsecret\nlogin\n" + login
                    + "\nsecret\nincome\nЗарплата\n1000\n" + incompleteCommands[i]);
            assertTrue(output.contains("Данные сохранены"), output);
            String reloaded = runConsole("login\n" + login + "\nsecret\nreport\nexit\n");
            assertTrue(reloaded.contains("Общий доход: 1000"), reloaded);
            assertTrue(reloaded.contains("Общие расходы: 0"), reloaded);
        }
    }

    @Test
    void warnsAboutImportedAndLoadedWalletState() throws IOException, InterruptedException {
        Files.writeString(directory.resolve("wallet.csv"), """
                recordType,operationType,category,amount
                OPERATION,EXPENSE,Еда,150
                BUDGET,,Еда,100
                OPERATION,EXPENSE,Такси,80
                BUDGET,,Такси,100
                """);
        String output = runConsole("register\nalice\nsecret\nlogin\nalice\nsecret\n"
                + "import\nwallet.csv\nexit\n");
        assertWalletWarnings(output);
        assertWalletWarnings(runConsole("login\nalice\nsecret\nexit\n"));
    }

    @Test
    void preservesWalletWhenImportFails() throws IOException, InterruptedException {
        Files.writeString(directory.resolve("invalid.csv"), """
                recordType,operationType,category,amount
                OPERATION,INCOME,Бонус,500
                OPERATION,EXPENSE,Еда,-10
                """);
        String output = runConsole("register\nalice\nsecret\nlogin\nalice\nsecret\n"
                + "income\nЗарплата\n1000\nimport\ninvalid.csv\nreport\nexit\n");
        assertTrue(output.contains("Ошибка работы с файлом"), output);
        assertTrue(output.contains("Общий доход: 1000"), output);
        assertTrue(output.contains("Общие расходы: 0"), output);
    }

    private void assertWalletWarnings(String output) {
        assertTrue(output.contains("бюджет категории Еда превышен на 50"), output);
        assertTrue(output.contains("не менее 80% бюджета категории Такси"), output);
        assertTrue(output.contains("расходы превышают доходы на 230"), output);
    }

    private String runConsole(String commands) throws IOException, InterruptedException {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Path outputFile = Files.createTempFile(directory, "console-", ".log");
        Process process = new ProcessBuilder(
                javaExecutable,
                "-Dfile.encoding=UTF-8",
                "-cp",
                System.getProperty("java.class.path"),
                "personalfinance.Main"
        ).directory(directory.toFile()).redirectErrorStream(true).redirectOutput(outputFile.toFile()).start();

        try {
            try (var input = process.getOutputStream()) {
                input.write(commands.getBytes(StandardCharsets.UTF_8));
            }
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            assertTrue(finished, "Консольное приложение не завершилось за 10 секунд");
            String output = Files.readString(outputFile, StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }
}
