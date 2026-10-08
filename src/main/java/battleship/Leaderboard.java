package battleship;

import com.opencsv.CSVWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Leaderboard {

    private final Path filePath = Path.of("leaderboard.csv");

    public void saveResult(String playerName, int shots) throws IOException {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be empty.");
        }

        if (shots <= 0) {
            throw new IllegalArgumentException("Shots must be greater than zero.");
        }

        try (CSVWriter writer = new CSVWriter(
                Files.newBufferedWriter(
                        filePath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND))) {

            String[] result = {
                    playerName.trim(),
                    String.valueOf(shots)
            };

            writer.writeNext(result);

            if (writer.checkError()) {
                throw new IOException("Could not save the leaderboard result.");
            }
        }
    }

    private record Result(String playerName, int shots) {
    }

    private List<Result> loadResults() throws IOException {
        List<Result> results = new ArrayList<>();

        if (Files.notExists(filePath)) {
            return results;
        }

        try (CSVReader reader = new CSVReader(
                Files.newBufferedReader(filePath, StandardCharsets.UTF_8))) {

            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row.length != 2 || row[0].isBlank()) {
                    throw new IOException("Invalid leaderboard entry.");
                }

                int shots;

                try {
                    shots = Integer.parseInt(row[1]);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid shot count in leaderboard.", e);
                }

                if (shots <= 0) {
                    throw new IOException("Invalid shot count in leaderboard.");
                }

                results.add(new Result(row[0], shots));
            }
        } catch (CsvValidationException e) {
            throw new IOException("Could not read the leaderboard CSV.", e);
        }

        return results;
    }

    public void printTop5() throws IOException {
        List<Result> results = loadResults();

        results.sort(Comparator.comparingInt(Result::shots));

        System.out.println("\n=== LEADERBOARD - TOP 5 ===");

        if (results.isEmpty()) {
            System.out.println("No completed games yet.");
            return;
        }

        System.out.printf("%-6s %-25s %s%n", "Rank", "Player", "Shots");

        int limit = Math.min(5, results.size());

        for (int i = 0; i < limit; i++) {
            Result result = results.get(i);

            System.out.printf(
                    "%-6d %-25s %d%n",
                    i + 1,
                    result.playerName(),
                    result.shots()
            );
        }
    }
}