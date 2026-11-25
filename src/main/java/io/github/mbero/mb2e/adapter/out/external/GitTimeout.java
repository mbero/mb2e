package io.github.mbero.mb2e.adapter.out.external;

import io.github.mbero.mb2e.domain.port.out.GitFailure;

/** A git invocation that did not answer within its budget. */
final class GitTimeout extends GitFailure {

    GitTimeout(String message) {
        super(message, null);
    }
}
