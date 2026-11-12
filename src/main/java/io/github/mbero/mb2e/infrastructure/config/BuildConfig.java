package io.github.mbero.mb2e.infrastructure.config;

import io.github.mbero.mb2e.adapter.out.external.GitProgram;
import io.github.mbero.mb2e.adapter.out.persistence.FilePublishTarget;
import io.github.mbero.mb2e.adapter.out.persistence.FileSiteSource;
import io.github.mbero.mb2e.adapter.out.persistence.FileVaultReader;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import io.github.mbero.mb2e.domain.service.BuildService;
import io.github.mbero.mb2e.domain.service.PipelineService;
import io.github.mbero.mb2e.domain.service.PublishService;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the build, publish and run use cases to their filesystem and git ports.
 * <p>The domain itself knows no framework. A path that is not set becomes a directory that
 * cannot exist, so an adapter never reads the working directory by mistake; the use case that
 * needs the path refuses before it touches the adapter.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Mb2eProperties.class)
public class BuildConfig {

    /** Parent of the stand-in path for a property that is not set. */
    static final String NOT_SET = "/mb2e-path-not-set/";

    @Bean
    ConfiguredPaths configuredPaths(Mb2eProperties properties) {
        return new ConfiguredPaths(properties.vault().path(), properties.template().path(),
                properties.target().path());
    }

    @Bean
    VaultReader vaultReader(ConfiguredPaths paths) {
        return new FileVaultReader(standIn(paths.vault(), ConfiguredPaths.VAULT_KEY));
    }

    @Bean
    SiteSource siteSource(ConfiguredPaths paths) {
        return new FileSiteSource(standIn(paths.template(), ConfiguredPaths.TEMPLATE_KEY));
    }

    @Bean
    BuildUseCase buildUseCase(VaultReader vault, SiteSource site, ConfiguredPaths paths, Mb2eProperties properties) {
        return new BuildService(vault, site, paths, properties.tag(), properties.inlineTags(),
                properties.defaultNote());
    }

    @Bean
    PublishTarget publishTarget(ConfiguredPaths paths) {
        return new FilePublishTarget(paths.target(), standIn(paths.template(), ConfiguredPaths.TEMPLATE_KEY),
                standIn(paths.vault(), ConfiguredPaths.VAULT_KEY));
    }

    @Bean
    GitRepository gitRepository(ConfiguredPaths paths) {
        return new GitProgram(standIn(paths.target(), ConfiguredPaths.TARGET_KEY));
    }

    @Bean
    PublishUseCase publishUseCase(BuildUseCase build, VaultReader vault, SiteSource site, PublishTarget target,
            GitRepository git, ConfiguredPaths paths, Mb2eProperties properties) {
        return new PublishService(build, vault, site, target, git, paths, properties.force());
    }

    @Bean
    PipelineUseCase pipelineUseCase(BuildUseCase build, PublishUseCase publish) {
        return new PipelineService(build, publish);
    }

    private static Path standIn(String configured, String key) {
        return Path.of(configured != null ? configured : NOT_SET + key);
    }
}
