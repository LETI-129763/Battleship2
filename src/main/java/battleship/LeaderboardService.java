package battleship;

import java.io.IOException;

/** Operations for storing and presenting leaderboard results. */
public interface LeaderboardService {

    void saveResult(String playerName, int shots) throws IOException;

    void printTop5() throws IOException;
}
