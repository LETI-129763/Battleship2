package battleship;

import com.opencsv.CSVWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
}