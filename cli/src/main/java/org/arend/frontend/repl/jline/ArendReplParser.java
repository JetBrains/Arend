package org.arend.frontend.repl.jline;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.arend.frontend.repl.CommonCliRepl;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.CompletingParsedLine;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.SyntaxError;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ArendReplParser implements Parser {
  public static final class ArendParsedLine implements CompletingParsedLine {
    private final int wordCursor;
    private final @NotNull List<@NotNull String> words;
    private final @NotNull String word;
    private final int wordIndex;
    private final @NotNull String line;
    private final int cursor;

    public ArendParsedLine(
      int wordCursor,
      @NotNull List<@NotNull String> words,
      @NotNull String word,
      int wordIndex,
      @NotNull String line,
      int cursor
    ) {
      this.wordCursor = wordCursor;
      this.words = words;
      this.word = word;
      this.wordIndex = wordIndex;
      this.line = line;
      this.cursor = cursor;
    }

    @Override
    public CharSequence escape(CharSequence charSequence, boolean b) {
      return charSequence;
    }

    @Override
    public int rawWordCursor() {
      return wordCursor;
    }

    @Override
    public int rawWordLength() {
      return word.length();
    }

    public int wordCursor() {
      return wordCursor;
    }

    public @NotNull List<@NotNull String> words() {
      return words;
    }

    @Override
    public @NotNull String word() {
      return word;
    }

    @Override
    public int wordIndex() {
      return wordIndex;
    }

    @Override
    public @NotNull String line() {
      return line;
    }

    @Override
    public int cursor() {
      return cursor;
    }
  }

  @Override
  public ParsedLine parse(String line, int cursor, ParseContext context) throws SyntaxError {
    if (line.isBlank()) return simplest(line, cursor, 0, Collections.emptyList());
    var tokensRaw = new CommonTokenStream(CommonCliRepl.createLexer(
      line, new BaseErrorListener()));
    tokensRaw.fill();
    var tokens = tokensRaw.getTokens().stream()
      // Drop the EOF
      .limit(tokensRaw.size() - 1)
      .filter(token -> token.getChannel() != Token.HIDDEN_CHANNEL)
      .collect(Collectors.toList());
    var wordOpt = tokens.stream().filter(token ->
      token.getStartIndex() <= cursor && token.getStopIndex() + 1 >= cursor
    ).findFirst();
    // In case we're in a whitespace or at the end
    if (wordOpt.isEmpty()) {
      var tokenOpt = tokens.stream().filter(tok -> tok.getStartIndex() >= cursor).findFirst();
      if (tokenOpt.isEmpty()) {
        return simplest(line, cursor, tokens.size(), tokens.stream().map(Token::getText).collect(Collectors.toList()));
      }
      var token = tokenOpt.get();
      var wordCursor = cursor - token.getStartIndex();
      return new ArendParsedLine(
        Math.max(wordCursor, 0), tokens.stream().map(Token::getText).collect(Collectors.toList()),
        token.getText(), tokens.size() - 1, line, cursor
      );
    }
    var word = wordOpt.get();
    var wordText = word.getText();
    return new ArendParsedLine(
      cursor - word.getStartIndex(),
      tokens.stream().map(Token::getText).collect(Collectors.toList()),
      wordText, tokens.indexOf(word), line, cursor
    );
  }

  @NotNull private ArendParsedLine simplest(String line, int cursor, int wordIndex, List<@NotNull String> tokens) {
    return new ArendParsedLine(0, tokens, "", wordIndex, line, cursor);
  }
}