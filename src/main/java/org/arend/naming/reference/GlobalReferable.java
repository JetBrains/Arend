package org.arend.naming.reference;

import org.arend.term.Precedence;

import javax.annotation.Nonnull;

public interface GlobalReferable extends TypedReferable {
  enum Kind {
    TYPECHECKABLE { @Override public boolean isTypecheckable() { return true; } },
    CONSTRUCTOR { @Override public boolean isConstructor() { return true; } },
    DEFINED_CONSTRUCTOR {
      @Override public boolean isTypecheckable() { return true; }
      @Override public boolean isConstructor() { return true; }
    },
    FIELD, OTHER;

    public boolean isTypecheckable() {
      return false;
    }

    public boolean isConstructor() {
      return false;
    }
  }

  @Nonnull Precedence getPrecedence();

  default GlobalReferable getTypecheckable() {
    return this;
  }

  default @Nonnull Kind getKind() {
    return Kind.TYPECHECKABLE;
  }
}
