package io.github.mbero.mb2e.domain.port.out;

/** Thrown on a filesystem failure behind a port, with the offending path in the message. */
public class StoreFailure extends RuntimeException {

    public StoreFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
