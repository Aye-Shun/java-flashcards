# Java Flashcards

A terminal quiz that reads question/answer pairs from a file and shows the missed cards afterward.

## Compile and run

JDK 17 or newer:

```sh
mkdir -p build
javac --release 17 -encoding UTF-8 -d build src/*.java
java -cp build Flashcards --deck decks/computing.tsv
```

For three shuffled cards, add `--limit 3 --shuffle --seed 42`. The seed makes the shuffle repeatable and requires `--shuffle`.

## Decks

Save as UTF-8, with one literal tab between the question and answer. No header. Blank lines and lines starting with `#` are ignored.

```text
Capital of France?	Paris
2 + 2?	4
```

Each field must be nonempty and cannot contain tabs or newlines. The supplied [computing.tsv](decks/computing.tsv) is another example.

Answers ignore case and surrounding whitespace; spelling, punctuation, and internal spacing must match. Type `:quit` to stop. Only attempted cards count in the score, and progress isn't saved between runs.

Parsing, card selection, and the quiz loop are in `src/Flashcards.java`. The whole deck is loaded into memory, so this is meant for small decks.

Test harness:

```sh
javac --release 17 -encoding UTF-8 -d build src/*.java test/*.java
java -cp build FlashcardsTest
```

[MIT](LICENSE)
