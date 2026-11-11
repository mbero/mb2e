package io.github.mbero.mb2e.domain.service;

import java.math.BigInteger;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orders strings like {@code Intl.Collator('pl', {numeric: true, sensitivity: 'base'})}: Polish
 * letter order, case and accents ignored unless they form a letter of their own, and digit runs
 * compared as numbers so item9 precedes item10. {@code java.text.Collator} alone lacks the last.
 */
final class PolishOrder implements Comparator<String> {

    private static final Pattern DIGITS = Pattern.compile("[0-9]+|[^0-9]+");

    private final Collator collator;

    PolishOrder() {
        collator = Collator.getInstance(Locale.forLanguageTag("pl"));
        collator.setStrength(Collator.PRIMARY);
    }

    @Override
    public int compare(String a, String b) {
        List<String> left = tokens(a);
        List<String> right = tokens(b);
        for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
            String x = left.get(i);
            String y = right.get(i);
            boolean numeric = Character.isDigit(x.charAt(0)) && Character.isDigit(y.charAt(0))
                    && x.charAt(0) < 0x80 && y.charAt(0) < 0x80;
            int result = numeric ? new BigInteger(x).compareTo(new BigInteger(y)) : collator.compare(x, y);
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private static List<String> tokens(String value) {
        List<String> out = new ArrayList<>();
        Matcher m = DIGITS.matcher(value);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }
}
