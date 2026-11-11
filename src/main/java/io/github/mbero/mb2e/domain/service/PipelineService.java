package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.model.BuildPlan;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.Datasource;
import io.github.mbero.mb2e.domain.model.PublishInputs;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.SourceNote;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.model.SyncPlan;
import io.github.mbero.mb2e.domain.model.SyncReport;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import io.github.mbero.mb2e.domain.port.in.SyncUseCase;
import io.github.mbero.mb2e.domain.port.out.DatasourceReader;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs sync, build and publish in that order; a refused step stops the rest.
 * <p>The target is checked before anything is written. A dry run plans each step from what
 * the step before it would have left behind and writes nothing.
 */
public class PipelineService implements PipelineUseCase {

    private final SyncUseCase sync;
    private final BuildUseCase build;
    private final PublishUseCase publish;
    private final DatasourceReader datasourceReader;
    private final SiteSource site;

    public PipelineService(SyncUseCase sync, BuildUseCase build, PublishUseCase publish,
            DatasourceReader datasourceReader, SiteSource site) {
        this.sync = sync;
        this.build = build;
        this.publish = publish;
        this.datasourceReader = datasourceReader;
        this.site = site;
    }

    /** The three sections while the run is under way; a step that has not happened is not_run or skipped. */
    private static final class Sections {
        StepResult<SyncReport> sync;
        StepResult<BuildReport> build;
        StepResult<PublishReport> publish;

        Sections(boolean skipSync, boolean skipBuild, boolean noPublish) {
            sync = skipSync ? StepResult.skipped() : StepResult.notRun();
            build = skipBuild ? StepResult.skipped() : StepResult.notRun();
            publish = noPublish ? StepResult.skipped() : StepResult.notRun();
        }
    }

    @Override
    public RunReport run(boolean dryRun, boolean skipSync, boolean skipBuild, boolean noPublish) {
        String traceId = UUID.randomUUID().toString();
        Sections sections = new Sections(skipSync, skipBuild, noPublish);
        steps(traceId, dryRun, skipSync, skipBuild, noPublish, sections);
        return summary(traceId, dryRun, noPublish, sections);
    }

    private void steps(String traceId, boolean dryRun, boolean skipSync, boolean skipBuild, boolean noPublish,
            Sections sections) {
        if (!noPublish) {
            Optional<PublishReport> unusable = publish.checkTarget();
            if (unusable.isPresent()) {
                sections.publish = StepResult.of("refused", unusable.get().withTraceId(traceId));
                return;
            }
        }

        Map<String, byte[]> planned = Map.of();
        if (!skipSync) {
            SyncReport report;
            if (dryRun) {
                SyncPlan plan = sync.plan();
                report = plan.report();
                planned = plan.writes();
            } else {
                report = sync.sync(false);
            }
            sections.sync = StepResult.of(report.outcome(), report.withTraceId(traceId));
            if (report.isRefused()) {
                return;
            }
        }

        String bundleText = null;
        if (!skipBuild) {
            BuildReport report;
            if (dryRun) {
                BuildPlan plan = build.plan(overlay(datasourceReader.read(), planned));
                report = plan.report();
                bundleText = plan.bundleText();
            } else {
                report = build.build(false);
            }
            sections.build = StepResult.of(report.outcome(), report.withTraceId(traceId));
            if (report.isRefused()) {
                return;
            }
        }

        if (!noPublish) {
            PublishReport report = dryRun ? publish.plan(new PublishInputs(bundleText, planned))
                    : publish.publish(false);
            sections.publish = StepResult.of(report.outcome(), report.withTraceId(traceId));
        }
    }

    private RunReport summary(String traceId, boolean dryRun, boolean noPublish, Sections sections) {
        String title = null;
        String detail = null;
        if (sections.sync.report() != null && sections.sync.isRefused()) {
            title = sections.sync.report().title();
            detail = sections.sync.report().detail();
        } else if (sections.build.report() != null && sections.build.isRefused()) {
            title = sections.build.report().title();
            detail = sections.build.report().detail();
        } else if (sections.publish.report() != null && sections.publish.isRefused()) {
            title = sections.publish.report().title();
            detail = sections.publish.report().detail();
        }
        String outcome = title == null ? "ok" : "refused";
        String openPath = noPublish ? site.location() + "/index.html" : null;
        return new RunReport(traceId, outcome, title, detail, dryRun, openPath, sections.sync, sections.build,
                sections.publish);
    }

    /** Returns the datasource as it will be once the planned sync has written its files. */
    static Datasource overlay(Datasource base, Map<String, byte[]> writes) {
        if (writes.isEmpty()) {
            return base;
        }
        Map<String, SourceNote> notes = new LinkedHashMap<>();
        base.notes().forEach(note -> notes.put(note.path(), note));
        List<String> attachments = new ArrayList<>(base.attachments());
        writes.forEach((path, bytes) -> {
            if (path.toLowerCase(Locale.ROOT).endsWith(".md")) {
                notes.put(path, new SourceNote(path, new String(bytes, StandardCharsets.UTF_8)));
            } else if (!attachments.contains(path)) {
                attachments.add(path);
            }
        });
        return new Datasource(base.location(), base.directories(), new ArrayList<>(notes.values()), attachments);
    }
}
