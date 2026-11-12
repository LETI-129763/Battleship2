package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QualityGateExamples129360Test {
    @Test
    @DisplayName("The Checkstyle example returns the student number")
    void returnsStudentNumber() {
        assertEquals(129360, QualityGateExamples129360.fixedCheckstyleViolationExample());
    }

    @Test
    @DisplayName("The private Qodana example sums all six parameters")
    void sumsParameters() throws ReflectiveOperationException {
        // This demonstration has no public caller; reflection keeps its visibility unchanged.
        var method = QualityGateExamples129360.class.getDeclaredMethod("qodanaParameterListTest",
                int.class, int.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        assertEquals(21, method.invoke(null, 1, 2, 3, 4, 5, 6));
        assertEquals(-5, method.invoke(null, -10, 2, 0, -4, 8, -1));
        assertEquals(0, method.invoke(null, 0, 0, 0, 0, 0, 0));
    }
}
