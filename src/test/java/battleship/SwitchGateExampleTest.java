package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwitchGateExampleTest {

    @Test
    @DisplayName("Descrever os valores de um a nove")
    void describeKnownValues() {
        String[] expected = {
                "one", "two", "three",
                "four", "five", "six",
                "seven", "eight", "nine"
        };

        for (int value = 1; value <= 9; value++) {
            assertEquals(
                    expected[value - 1],
                    SwitchGateExample.describe(value),
                    "Descrição incorreta para o valor " + value
            );
        }
    }

    @Test
    @DisplayName("Devolver other para valores fora de um a nove")
    void describeOtherValues() {
        int[] values = {
                Integer.MIN_VALUE, -1, 0, 10, Integer.MAX_VALUE
        };

        for (int value : values) {
            assertEquals(
                    "other",
                    SwitchGateExample.describe(value),
                    "Descrição incorreta para o valor " + value
            );
        }
    }
}