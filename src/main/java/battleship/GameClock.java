package battleship;

import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Mede o tempo gasto em cada rajada e o tempo total do jogo,
 * usando o StopWatch da biblioteca Apache Commons Lang.
 */
public class GameClock {

    private final StopWatch moveWatch = new StopWatch();
    private final StopWatch totalWatch = new StopWatch();
    private final List<Long> moveTimesMs = new ArrayList<>();

    /** Chamar quando um novo jogo começa. Arranca o tempo total e o da primeira rajada. */
    public void startGame() {
        moveTimesMs.clear();
        totalWatch.reset();
        totalWatch.start();
        startMove();
    }

    /** Chamar quando começa a contar uma nova rajada. */
    public void startMove() {
        moveWatch.reset();
        moveWatch.start();
    }

    /** Chamar quando o jogador submete a rajada. Devolve o tempo gasto em ms. */
    public long endMove() {
        if (!moveWatch.isStarted()) {
            return 0;
        }
        moveWatch.stop();
        long ms = moveWatch.getTime();
        moveTimesMs.add(ms);
        return ms;
    }

    /** Chamar quando o jogo termina. */
    public void endGame() {
        if (totalWatch.isStarted()) {
            totalWatch.stop();
        }
    }

    /** Formata milissegundos como segundos, por exemplo "8,2 s". */
    public static String format(long ms) {
        return String.format("%.1f s", ms / 1000.0);
    }

    /** Resumo final: total, média, mais rápida e mais lenta. */
    public String summary() {
        if (moveTimesMs.isEmpty()) {
            return "Sem rajadas registadas.";
        }
        long total = totalWatch.getTime();
        long avg = (long) moveTimesMs.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = moveTimesMs.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = moveTimesMs.stream().mapToLong(Long::longValue).max().orElse(0);
        return "Tempo total: " + format(total)
                + " | Média por rajada: " + format(avg)
                + " | Mais rápida: " + format(min)
                + " | Mais lenta: " + format(max);
    }
}
