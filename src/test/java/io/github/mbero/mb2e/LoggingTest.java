package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ConsoleAppender;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Verifies that logging never writes to standard output, also for failures before Spring
 * configures logging, such as a malformed SPRING_APPLICATION_JSON. Hence logback.xml, which
 * logback reads on its own, and not logback-spring.xml, which Spring reads late.
 */
class LoggingTest {

    @Test
    void logbackReadsItsConfigurationOnItsOwn() {
        assertThat(getClass().getResource("/logback.xml")).isNotNull();
        assertThat(getClass().getResource("/logback-spring.xml")).isNull();
    }

    @Test
    void everyAppenderWritesToStandardError() {
        Logger root = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(Logger.ROOT_LOGGER_NAME);
        Iterator<?> appenders = root.iteratorForAppenders();
        assertThat(appenders.hasNext()).isTrue();
        while (appenders.hasNext()) {
            assertThat(appenders.next()).isInstanceOfSatisfying(ConsoleAppender.class,
                    appender -> assertThat(appender.getTarget()).isEqualTo("System.err"));
        }
    }
}
