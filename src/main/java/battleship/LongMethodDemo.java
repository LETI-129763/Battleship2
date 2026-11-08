package battleship;
    public class LongMethodDemo {

        public static String buildGameReport(String playerName, int shots, int hits, int sunkShips) {
            StringBuilder report = new StringBuilder();
            report.append("=== Game Report ===\n");
            report.append("Player: ").append(playerName).append("\n");
            report.append("Total shots: ").append(shots).append("\n");
            report.append("Hits: ").append(hits).append("\n");
            report.append("Sunk ships: ").append(sunkShips).append("\n");

            int misses = shots - hits;
            report.append("Misses: ").append(misses).append("\n");

            double accuracy = 0;
            if (shots > 0) {
                accuracy = (hits * 100.0) / shots;
            }
            report.append("Accuracy: ").append(accuracy).append("%\n");

            String rating;
            if (accuracy >= 80) {
                rating = "Excellent";
            } else if (accuracy >= 60) {
                rating = "Good";
            } else if (accuracy >= 40) {
                rating = "Average";
            } else {
                rating = "Poor";
            }
            report.append("Rating: ").append(rating).append("\n");

            int score = hits * 10 + sunkShips * 50 - misses * 2;
            if (score < 0) {
                score = 0;
            }
            report.append("Score: ").append(score).append("\n");

            String rank;
            if (score >= 500) {
                rank = "Admiral";
            } else if (score >= 300) {
                rank = "Captain";
            } else if (score >= 100) {
                rank = "Sailor";
            } else {
                rank = "Cabin boy";
            }
            report.append("Rank: ").append(rank).append("\n");

            int remainingShips = 11 - sunkShips;
            report.append("Remaining enemy ships: ").append(remainingShips).append("\n");

            if (remainingShips == 0) {
                report.append("Result: Victory\n");
            } else if (shots > 100) {
                report.append("Result: Too many shots\n");
            } else {
                report.append("Result: In progress\n");
            }

            report.append("===================\n");
            return report.toString();
        }
    }
