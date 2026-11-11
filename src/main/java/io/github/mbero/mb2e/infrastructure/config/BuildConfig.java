package io.github.mbero.mb2e.infrastructure.config;

import io.github.mbero.mb2e.adapter.out.external.GitProgram;
import io.github.mbero.mb2e.adapter.out.persistence.FileBundleStore;
import io.github.mbero.mb2e.adapter.out.persistence.FileDatasourceFiles;
import io.github.mbero.mb2e.adapter.out.persistence.FilePublishTarget;
import io.github.mbero.mb2e.adapter.out.persistence.FileSiteSource;
import io.github.mbero.mb2e.adapter.out.persistence.FileDatasourceMirror;
import io.github.mbero.mb2e.adapter.out.persistence.FileDatasourceReader;
import io.github.mbero.mb2e.adapter.out.persistence.FileVaultReader;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import io.github.mbero.mb2e.domain.port.in.SyncUseCase;
import io.github.mbero.mb2e.domain.port.out.BundleStore;
import io.github.mbero.mb2e.domain.port.out.DatasourceFiles;
import io.github.mbero.mb2e.domain.port.out.DatasourceMirror;
import io.github.mbero.mb2e.domain.port.out.DatasourceReader;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import io.github.mbero.mb2e.domain.service.BuildService;
import io.github.mbero.mb2e.domain.service.PipelineService;
import io.github.mbero.mb2e.domain.service.PublishService;
import io.github.mbero.mb2e.domain.service.SyncService;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the build and sync use cases to their filesystem ports. The domain itself knows no framework. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Mb2eProperties.class)
public class BuildConfig {

    @Bean
    DatasourceReader datasourceReader(Mb2eProperties properties) {
        return new FileDatasourceReader(Path.of(properties.datasource().path()));
    }

    @Bean
    BundleStore bundleStore(Mb2eProperties properties) {
        return new FileBundleStore(Path.of(properties.site().templatePath()));
    }

    @Bean
    SiteSource siteSource(Mb2eProperties properties) {
        return new FileSiteSource(Path.of(properties.site().templatePath()));
    }

    @Bean
    BuildUseCase buildUseCase(DatasourceReader reader, BundleStore store, SiteSource site, Clock clock,
            Mb2eProperties properties) {
        return new BuildService(reader, store, site, clock, properties.build().defaultNote());
    }

    @Bean
    DatasourceFiles datasourceFiles(Mb2eProperties properties) {
        return new FileDatasourceFiles(Path.of(properties.datasource().path()));
    }

    @Bean
    PublishTarget publishTarget(Mb2eProperties properties) {
        return new FilePublishTarget(properties.publish().targetPath(), Path.of(properties.site().templatePath()),
                Path.of(properties.datasource().path()));
    }

    @Bean
    GitRepository gitRepository(Mb2eProperties properties) {
        return new GitProgram(Path.of(properties.publish().targetPath()));
    }

    @Bean
    PublishUseCase publishUseCase(SiteSource site, PublishTarget target, GitRepository git,
            DatasourceFiles datasource, Mb2eProperties properties) {
        return new PublishService(site, target, git, datasource, properties.publish().expectedRemote(),
                properties.publish().force());
    }

    @Bean
    PipelineUseCase pipelineUseCase(SyncUseCase sync, BuildUseCase build, PublishUseCase publish,
            DatasourceReader reader, SiteSource site) {
        return new PipelineService(sync, build, publish, reader, site);
    }

    @Bean
    VaultReader vaultReader(Mb2eProperties properties) {
        return new FileVaultReader(Path.of(properties.vault().path()));
    }

    @Bean
    DatasourceMirror datasourceMirror(Mb2eProperties properties) {
        return new FileDatasourceMirror(Path.of(properties.datasource().path()));
    }

    @Bean
    SyncUseCase syncUseCase(VaultReader vault, DatasourceMirror mirror, Mb2eProperties properties) {
        return new SyncService(vault, mirror, properties.sync().tag(), properties.sync().inlineTags());
    }
}
