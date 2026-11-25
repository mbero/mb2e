package io.github.mbero.mb2e;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;

/**
 * Starts the server mode: validates the {@code MB2E_*} variables, builds the environment and starts the web
 * context, which keeps the process alive. A start failure is one line on standard error and exit status 1.
 */
final class ServerLauncher {

    static final String COMMAND = "serve";

    private ServerLauncher() {
    }

    /** Returns whether the arguments name the server mode. */
    static boolean isServe(String[] args) {
        return args.length > 0 && COMMAND.equals(args[0]);
    }

    /** Starts the server with the process environment; returns 0 once the context runs, 1 when it cannot start. */
    static int start(String[] args) {
        return start(args, System.getenv(), ServerLauncher::readFile);
    }

    static int start(String[] args, Map<String, String> environment, Function<Path, String> fileReader) {
        if (args.length > 1) {
            return refuse("The command 'serve' does not take this word: " + args[1]);
        }
        Mb2eEnvironment server;
        try {
            server = Mb2eEnvironment.forServer(environment.get(Mb2eEnvironment.JSON_VARIABLE),
                    ServerEnvironment.properties(environment, fileReader));
        } catch (Mb2eEnvironment.Refused refused) {
            return refuse(refused.getMessage());
        }
        SpringApplication application = new SpringApplication(Mb2eApplication.class);
        application.setEnvironment(server);
        application.setAddCommandLineProperties(false);
        application.setBannerMode(Banner.Mode.OFF);
        try {
            application.run();
            return 0;
        } catch (Throwable failure) {
            Throwable root = failure;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String message = root.getMessage();
            return refuse(message == null || message.isBlank() ? root.getClass().getSimpleName() : message);
        }
    }

    private static int refuse(String message) {
        System.err.println(message);
        System.err.flush();
        return Mb2eApplication.REFUSED;
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
