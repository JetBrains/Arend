package org.arend.naming.reference;

import org.arend.term.Precedence;

import javax.annotation.Nonnull;

public interface GlobalReferable extends TypedReferable {
  enum Kind { TYPECHECKABLE, CONSTRUCTOR, FIELD, OTHER }

  @Nonnull Precedence getPrecedence();

  default GlobalReferable getTypecheckable() {
    return this;
  }

  default @Nonnull Kind getKind() {
    return Kind.TYPECHECKABLE;
  }
}
