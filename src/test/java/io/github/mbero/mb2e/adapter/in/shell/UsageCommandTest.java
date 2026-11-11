package io.github.mbero.mb2e.adapter.in.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class UsageCommandTest {

    private static final List<String> RETIRED = List.of(
            "--vault", "--tag", "--inline-tags", "--target", "--dest",
            "--no-prune", "--yes", "--with-datasource", "--force");

    private static final Set<String> ACCEPTED = Set.of(
            "--json", "--dry-run", "--skip-sync", "--skip-build", "--no-publish");

    private final String text = new UsageCommand().text();

    @Test
    void namesEachWorkingCommandFirstOnItsOwnLine() {
        for (String command : List.of("run", "sync", "build", "publish")) {
            assertThat(text.lines().anyMatch(l -> l.strip().startsWith(command + " ")))
                    .as("a line starting with %s", command).isTrue();
        }
    }

    @Test
    void namesEachAcceptedParameter() {
        assertThat(text).contains(ACCEPTED);
    }

    @Test
    void namesNoRetiredSpelling() {
        assertThat(text).doesNotContain(RETIRED);
    }

    @Test
    void everyDoubleDashTokenIsAnAcceptedParameter() {
        Set<String> found = new TreeSet<>();
        Matcher m = Pattern.compile("--[A-Za-z][A-Za-z-]*").matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        assertThat(found).isSubsetOf(ACCEPTED);
    }
}
