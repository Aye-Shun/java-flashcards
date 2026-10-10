import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** A small terminal quiz for UTF-8, tab-separated flashcard decks. */
public final class Flashcards {
    private static final String USAGE =
            "Usage: java -cp build Flashcards --deck FILE [--limit N] [--shuffle [--seed N]]";

    private Flashcards() {}

    record Card(String prompt, String answer) {}
    record Options(Path deck, int limit, boolean shuffle, Long seed, boolean help) {}

    public static void main(String[] args) {
        Reader input = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        System.exit(run(args, input, System.out, System.err));
    }

    static int run(String[] args, Reader input, PrintStream out, PrintStream err) {
        final Options options;
        try {
            options = parseOptions(args);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            err.println(USAGE);
            return 2;
        }
        if (options.help()) {
            out.println(USAGE);
            out.println("Deck format: one prompt, a tab, then its answer per line.");
            out.println("Answers ignore case and surrounding whitespace. Type :quit to stop.");
            return 0;
        }
        try {
            List<Card> cards = selectCards(loadDeck(options.deck()), options);
            quiz(cards, input, out);
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    static Options parseOptions(String[] args) {
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            return new Options(null, Integer.MAX_VALUE, false, null, true);
        }
        Path deck = null;
        int limit = Integer.MAX_VALUE;
        boolean shuffle = false;
        Long seed = null;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            String name = args[i];
            if (!Set.of("--deck", "--limit", "--shuffle", "--seed").contains(name)) {
                throw new IllegalArgumentException("Unknown option: " + name);
            }
            if (!seen.add(name)) throw new IllegalArgumentException("Repeated option: " + name);
            if (name.equals("--shuffle")) {
                shuffle = true;
                continue;
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + name);
            }
            String value = args[++i];
            try {
                switch (name) {
                    case "--deck" -> deck = Path.of(value);
                    case "--limit" -> {
                        limit = Integer.parseInt(value);
                        if (limit < 1) throw new IllegalArgumentException("--limit must be a positive integer.");
                    }
                    case "--seed" -> seed = Long.parseLong(value);
                    default -> throw new IllegalArgumentException("Unknown option: " + name);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(name + " needs a valid integer.");
            } catch (InvalidPathException e) {
                throw new IllegalArgumentException("Invalid deck path.");
            }
        }
        if (deck == null) throw new IllegalArgumentException("--deck is required.");
        if (seed != null && !shuffle) throw new IllegalArgumentException("--seed requires --shuffle.");
        return new Options(deck, limit, shuffle, seed, false);
    }

    static List<Card> loadDeck(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && line.startsWith("\uFEFF")) line = line.substring(1);
            if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
            String[] fields = line.split("\t", -1);
            if (fields.length != 2 || fields[0].isBlank() || fields[1].isBlank()) {
                throw new IllegalArgumentException("Deck line " + (i + 1)
                        + " must contain a nonempty prompt and answer separated by one tab.");
            }
            cards.add(new Card(fields[0].strip(), fields[1].strip()));
        }
        if (cards.isEmpty()) throw new IllegalArgumentException("The deck has no cards.");
        return List.copyOf(cards);
    }

    static List<Card> selectCards(List<Card> deck, Options options) {
        List<Card> selected = new ArrayList<>(deck);
        if (options.shuffle()) {
            Collections.shuffle(selected, options.seed() == null ? new Random() : new Random(options.seed()));
        }
        return List.copyOf(selected.subList(0, Math.min(options.limit(), selected.size())));
    }

    static boolean matches(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text.strip(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    static void quiz(List<Card> cards, Reader input, PrintStream out) throws IOException {
        BufferedReader reader = input instanceof BufferedReader buffered ? buffered : new BufferedReader(input);
        List<Card> missed = new ArrayList<>();
        int attempted = 0;
        int correct = 0;
        out.println("Flashcards · " + cards.size() + " cards. Type :quit to stop.");
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            out.println();
            out.println("Question " + (i + 1) + "/" + cards.size() + ": " + card.prompt());
            out.print("Your answer: ");
            out.flush();
            String answer = reader.readLine();
            if (answer == null || answer.strip().equals(":quit")) {
                out.println("\nSession stopped.");
                break;
            }
            attempted++;
            if (matches(answer, card.answer())) {
                correct++;
                out.println("Correct.");
            } else {
                missed.add(card);
                out.println("Answer: " + card.answer());
            }
        }
        out.println();
        out.println("Score: " + correct + "/" + attempted + " attempted ("
                + (attempted == 0 ? 0 : Math.round(100.0 * correct / attempted)) + "%).");
        out.println("Unanswered: " + (cards.size() - attempted));
        if (missed.isEmpty()) {
            out.println("No missed cards to review.");
        } else {
            out.println("Review missed cards:");
            for (Card card : missed) out.println("- " + card.prompt() + " -> " + card.answer());
        }
    }
}
