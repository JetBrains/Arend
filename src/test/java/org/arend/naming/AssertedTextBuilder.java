package org.arend.naming;

import org.arend.term.prettyprint.PlainTextBuilder;
import org.jetbrains.annotations.NotNull;

import static org.junit.Assert.assertTrue;

public class AssertedTextBuilder extends PlainTextBuilder {
  public AssertedTextBuilder(@NotNull StringBuilder builder) {
    super(builder);
  }

  @Override
  public @NotNull PlainTextBuilder openingBrace(char brace, long braceId) {
    assertTrue("{[(<".contains(String.valueOf(brace)));
    return super.openingBrace(brace, braceId);
  }

  @Override
  public @NotNull PlainTextBuilder closingBrace(char brace, long braceId) {
    assertTrue(">)]}".contains(String.valueOf(brace)));
    return super.closingBrace(brace, braceId);
  }
}
