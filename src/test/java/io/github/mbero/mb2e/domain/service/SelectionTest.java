package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SelectionTest {

    @Test
    void allThreeSpellingsOfTheTagSelect() {
        assertThat(Selection.frontMatterTags("---\ntags: [a, \"#Blog-Ready\"]\n---\nx")).contains("blog-ready");
        assertThat(Selection.frontMatterTags("---\ntags: a, blog-ready\n---\nx")).contains("blog-ready");
        assertThat(Selection.frontMatterTags("---\ntags:\n  - a\n  - Blog-Ready\ntitle: t\n---\nx"))
                .contains("blog-ready");
        assertThat(Selection.frontMatterTags("---\ntitle: blog-ready\n---\nx")).isEmpty();
        assertThat(Selection.frontMatterTags("no front matter")).isEmpty();
    }

    @Test
    void anInlineTagIsHonouredOnlyOutsideTheFrontMatter() {
        assertThat(Selection.hasInlineTag("---\nt: x\n---\ntext #Blog-Ready here", "blog-ready")).isTrue();
        assertThat(Selection.hasInlineTag("---\nt: #blog-ready\n---\ntext", "blog-ready")).isFalse();
    }

    @Test
    void anInlineTagSelectsOnlyWhenSwitchedOn() {
        String text = "---\ntitle: x\n---\nbody #blog-ready";
        assertThat(Selection.selects(text, "blog-ready", false)).isFalse();
        assertThat(Selection.selects(text, "blog-ready", true)).isTrue();
    }

    @Test
    void theTagIsComparedWithoutHashQuotesOrLetterCase() {
        assertThat(Selection.cleanTag(" \"#Blog-Ready\" ")).isEqualTo("blog-ready");
    }

    @Test
    void aByteOrderMarkAndWindowsLineBreaksAreNormalised() {
        assertThat(Selection.normalise("﻿a\r\nb\rc")).isEqualTo("a\nb\nc");
    }
}
