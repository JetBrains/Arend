package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  default boolean hasAlias() {
    return getAliasName() != null;
  }

  default @Nullable String getAliasName() {
    return null;
  }

  default @NotNull Precedence getAliasPrecedence() {
    return Precedence.DEFAULT;
  }

  default @NotNull Precedence getRepresentablePrecedence() {
    return hasAlias() ? getAliasPrecedence() : getPrecedence();
  }

  default GlobalReferable getTypecheckable() {
    return this;
  }

  default @NotNull Kind getKind() {
    return Kind.TYPECHECKABLE;
  }
}
