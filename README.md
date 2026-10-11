# Java Flashcards

A terminal flashcard quiz in Java. It reads a deck from a text file, checks answers, and lists missed cards at the end. The sample deck has five computing questions.

## Running it

Use JDK 17 or newer. From the project folder:

```sh
mkdir -p build
javac --release 17 -encoding UTF-8 -d build src/*.java
java -cp build Flashcards --deck decks/computing.tsv
```

To try just the first three cards:

```sh
java -cp build Flashcards --deck decks/computing.tsv --limit 3
```

`--shuffle` changes the order. Add `--seed 42` for a repeatable shuffle; a seed requires `--shuffle`. The limit must be a positive integer. Use `--help` for the full command.

## Making a deck

Each line is a question and answer separated by **one literal tab**. Save the file as UTF-8. There is no header row; blank lines and lines starting with `#` are skipped.

```text
# Example deck
Capital of France?	Paris
2 + 2?	4
```

The two fields cannot contain tabs or newlines, and neither can be empty. Copy [computing.tsv](decks/computing.tsv) if your editor makes tabs hard to see.

Answers ignore case, surrounding whitespace, and equivalent Unicode accents. Spelling, punctuation, and internal spacing must match. A blank answer counts as a miss. Type `:quit` to stop; that text is reserved and cannot be a card answer.

For example, missing the second question in a three-card sample session gives:

```text
Score: 2/3 attempted (67%).
Unanswered: 0
Review missed cards:
- Which SQL clause groups rows before aggregation? -> GROUP BY
```

## Code and tests

`src/Flashcards.java` keeps deck parsing, card selection, and the quiz loop separate. The test harness supplies answers through a `StringReader`, so it can check complete sessions without typing into a terminal.

```sh
javac --release 17 -encoding UTF-8 -d build src/*.java test/*.java
java -cp build FlashcardsTest
```

Tests cover malformed decks and options, Unicode answers, seeded shuffling, scores, missed-card review, and early quit/end-of-input. CI runs them on Java 17 and 21.

## Limits

This is for small decks with short, exact answers. The whole deck stays in memory; progress is not saved between runs. Scores include only attempted cards when a session stops early.

Exit codes: `0` for a finished/stopped session or help, `1` for deck/input errors, and `2` for invalid options.

[MIT license](LICENSE)
