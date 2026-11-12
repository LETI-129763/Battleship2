package battleship;

import battleship.ui.JavaFxLauncher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainTest {

    @Test
    @DisplayName("Terminar quando o launcher já tratou o arranque")
    void returnWhenLauncherHandledStartup() {
        String[] args = {"argumento-teste"};

        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (PrintStream capturedOut =
                     new PrintStream(buffer, true, StandardCharsets.UTF_8);
             MockedStatic<JavaFxLauncher> launcher =
                     mockStatic(JavaFxLauncher.class);
             MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {

            launcher.when(() -> JavaFxLauncher.relaunchIfNeeded(args))
                    .thenReturn(true);

            System.setOut(capturedOut);

            Main.main(args);

            launcher.verify(
                    () -> JavaFxLauncher.relaunchIfNeeded(args),
                    times(1)
            );
            tasks.verifyNoInteractions();

            assertEquals(
                    "",
                    buffer.toString(StandardCharsets.UTF_8)
            );
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Apresentar o título e chamar o menu ao continuar o arranque")
    void printTitleAndCallMenu() {
        String[] args = {};

        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (PrintStream capturedOut =
                     new PrintStream(buffer, true, StandardCharsets.UTF_8);
             MockedStatic<JavaFxLauncher> launcher =
                     mockStatic(JavaFxLauncher.class);
             MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {

            launcher.when(() -> JavaFxLauncher.relaunchIfNeeded(args))
                    .thenReturn(false);

            System.setOut(capturedOut);

            Main.main(args);

            launcher.verify(
                    () -> JavaFxLauncher.relaunchIfNeeded(args),
                    times(1)
            );
            tasks.verify(Tasks::menu, times(1));
            tasks.verifyNoMoreInteractions();

            assertEquals(
                    "***  Battleship  ***" + System.lineSeparator(),
                    buffer.toString(StandardCharsets.UTF_8)
            );
        } finally {
            System.setOut(originalOut);
        }
    }
}