package io.github.mbero.mb2e.adapter.in.shell;

import java.io.File;
import java.util.Optional;

/**
 * Validates the process arguments before Spring Shell parses them.
 * <p>Spring Shell keeps only part of a script-mode command line and splits a word
 * that contains a space, so both cases are decided here and no word is lost.
 */
public final class ArgumentGuard {

    private ArgumentGuard() {
    }

    /**
     * Returns the refusal for a word that is never a command or a parameter.
     * <p>An empty or blank word and a bare {@code --} are refused alike on the command
     * line, in a script and at the prompt, because the line tokenizer drops both.
     */
    public static Optional<String> wordRefusal(String word) {
        if (word.isBlank()) {
            return Optional.of("An empty word is not a command or a parameter. Refused: '" + word + "'");
        }
        if (word.equals("--")) {
            return Optional.of("The word '--' is not a command or a parameter. Refused: --");
        }
        return Optional.empty();
    }

    /** Returns whether the arguments select script mode: a first argument of {@code @} and a file name. */
    public static boolean isScriptMode(String[] args) {
        return args.length > 0 && args[0].startsWith("@") && args[0].length() > 1;
    }

    /**
     * Returns the refusal message for these arguments, or empty when they may run.
     * <p>In script mode the script file is the whole command line, so any second word,
     * a second script file included, is refused.
     */
    public static Optional<String> refusal(String[] args) {
        if (isScriptMode(args)) {
            if (args.length > 1) {
                return Optional.of("Script mode takes one script file and no other word. Refused: " + args[1]);
            }
            String name = args[0].substring(1);
            File file = new File(name);
            if (!file.exists() || file.isDirectory() || !file.canRead()) {
                return Optional.of("The script file cannot be read: " + name);
            }
            return Optional.empty();
        }
        for (String word : args) {
            Optional<String> refused = wordRefusal(word);
            if (refused.isPresent()) {
                return refused;
            }
            if (word.chars().anyMatch(Character::isWhitespace)) {
                return Optional.of("A command or a parameter is one word with no space in it. Refused: '"
                        + word + "'");
            }
        }
        return Optional.empty();
    }
}
