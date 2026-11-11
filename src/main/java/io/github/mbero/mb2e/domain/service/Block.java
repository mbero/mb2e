package io.github.mbero.mb2e.domain.service;

/** A rendered block: its type ("p", "h1", "list", ...) and its HTML. */
record Block(String type, String html) {
}
