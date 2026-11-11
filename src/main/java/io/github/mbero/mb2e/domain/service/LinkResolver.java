package io.github.mbero.mb2e.domain.service;

import java.util.Optional;

/** What the renderer asks of the datasource: a note for a link, a file for an embed or an image. */
interface LinkResolver {

    /** A resolved note: the href to write and the title to show when no alias is given. */
    record NoteLink(String href, String title) {
    }

    Optional<NoteLink> resolveNote(String target);

    /** The published address of an attachment, or empty. */
    Optional<String> resolveAsset(String target);
}
