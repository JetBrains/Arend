package org.arend.term.prettyprint;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;

public class PlainTextBuilder implements TextBuilder<@NotNull PlainTextBuilder> {
  private final @NotNull StringBuilder builder;

  public PlainTextBuilder() {
    this(new StringBuilder());
  }

  public PlainTextBuilder(@NotNull StringBuilder builder) {
    this.builder = builder;
  }

  private @NotNull PlainTextBuilder append(String s) {
    builder.append(s);
    return this;
  }

  private @NotNull PlainTextBuilder append(char c) {
    builder.append(c);
    return this;
  }

  @Override
  public @NotNull PlainTextBuilder appendWhitespaces(int number) {
    for (int i = 0; i < number; i++) builder.append(' ');
    return this;
  }

  @Override
  public @NotNull PlainTextBuilder appendUnderscore() {
    return append('_');
  }

  @Override
  public @NotNull PlainTextBuilder appendEol() {
    return append(System.lineSeparator());
  }

  @Override
  public @NotNull PlainTextBuilder appendColon() {
    return append(':');
  }

  @Override
  public @NotNull PlainTextBuilder appendComma() {
    return append(',');
  }

  @Override
  public @NotNull PlainTextBuilder appendSemicolon() {
    return append(';');
  }

  @Override
  public @NotNull PlainTextBuilder appendKeyword(@NotNull String keyword) {
    return append(keyword);
  }

  @Override
  public @NotNull PlainTextBuilder appendGoal(@NotNull String goalText) {
    return append(goalText);
  }

  @Override
  public @NotNull PlainTextBuilder appendReference(@NotNull Referable ref) {
    return append(ref.textRepresentation());
  }

  @Override
  public @NotNull PlainTextBuilder appendOpeningBrace(@NotNull String brace, long braceId) {
    return append(brace);
  }

  @Override
  public @NotNull PlainTextBuilder appendClosingBrace(@NotNull String brace, long braceId) {
    return append(brace);
  }

  @Override
  public @NotNull PlainTextBuilder appendBar(long barId) {
    return append('|');
  }
}
