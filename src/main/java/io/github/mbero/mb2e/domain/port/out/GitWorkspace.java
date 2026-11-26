package io.github.mbero.mb2e.domain.port.out;

/**
 * One local clone that the service owns; it can be discarded and created again at any time.
 * <p>Every method throws {@link GitFailure} when git does not run, does not answer in time,
 * or fails for another reason.
 */
public interface GitWorkspace {

    /** The result of a push. */
    enum PushOutcome {
        PUSHED,
        /** The remote branch moved since the last sync. */
        REJECTED
    }

    /** Makes the clone equal to the remote branch and returns the commit id; local changes are lost. */
    String sync();

    /** Returns whether the working tree differs from the last commit. */
    boolean hasChanges();

    /** Stages every change, commits it and returns the new commit id. */
    String commitAll(String subject, String body);

    /** Pushes the branch without force; {@link PushOutcome#REJECTED} means the remote branch moved. */
    PushOutcome push();
}
