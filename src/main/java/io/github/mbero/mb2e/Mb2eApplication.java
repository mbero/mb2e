package io.github.mbero.mb2e;

import io.github.mbero.mb2e.adapter.in.shell.ArgumentGuard;
import io.github.mbero.mb2e.adapter.in.shell.RefusalReporter;
import java.util.Optional;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.boot.StandardCommandsAutoConfiguration;

/**
 * Entry point of mb2e.
 * <p>{@code StandardCommandsAutoConfiguration} is excluded so that Spring Shell's built-in
 * commands do not exist; no property, {@code SPRING_APPLICATION_JSON} included, can re-enable it.
 */
@SpringBootApplication(exclude = StandardCommandsAutoConfiguration.class)
public class Mb2eApplication {

    /** Exit status of a refused operation; a successful one exits 0. */
    static final int REFUSED = 1;

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * Runs the shell once and returns the process exit code. Any failure leaves
     * with 1: two codes, not a taxonomy. The framework would give a rejected
     * parameter its own code (2), so no failure is left to its mapping.
     */
    static int run(String... args) {
        Optional<String> refusal = ArgumentGuard.refusal(args);
        if (refusal.isPresent()) {
            System.err.println(refusal.get());
            System.err.flush();
            return REFUSED;
        }
        Mb2eEnvironment environment;
        try {
            environment = Mb2eEnvironment.create(System.getenv(Mb2eEnvironment.JSON_VARIABLE),
                    System.getenv(Mb2eEnvironment.LOCAL_FILE_VARIABLE));
        } catch (Mb2eEnvironment.Refused refused) {
            System.err.println(refused.getMessage());
            System.err.flush();
            return REFUSED;
        }
        RefusalReporter.forget();
        SpringApplication application = new SpringApplication(Mb2eApplication.class);
        // Configuration comes only from the packaged application.yaml and
        // SPRING_APPLICATION_JSON (see Mb2eEnvironment).
        application.setEnvironment(environment);
        // No process argument is a configuration property: every word on the command
        // line is a command or a parameter, or it is refused.
        application.setAddCommandLineProperties(false);
        // The mb2e logo (resources/banner.txt) greets the interactive shell only.
        // A one-shot or script run prints no banner: stdout carries the result
        // alone, and with --json exactly one JSON object.
        application.setBannerMode(args.length == 0 ? Banner.Mode.CONSOLE : Banner.Mode.OFF);
        try (ConfigurableApplicationContext context = application.run(args)) {
            return SpringApplication.exit(context);
        } catch (Throwable failure) {
            // A refusal was reported on standard error by its handler, and the
            // framework's own "Application run failed" log is switched off.
            // Anything that was not reported is said here, once, in one line.
            if (!RefusalReporter.reported()) {
                RefusalReporter.report(failure);
            }
            return REFUSED;
        }
    }
}
