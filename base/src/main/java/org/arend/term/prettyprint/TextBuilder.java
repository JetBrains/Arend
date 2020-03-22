package org.arend.term.prettyprint;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;

public interface TextBuilder<Self extends TextBuilder<Self>> {
  Self appendWhitespaces(int number);
  Self appendUnderscore();
  Self appendEol();
  Self appendColon();
  Self appendComma();
  Self appendSemicolon();
  Self appendKeyword(@NotNull String keyword);
  Self appendGoal(@NotNull String goalText);
  Self appendReference(@NotNull Referable ref);

  default long allocBraceId() { return 0; }
  Self appendOpeningBrace(@NotNull String brace, long braceId);
  Self appendClosingBrace(@NotNull String brace, long braceId);

  /**
   * @param barId same as braceId, see {@link #allocBraceId()}
   */
  Self appendBar(long barId);

  default Self appendWhitespace() {
    return appendWhitespaces(1);
  }

  default Self appendError(@NotNull String errorText) {
    return appendGoal(errorText);
  }
}
