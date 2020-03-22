package org.arend.term.prettyprint;

import org.jetbrains.annotations.NotNull;

public class PlainTextBuilder implements TextBuilder<@NotNull PlainTextBuilder> {
  private final @NotNull StringBuilder builder;

  public PlainTextBuilder() {
    this(new StringBuilder());
  }

  public PlainTextBuilder(@NotNull StringBuilder builder) {
    this.builder = builder;
  }

  @Override
  public @NotNull PlainTextBuilder plainText(@NotNull String text) {
    builder.append(text);
    return this;
  }

  @Override
  public @NotNull PlainTextBuilder plainText(char c) {
    builder.append(c);
    return this;
  }

  @Override
  public @NotNull PlainTextBuilder whitespaces(int number) {
    for (int i = 0; i < number; i++) builder.append(' ');
    return this;
  }

  @Override
  public @NotNull PlainTextBuilder whitespace() {
    return plainText(' ');
  }
}
