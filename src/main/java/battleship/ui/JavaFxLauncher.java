package battleship.ui;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.scene.control.Control;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configures a modular JavaFX launch for IDEs that start Main on the classpath.
 * Uses the JavaFX JARs already resolved by Maven and the current Java executable.
 * The child process shares the console and never initializes JavaFX until requested.
 */
public final class JavaFxLauncher {
    private static final String RELAUNCHED = "battleship.javafx.relaunched";

    private JavaFxLauncher() {
    }

    /**
     * Relaunches the console with JavaFX modules when necessary.
     * An existing modular launch continues in place.
     *
     * @param args original application arguments
     * @return true if a child process has already handled the application
     * @throws IllegalStateException if the modular launch cannot be prepared or run
     */
    public static boolean relaunchIfNeeded(String[] args) {
        if (Platform.class.getModule().isNamed()) {
            return false;
        }
        if (Boolean.getBoolean(RELAUNCHED)) {
            throw new IllegalStateException("Não foi possível carregar os módulos JavaFX.");
        }

        List<String> command = new ArrayList<>();
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
        for (String option : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (option.startsWith("-D") || option.startsWith("-Xmx") || option.startsWith("-Xms")
                    || option.equals("-ea") || option.equals("-esa")) {
                command.add(option);
            }
        }
        command.add("--module-path");
        command.add(modulePath());
        command.add("--add-modules=javafx.controls");
        command.add("--enable-native-access=javafx.graphics");
        if (Runtime.version().feature() >= 24) {
            command.add("--sun-misc-unsafe-memory-access=allow");
        }
        command.add("-D" + RELAUNCHED + "=true");
        command.add("-classpath");
        command.add(System.getProperty("java.class.path"));
        command.add("battleship.Main");
        command.addAll(List.of(args));

        try {
            Process process = new ProcessBuilder(command).inheritIO().start();
            Thread cleanup = new Thread(process::destroy, "javafx-launcher-shutdown");
            Runtime.getRuntime().addShutdownHook(cleanup);
            int exitCode;
            try {
                exitCode = process.waitFor();
            } finally {
                process.destroy();
                try {
                    Runtime.getRuntime().removeShutdownHook(cleanup);
                } catch (IllegalStateException ignored) {
                    // JVM shutdown has begun; the registered hook will terminate the child.
                }
            }
            if (exitCode != 0) {
                System.exit(exitCode);
            }
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível iniciar o jogo com JavaFX.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Arranque do jogo interrompido.", e);
        }
    }

    /** Locates the base, graphics and controls modules without platform-specific paths. */
    private static String modulePath() {
        List<String> modules = new ArrayList<>();
        for (Class<?> type : List.of(Observable.class, Platform.class, Control.class)) {
            try {
                modules.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
            } catch (URISyntaxException | IllegalArgumentException e) {
                throw new IllegalStateException("Não foi possível localizar os módulos JavaFX.", e);
            }
        }
        return String.join(File.pathSeparator, modules);
    }
}
