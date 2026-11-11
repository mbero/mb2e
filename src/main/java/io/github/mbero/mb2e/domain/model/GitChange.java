package io.github.mbero.mb2e.domain.model;

/** One line of {@code git status --porcelain}: the two status letters and the path. */
public record GitChange(String code, String path) {

    public boolean isUntracked() {
        return code.equals("??");
    }

    public boolean isDeletion() {
        return code.indexOf('D') >= 0;
    }
}
