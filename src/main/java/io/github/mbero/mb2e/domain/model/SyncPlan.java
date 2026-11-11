package io.github.mbero.mb2e.domain.model;

import java.util.Map;

/** A sync that wrote nothing: its report, and the bytes it would write by datasource-relative path. */
public record SyncPlan(SyncReport report, Map<String, byte[]> writes) {

    public SyncPlan {
        writes = Map.copyOf(writes);
    }
}
