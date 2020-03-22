package org.arend.term.prettyprint;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;

public interface TextBuilder<Self extends TextBuilder<Self>> {
  default Self underscore() { return plainText('_'); }
  default Self eol() { return plainText(System.lineSeparator()); }
  default Self colon() { return plainText(':'); }
  default Self comma() { return plainText(','); }
  default Self semicolon() { return plainText(';'); }
  default Self integer(int i) { return plainText(String.valueOf(i)); }
  default Self keyword(@NotNull String keyword) { return plainText(keyword); }
  default Self goal(@NotNull String goalText) { return plainText(goalText); }
  default Self reference(@NotNull Referable ref) {
    return plainText(ref.textRepresentation());
  }

  default long allocBraceId() {
    return 0;
  }

  default Self openingBrace(@NotNull String brace, long braceId) {
    return plainText(brace);
  }
  default Self closingBrace(@NotNull String brace, long braceId) {
    return plainText(brace);
  }
  default Self openingBrace(char brace, long braceId) {
    return plainText(brace);
  }
  default Self closingBrace(char brace, long braceId) {
    return plainText(brace);
  }

  /**
   * @param barId same as braceId, see {@link #allocBraceId()}
   */
  default Self bar(long barId) {
    return plainText('|');
  }

  Self plainText(@NotNull String text);
  Self plainText(char c);
  Self whitespaces(int number);

  default Self whitespace() {
    return whitespaces(1);
  }

  default Self error(@NotNull String errorText) {
    return goal(errorText);
  }
}
