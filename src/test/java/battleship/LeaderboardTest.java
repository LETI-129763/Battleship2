package battleship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardTest {

    @TempDir
    Path tempDir;

    private Leaderboard leaderboard;
    private Path csvFile;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        csvFile = tempDir.resolve("leaderboard.csv");
        leaderboard = new Leaderboard();

        // Isola os testes sem alterar o código de produção.
        Field field = Leaderboard.class.getDeclaredField("filePath");
        field.setAccessible(true);
        field.set(leaderboard, csvFile);
    }

    @Test
    @DisplayName("Guardar um resultado e remover espaços do nome")
    void saveResult() throws IOException {
        leaderboard.saveResult("  Ana  ", 12);

        assertTrue(Files.exists(csvFile));

        String content = Files.readString(csvFile, StandardCharsets.UTF_8);

        assertEquals(
                "\"Ana\",\"12\"",
                content.strip()
        );
    }

    @Test
    @DisplayName("Acrescentar resultados sem apagar os anteriores")
    void saveResultAppendsEntries() throws IOException {
        leaderboard.saveResult("Ana", 12);
        leaderboard.saveResult("Bruno", 20);

        assertEquals(
                java.util.List.of(
                        "\"Ana\",\"12\"",
                        "\"Bruno\",\"20\""
                ),
                Files.readAllLines(csvFile, StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("Rejeitar nomes nulos, vazios ou apenas com espaços")
    void saveResultRejectsInvalidNames() {
        String[] invalidNames = {null, "", "   ", "\t"};

        for (String name : invalidNames) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> leaderboard.saveResult(name, 10)
            );

            assertEquals(
                    "Player name cannot be empty.",
                    exception.getMessage()
            );
        }

        assertFalse(Files.exists(csvFile));
    }

    @Test
    @DisplayName("Rejeitar resultados com zero ou menos tiros")
    void saveResultRejectsInvalidShots() {
        int[] invalidShots = {0, -1, -20};

        for (int shots : invalidShots) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> leaderboard.saveResult("Ana", shots)
            );

            assertEquals(
                    "Shots must be greater than zero.",
                    exception.getMessage()
            );
        }

        assertFalse(Files.exists(csvFile));
    }

    @Test
    @DisplayName("Mostrar uma mensagem quando o ficheiro não existe")
    void printTop5WithoutFile() throws IOException {
        String output = captureTop5();

        assertTrue(output.contains("=== LEADERBOARD - TOP 5 ==="));
        assertTrue(output.contains("No completed games yet."));
        assertFalse(Files.exists(csvFile));
    }

    @Test
    @DisplayName("Mostrar uma mensagem quando o ficheiro está vazio")
    void printTop5WithEmptyFile() throws IOException {
        Files.createFile(csvFile);

        String output = captureTop5();

        assertTrue(output.contains("No completed games yet."));
        assertFalse(output.contains("Rank"));
    }

    @Test
    @DisplayName("Ordenar por tiros e apresentar apenas os cinco melhores")
    void printTop5() throws IOException {
        leaderboard.saveResult("Filipe", 60);
        leaderboard.saveResult("Carla", 30);
        leaderboard.saveResult("Ana", 10);
        leaderboard.saveResult("Eva", 50);
        leaderboard.saveResult("Bruno", 20);
        leaderboard.saveResult("Diogo", 40);

        String output = captureTop5();

        String expected = System.lineSeparator()
                + "=== LEADERBOARD - TOP 5 ==="
                + System.lineSeparator()
                + String.format(
                "%-6s %-25s %s%n",
                "Rank", "Player", "Shots"
        )
                + String.format("%-6d %-25s %d%n", 1, "Ana", 10)
                + String.format("%-6d %-25s %d%n", 2, "Bruno", 20)
                + String.format("%-6d %-25s %d%n", 3, "Carla", 30)
                + String.format("%-6d %-25s %d%n", 4, "Diogo", 40)
                + String.format("%-6d %-25s %d%n", 5, "Eva", 50);

        // O cabeçalho usa "\n" diretamente no código de produção.
        assertEquals(normalize(expected), normalize(output));
        assertFalse(output.contains("Filipe"));
    }

    @Test
    @DisplayName("Apresentar todos os resultados quando existem menos de cinco")
    void printTop5WithFewResults() throws IOException {
        leaderboard.saveResult("Bruno", 20);
        leaderboard.saveResult("Ana", 10);

        String output = captureTop5();

        assertTrue(output.contains(
                String.format("%-6d %-25s %d", 1, "Ana", 10)
        ));
        assertTrue(output.contains(
                String.format("%-6d %-25s %d", 2, "Bruno", 20)
        ));
        assertEquals(4, output.lines()
                .filter(line -> !line.isBlank())
                .count());
    }

    @Test
    @DisplayName("Rejeitar entradas com estrutura ou nome inválidos")
    void printTop5RejectsInvalidEntries() throws IOException {
        String[] invalidEntries = {
                "\"Ana\"\n",
                "\"Ana\",\"10\",\"extra\"\n",
                "\"\",\"10\"\n",
                "\"   \",\"10\"\n"
        };

        for (String entry : invalidEntries) {
            Files.writeString(csvFile, entry, StandardCharsets.UTF_8);

            IOException exception = assertThrows(
                    IOException.class,
                    () -> leaderboard.printTop5()
            );

            assertEquals(
                    "Invalid leaderboard entry.",
                    exception.getMessage()
            );
        }
    }

    @Test
    @DisplayName("Rejeitar contagens de tiros que não são números inteiros")
    void printTop5RejectsNonNumericShots() throws IOException {
        Files.writeString(
                csvFile,
                "\"Ana\",\"abc\"\n",
                StandardCharsets.UTF_8
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> leaderboard.printTop5()
        );

        assertEquals(
                "Invalid shot count in leaderboard.",
                exception.getMessage()
        );
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    @Test
    @DisplayName("Rejeitar contagens de tiros nulas ou negativas no CSV")
    void printTop5RejectsNonPositiveShots() throws IOException {
        String[] invalidShots = {"0", "-5"};

        for (String shots : invalidShots) {
            Files.writeString(
                    csvFile,
                    "\"Ana\",\"" + shots + "\"\n",
                    StandardCharsets.UTF_8
            );

            IOException exception = assertThrows(
                    IOException.class,
                    () -> leaderboard.printTop5()
            );

            assertEquals(
                    "Invalid shot count in leaderboard.",
                    exception.getMessage()
            );
        }
    }

    @Test
    @DisplayName("Guardar e ler nomes com vírgulas, aspas e acentos")
    void saveAndReadSpecialCharacters() throws IOException {
        String name = "João, \"Capitão\"";

        leaderboard.saveResult(name, 15);

        String output = captureTop5();

        assertTrue(output.contains(
                String.format("%-6d %-25s %d", 1, name, 15)
        ));
    }

    @Test
    @DisplayName("Propagar erros de acesso ao ficheiro")
    void rejectDirectoryInsteadOfFile() throws IOException {
        Files.createDirectory(csvFile);

        assertThrows(
                IOException.class,
                () -> leaderboard.saveResult("Ana", 10)
        );
        assertThrows(
                IOException.class,
                () -> leaderboard.printTop5()
        );
    }

    private String captureTop5() throws IOException {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (PrintStream capturedOut =
                     new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            leaderboard.printTop5();
        } finally {
            System.setOut(originalOut);
        }

        return buffer.toString(StandardCharsets.UTF_8);
    }

    private String normalize(String text) {
        return text.replace("\r\n", "\n");
    }
}