import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class FlashcardsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path deck = Files.createTempFile("flashcards-test-", ".tsv");
        try {
            Files.writeString(deck, "\uFEFF# test deck\n\n Capital of France? \t Paris \n2 + 2?\t4\nAccent?\tcafé\n",
                    StandardCharsets.UTF_8);
            List<Flashcards.Card> cards = Flashcards.loadDeck(deck);
            check(cards.size() == 3 && cards.get(0).answer().equals("Paris"), "comments, BOM, and trimmed fields");
            check(Flashcards.matches("  PARIS  ", "Paris"), "case and edge whitespace");
            check(Flashcards.matches("cafe\u0301", "café"), "Unicode normalization");
            check(!Flashcards.matches("Paris!", "Paris"), "punctuation remains significant");
            Locale previous = Locale.getDefault();
            try {
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                check(Flashcards.matches("INPUT", "input"), "locale-independent comparison");
            } finally { Locale.setDefault(previous); }

            Flashcards.Options shuffled = Flashcards.parseOptions(new String[] {
                    "--deck", deck.toString(), "--shuffle", "--seed", "7", "--limit", "2"});
            List<Flashcards.Card> selected = Flashcards.selectCards(cards, shuffled);
            check(selected.size() == 2 && selected.equals(Flashcards.selectCards(cards, shuffled)),
                    "seeded shuffle and limit");
            check(cards.get(0).prompt().equals("Capital of France?"), "deck order is not mutated");
            check(Flashcards.selectCards(cards, new Flashcards.Options(deck, 99, false, null, false))
                    .equals(cards), "oversized limit includes whole deck");

            Result quiz = run("PARIS\nwrong\ncafe\u0301\n", "--deck", deck.toString());
            check(quiz.code == 0 && quiz.out.contains("Score: 2/3 attempted (67%)."), "real quiz score");
            check(quiz.out.contains("- 2 + 2? -> 4") && !quiz.out.contains("- Capital of France? ->"),
                    "review lists only missed cards");
            Result quit = run(":quit\n", "--deck", deck.toString());
            check(quit.code == 0 && quit.out.contains("Score: 0/0 attempted (0%).")
                    && quit.out.contains("Unanswered: 3"), "early quit does not count unattempted cards");
            Result eof = run("Paris\n", "--deck", deck.toString());
            check(eof.out.contains("Score: 1/1 attempted (100%).") && eof.out.contains("Unanswered: 2"),
                    "EOF preserves attempted score");
            check(run("\n4\ncafé\n", "--deck", deck.toString()).out.contains("Score: 2/3"),
                    "blank answer is an attempted miss");

            for (String bad : List.of("missing separator", "question\t", "\tanswer", "q\ta\textra")) {
                Files.writeString(deck, "# header\n" + bad, StandardCharsets.UTF_8);
                Result result = run("", "--deck", deck.toString());
                check(result.code == 1 && result.err.contains("Deck line 2"), "invalid deck: " + bad);
            }
            Files.writeString(deck, "# comments only\n\n", StandardCharsets.UTF_8);
            check(run("", "--deck", deck.toString()).code == 1, "empty deck rejected");
            Files.write(deck, new byte[] {(byte) 0xC3, (byte) 0x28});
            check(run("", "--deck", deck.toString()).code == 1, "malformed UTF-8 rejected");
        } finally { Files.deleteIfExists(deck); }
        check(run("", "--deck", deck.toString()).code == 1, "missing file rejected");
        for (String[] bad : List.of(new String[] {}, new String[] {"--deck"},
                new String[] {"--deck", "x", "--limit", "0"},
                new String[] {"--deck", "x", "--limit", "abc"},
                new String[] {"--deck", "x", "--limit", "2147483648"},
                new String[] {"--deck", "x", "--seed", "4"},
                new String[] {"--deck", "x", "--shuffle", "--seed", "no"},
                new String[] {"--deck", "x", "--deck", "y"},
                new String[] {"--deck", "x", "--other"})) {
            check(run("", bad).code == 2, "invalid command-line options");
        }
        check(run("", "--help").code == 0 && run("", "--help").out.contains("Usage:"), "help");
        System.out.println("Passed " + checks + " checks.");
    }

    private record Result(int code, String out, String err) {}
    private static Result run(String answers, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Flashcards.run(args, new StringReader(answers),
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("Failed: " + name);
        checks++;
    }
}
