package io.github.mbero.mb2e.domain.port.out;

import io.github.mbero.mb2e.domain.model.GitChange;
import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the publication target's git metadata: origin URL, branch and status.
 * <p>Nothing here writes. Each read runs within its own time budget and is never retried.
 */
public interface GitRepository {

    /** Returns the origin URL, or empty when the repository has no origin. */
    Optional<String> originUrl();

    String branch();

    List<GitChange> status();
}
