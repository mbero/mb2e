package io.github.mbero.mb2e.domain.port.out;

/** Thrown when the git program did not run, did not answer in time, or failed. */
public class GitFailure extends RuntimeException {

    public GitFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
