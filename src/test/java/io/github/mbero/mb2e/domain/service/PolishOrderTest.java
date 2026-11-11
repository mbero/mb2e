package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolishOrderTest {

    @Test
    void polishLettersAndNumbersAreOrderedAsTheReferenceOrdersThem() {
        List<String> names = new ArrayList<>(List.of("Ćwiczenia", "Cytaty", "item10", "Cache", "item9"));
        names.sort(new PolishOrder());
        assertThat(names).containsExactly("Cache", "Cytaty", "Ćwiczenia", "item9", "item10");
    }
}
