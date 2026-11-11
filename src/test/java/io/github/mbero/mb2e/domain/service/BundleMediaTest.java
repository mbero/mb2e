package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class BundleMediaTest {

    private static String bundle(String notes) {
        return "window.BLOG_DATA = {\"generated\":\"x\",\"notes\":{" + notes + "}};\n";
    }

    @Test
    void theMediaSetIsTheBundlesOwnSrcValuesDecodedInBundleOrder() {
        String text = bundle("\"A\":{\"lead\":\"\",\"html\":\"<p><img src=\\\"assets/media/Java/b%20%C4%85.png\\\" alt=\\\"x\\\"></p>\"},"
                + "\"B\":{\"lead\":\"\",\"html\":\"<img src=\\\"assets/media/a.png\\\"><img src=\\\"assets/media/Java/b%20%C4%85.png\\\">\"}");
        assertThat(BundleMedia.parse(text)).containsExactly(
                new BundleMedia.Ref("Java/b ą.png", "A"), new BundleMedia.Ref("a.png", "B"));
    }

    @Test
    void aSrcWithoutThePrefixIsPassedOverWithoutATrace() {
        String text = bundle("\"A\":{\"lead\":\"\",\"html\":\"<img src=\\\"https://example.com/d.png\\\"><img src=\\\"../typo.png\\\">\"}");
        assertThat(BundleMedia.parse(text)).isEmpty();
    }

    @Test
    void aLeadParagraphIsReadToo() {
        String text = bundle("\"A\":{\"lead\":\"<img src=\\\"assets/media/l.png\\\">\",\"html\":\"\"}");
        assertThat(BundleMedia.parse(text)).extracting(BundleMedia.Ref::path).isEqualTo(List.of("l.png"));
    }

    @Test
    void textThatIsNotABundleIsRejected() {
        assertThatThrownBy(() -> BundleMedia.parse("not json")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BundleMedia.parse("window.X = {\"a\":1};")).isInstanceOf(IllegalArgumentException.class);
    }
}
