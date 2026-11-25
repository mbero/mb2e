package io.github.mbero.mb2e.infrastructure.config;

/** A secret value whose text form never shows the value; read it with {@link #value()}. */
public final class Secret {

    private final String value;

    public Secret(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "***";
    }
}
