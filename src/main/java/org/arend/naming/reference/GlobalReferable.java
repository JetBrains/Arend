package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;

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

  @NotNull Precedence getPrecedence();

  default GlobalReferable getTypecheckable() {
    return this;
  }

  default @NotNull Kind getKind() {
    return Kind.TYPECHECKABLE;
  }
}
