# Java Flashcards

A dependency-free terminal quiz for small text flashcard decks. Load your own questions, answer one at a time, and review the cards you missed at the end.

This project practices Java file I/O, records, collections, option parsing, and testable command-line programs. It is a simple quiz tool; it does not implement spaced repetition.

## Run

Install **JDK 17 or newer** and check that both `java` and `javac` are available. From this directory:

```sh
mkdir -p build
javac --release 17 -encoding UTF-8 -d build src/*.java
java -cp build Flashcards --deck decks/computing.tsv
```

For a shorter, repeatable practice session:

```sh
java -cp build Flashcards --deck decks/computing.tsv --limit 3 --shuffle --seed 42
```

## Deck format

Save a **UTF-8 `.tsv` file** with one question and answer per line, separated by a **literal tab**, not the two characters `\t`. There is no header row. Blank lines and lines starting with `#` are ignored.

```text
# A small deck
Capital of France?    Paris
2 + 2?                4
```

Replace the spacing between each question and answer above with one tab, or copy the supplied [sample deck](decks/computing.tsv). Leading and trailing field whitespace is stripped. Each row must have exactly two nonempty fields; tabs or newlines inside a field are unsupported. A leading UTF-8 byte-order mark is accepted.

## Options and scoring

| Option | Behavior |
| --- | --- |
| `--deck FILE` | Required path to your deck; quote paths containing spaces |
| `--limit N` | Positive integer; use at most N cards, defaulting to the whole deck |
| `--shuffle` | Randomize the cards before applying the limit |
| `--seed N` | Reproducible shuffle with a signed 64-bit integer; requires `--shuffle` |
| `--help` / `-h` | Print usage |

Answers ignore case, surrounding whitespace, and equivalent composed/decomposed Unicode accents. Punctuation, spelling, and internal spacing must match. A blank answer counts as a miss.

Type `:quit`, or send end-of-input, to stop. The score includes only attempted cards, and the summary lists unanswered cards separately. Missed answers are revealed immediately and listed again for review. `:quit` is reserved, so it cannot be used as a card answer.

Example from a three-card session:

```text
Score: 2/3 attempted (67%).
Unanswered: 0
Review missed cards:
- 2 + 2? -> 4
```

Exit codes are `0` for a completed/stopped session or help, `1` for deck/input errors, and `2` for invalid options.

## Test

```sh
mkdir -p build
javac --release 17 -encoding UTF-8 -d build src/*.java test/*.java
java -cp build FlashcardsTest
```

The plain Java harness tests deck parsing, invalid rows, UTF-8 errors, Unicode comparison, seeded selection, real quiz scoring, missed-card review, quit/EOF behavior, and invalid options. GitHub Actions runs it on Java 17 and 21.

## Scope

Decks are loaded into memory. No account, network service, saved progress, multiple-choice parser, or scheduling system is involved. The tool is intended for small decks with short, exact answers. Terminal Unicode display depends on your terminal font and encoding.

## License

[MIT](LICENSE)
