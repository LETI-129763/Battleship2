package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    @Test
    @DisplayName("Obter o número atribuído à jogada")
    void getNumber() {
        Move move = new Move(7, new ArrayList<>(), new ArrayList<>());

        assertEquals(7, move.getNumber());
    }

    @Test
    @DisplayName("Obter a lista de posições dos tiros")
    void getShots() {
        List<IPosition> shots = new ArrayList<>();
        shots.add(new Position(2, 3));

        Move move = new Move(7, shots, new ArrayList<>());

        assertSame(shots, move.getShots());
        assertEquals(1, move.getShots().size());
        assertEquals(new Position(2, 3), move.getShots().get(0));
    }

    @Test
    @DisplayName("Obter a lista de resultados dos tiros")
    void getShotResults() {
        IGame.ShotResult result =
                new IGame.ShotResult(true, false, null, false);

        List<IGame.ShotResult> results = new ArrayList<>();
        results.add(result);

        Move move = new Move(7, new ArrayList<>(), results);

        assertSame(results, move.getShotResults());
        assertEquals(1, move.getShotResults().size());
        assertEquals(result, move.getShotResults().get(0));
    }

    @Test
    @DisplayName("Representar a jogada com o número e as quantidades de tiros e resultados")
    void testToString() {
        Move empty = new Move(7, new ArrayList<>(), new ArrayList<>());

        assertEquals("Move{number=7, shots=0, results=0}", empty.toString());

        Move populated = new Move(
                8,
                List.of(new Position(2, 3), new Position(4, 5)),
                List.of(new IGame.ShotResult(true, false, null, false))
        );

        assertEquals(
                "Move{number=8, shots=2, results=1}",
                populated.toString()
        );
    }

    @Test
    @DisplayName("Processar uma jogada sem resultados")
    void processEmptyMove() throws Exception {
        Execution execution = execute(true);

        assertCounts(execution.json(), 0, 0, 0);
        assertTrue(execution.json().get("sunkBoats").isEmpty());
        assertTrue(execution.json().get("hitsOnBoats").isEmpty());
        assertTrue(execution.output().contains("Jogada nº7 -> "));
        assertTrue(execution.output().contains(
                Game.NUMBER_SHOTS + " tiros exteriores"
        ));
    }

    @Test
    @DisplayName("Ignorar resultados inválidos mesmo que tenham outros indicadores")
    void processInvalidResults() throws Exception {
        IShip ship = new Frigate(Compass.NORTH, new Position(1, 1));

        Execution execution = execute(
                true,
                new IGame.ShotResult(false, false, null, false),
                new IGame.ShotResult(false, true, ship, true)
        );

        assertCounts(execution.json(), 0, 0, 0);
        assertTrue(execution.json().get("sunkBoats").isEmpty());
        assertTrue(execution.json().get("hitsOnBoats").isEmpty());
    }

    @Test
    @DisplayName("Contar tiros na água e apresentar singular e plural")
    void processMissedShots() throws Exception {
        for (int count = 1; count <= Game.NUMBER_SHOTS; count++) {
            Execution execution = execute(
                    true,
                    repeatedResults(
                            count,
                            new IGame.ShotResult(true, false, null, false)
                    )
            );

            assertCounts(execution.json(), count, 0, count);
            assertTrue(execution.json().get("sunkBoats").isEmpty());
            assertTrue(execution.json().get("hitsOnBoats").isEmpty());

            String expected = count == 1
                    ? "1 tiro válido: 1 tiro na água"
                    : count + " tiros válidos: " + count + " tiros na água";

            assertTrue(execution.output().contains(expected));

            if (Game.NUMBER_SHOTS - count == 1) {
                assertTrue(execution.output().contains("1 tiro exterior"));
            }
        }
    }

    @Test
    @DisplayName("Contar apenas tiros repetidos no singular e no plural")
    void processRepeatedShots() throws Exception {
        for (int count = 1; count <= Game.NUMBER_SHOTS; count++) {
            Execution execution = execute(
                    true,
                    repeatedResults(
                            count,
                            new IGame.ShotResult(true, true, null, false)
                    )
            );

            assertCounts(execution.json(), 0, count, 0);
            assertTrue(execution.json().get("sunkBoats").isEmpty());
            assertTrue(execution.json().get("hitsOnBoats").isEmpty());

            String expected = count == 1
                    ? "1 tiro repetido"
                    : count + " tiros repetidos";

            assertTrue(execution.output().contains(expected));
        }
    }

    @Test
    @DisplayName("Agrupar acertos numa fragata sem a afundar")
    void processShipHits() throws Exception {
        IShip ship = new Frigate(Compass.NORTH, new Position(1, 1));

        for (int count = 1; count <= 2; count++) {
            Execution execution = execute(
                    true,
                    repeatedResults(
                            count,
                            new IGame.ShotResult(true, false, ship, false)
                    )
            );

            assertCounts(execution.json(), count, 0, 0);
            assertTrue(execution.json().get("sunkBoats").isEmpty());

            JsonNode hits = execution.json().get("hitsOnBoats");

            assertEquals(1, hits.size());
            assertEquals("Fragata", hits.get(0).get("type").asText());
            assertEquals(count, hits.get(0).get("hits").asInt());

            String expected = count == 1
                    ? "1 tiro num(a) Fragata"
                    : "2 tiros num(a) Fragata";

            assertTrue(execution.output().contains(expected));
            assertFalse(execution.output().contains("+"));
        }
    }

    @Test
    @DisplayName("Agrupar fragatas afundadas no singular e no plural")
    void processSunkShips() throws Exception {
        for (int count = 1; count <= 2; count++) {
            IGame.ShotResult[] results = new IGame.ShotResult[count];

            for (int i = 0; i < count; i++) {
                IShip ship = new Frigate(
                        Compass.NORTH,
                        new Position(1, i + 1)
                );
                results[i] = new IGame.ShotResult(true, false, ship, true);
            }

            Execution execution = execute(true, results);

            assertCounts(execution.json(), count, 0, 0);
            assertTrue(execution.json().get("hitsOnBoats").isEmpty());

            JsonNode sunk = execution.json().get("sunkBoats");

            assertEquals(1, sunk.size());
            assertEquals("Fragata", sunk.get(0).get("type").asText());
            assertEquals(count, sunk.get(0).get("count").asInt());

            String expected = count == 1
                    ? "1 Fragata ao fundo"
                    : "2 Fragatas ao fundo";

            assertTrue(execution.output().contains(expected));
            assertFalse(execution.output().contains("+"));
        }
    }

    @Test
    @DisplayName("Combinar acertos num barco com tiros na água")
    void processHitsAndMisses() throws Exception {
        IShip ship = new Frigate(Compass.NORTH, new Position(1, 1));

        Execution execution = execute(
                true,
                new IGame.ShotResult(true, false, ship, false),
                new IGame.ShotResult(true, false, null, false)
        );

        assertCounts(execution.json(), 2, 0, 1);
        assertEquals(
                1,
                execution.json().get("hitsOnBoats").get(0).get("hits").asInt()
        );
        assertTrue(execution.output().contains(
                "1 tiro num(a) Fragata + 1 tiro na água"
        ));
    }

    @Test
    @DisplayName("Combinar tiros válidos com um ou vários tiros repetidos")
    void processValidAndRepeatedShots() throws Exception {
        for (int repeated = 1; repeated <= 2; repeated++) {
            IGame.ShotResult[] results =
                    new IGame.ShotResult[1 + repeated];

            results[0] = new IGame.ShotResult(true, false, null, false);

            for (int i = 1; i < results.length; i++) {
                results[i] =
                        new IGame.ShotResult(true, true, null, false);
            }

            Execution execution = execute(true, results);

            assertCounts(execution.json(), 1, repeated, 1);

            String expected = repeated == 1
                    ? ", 1 tiro repetido"
                    : ", 2 tiros repetidos";

            assertTrue(execution.output().contains(
                    "1 tiro válido: 1 tiro na água" + expected
            ));
        }
    }

    @Test
    @DisplayName("Excluir dos acertos a categoria já registada como afundada")
    void processHitsAndSinkingOfSameCategory() throws Exception {
        IShip ship = new Frigate(Compass.NORTH, new Position(1, 1));

        Execution execution = execute(
                true,
                new IGame.ShotResult(true, false, ship, false),
                new IGame.ShotResult(true, false, ship, true),
                new IGame.ShotResult(true, false, null, false)
        );

        assertCounts(execution.json(), 3, 0, 1);
        assertEquals(1, execution.json().get("sunkBoats").size());
        assertEquals(
                1,
                execution.json().get("sunkBoats").get(0).get("count").asInt()
        );
        assertTrue(execution.json().get("hitsOnBoats").isEmpty());
        assertTrue(execution.output().contains(
                "1 Fragata ao fundo + 1 tiro na água"
        ));
    }

    @Test
    @DisplayName("Manter o JSON ao desligar verbose e omitir apenas o resumo da jogada")
    void processWithoutVerbose() throws Exception {
        IGame.ShotResult result =
                new IGame.ShotResult(true, false, null, false);

        Execution verbose = execute(true, result);
        Execution quiet = execute(false, result);

        assertEquals(verbose.json(), quiet.json());
        assertCounts(quiet.json(), 1, 0, 1);

        assertTrue(verbose.output().contains("Jogada nº7 -> "));
        assertFalse(quiet.output().contains("Jogada nº"));

        // A implementação atual imprime o JSON mesmo com verbose=false.
        assertEquals(
                quiet.json(),
                new ObjectMapper().readTree(quiet.output())
        );
    }

    private IGame.ShotResult[] repeatedResults(
            int count,
            IGame.ShotResult result
    ) {
        IGame.ShotResult[] results = new IGame.ShotResult[count];
        java.util.Arrays.fill(results, result);
        return results;
    }

    private Execution execute(
            boolean verbose,
            IGame.ShotResult... results
    ) throws Exception {
        List<IPosition> shots = new ArrayList<>();

        for (int i = 0; i < results.length; i++) {
            shots.add(new Position(1, i + 1));
        }

        Move move = new Move(7, shots, List.of(results));

        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String json;

        try (PrintStream capturedOut =
                     new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            json = move.processEnemyFire(verbose);
        } finally {
            System.setOut(originalOut);
        }

        return new Execution(
                new ObjectMapper().readTree(json),
                buffer.toString(StandardCharsets.UTF_8)
        );
    }

    private void assertCounts(
            JsonNode json,
            int valid,
            int repeated,
            int missed
    ) {
        assertEquals(valid, json.get("validShots").asInt());
        assertEquals(repeated, json.get("repeatedShots").asInt());
        assertEquals(missed, json.get("missedShots").asInt());
        assertEquals(
                Game.NUMBER_SHOTS - valid - repeated,
                json.get("outsideShots").asInt()
        );
    }

    private record Execution(JsonNode json, String output) {
    }
}