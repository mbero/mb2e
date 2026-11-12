package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.infrastructure.config.Mb2eProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;

/** Verifies the keys of the configuration and the defaults of the optional ones. */
class Mb2ePropertiesTest {

    private static Mb2eProperties bind(Map<String, Object> source) {
        Mb2eEnvironment environment = Mb2eEnvironment.create(null);
        environment.getPropertySources().addFirst(new MapPropertySource("test", source));
        return Binder.get(environment).bind("mb2e", Mb2eProperties.class).get();
    }

    @Test
    void thePathsAreNotSetByDefaultAndTheOptionalKeysHaveTheirDefaults() {
        Mb2eProperties properties = bind(Map.of("mb2e.log.file", ""));
        assertThat(properties.vault().path()).isNull();
        assertThat(properties.template().path()).isNull();
        assertThat(properties.target().path()).isNull();
        assertThat(properties.tag()).isEqualTo("blog-ready");
        assertThat(properties.inlineTags()).isFalse();
        assertThat(properties.defaultNote()).isEqualTo("about");
        assertThat(properties.force()).isFalse();
        assertThat(properties.log().file()).isEmpty();
    }

    @Test
    void everyKeyIsBoundFromItsName() {
        Mb2eProperties properties = bind(Map.of(
                "mb2e.vault.path", "/v", "mb2e.template.path", "/tpl", "mb2e.target.path", "/t",
                "mb2e.tag", "pub", "mb2e.inline-tags", "true", "mb2e.default-note", "start",
                "mb2e.force", "true", "mb2e.log.file", "/l"));
        assertThat(properties.vault().path()).isEqualTo("/v");
        assertThat(properties.template().path()).isEqualTo("/tpl");
        assertThat(properties.target().path()).isEqualTo("/t");
        assertThat(properties.tag()).isEqualTo("pub");
        assertThat(properties.inlineTags()).isTrue();
        assertThat(properties.defaultNote()).isEqualTo("start");
        assertThat(properties.force()).isTrue();
        assertThat(properties.log().file()).isEqualTo("/l");
    }

    @Test
    void anEmptyStartNoteStaysEmpty() {
        assertThat(bind(Map.of("mb2e.default-note", "")).defaultNote()).isEmpty();
    }
}
