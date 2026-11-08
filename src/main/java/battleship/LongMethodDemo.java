package battleship;

public class LongMethodDemo {

    public static String buildGameReport(String playerName, int shots, int hits, int sunkShips) {
        int misses = shots - hits;
        double accuracy = computeAccuracy(shots, hits);
        int score = computeScore(hits, sunkShips, misses);
        int remainingShips = 11 - sunkShips;

        StringBuilder report = new StringBuilder();
        appendHeader(report, playerName, shots, hits, sunkShips, misses);
        report.append("Accuracy: ").append(accuracy).append("%\n");
        report.append("Rating: ").append(rate(accuracy)).append("\n");
        report.append("Score: ").append(score).append("\n");
        report.append("Rank: ").append(rank(score)).append("\n");
        report.append("Remaining enemy ships: ").append(remainingShips).append("\n");
        report.append("Result: ").append(result(remainingShips, shots)).append("\n");
        report.append("===================\n");
        return report.toString();
    }

    private static void appendHeader(StringBuilder report, String playerName,
                                     int shots, int hits, int sunkShips, int misses) {
        report.append("=== Game Report ===\n");
        report.append("Player: ").append(playerName).append("\n");
        report.append("Total shots: ").append(shots).append("\n");
        report.append("Hits: ").append(hits).append("\n");
        report.append("Sunk ships: ").append(sunkShips).append("\n");
        report.append("Misses: ").append(misses).append("\n");
    }

    private static double computeAccuracy(int shots, int hits) {
        return shots > 0 ? (hits * 100.0) / shots : 0;
    }

    private static int computeScore(int hits, int sunkShips, int misses) {
        return Math.max(0, hits * 10 + sunkShips * 50 - misses * 2);
    }

    private static String rate(double accuracy) {
        if (accuracy >= 80) return "Excellent";
        if (accuracy >= 60) return "Good";
        if (accuracy >= 40) return "Average";
        return "Poor";
    }

    private static String rank(int score) {
        if (score >= 500) return "Admiral";
        if (score >= 300) return "Captain";
        if (score >= 100) return "Sailor";
        return "Cabin boy";
    }

    private static String result(int remainingShips, int shots) {
        if (remainingShips == 0) return "Victory";
        if (shots > 100) return "Too many shots";
        return "In progress";
    }
}