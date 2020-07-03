package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GlobalReferable extends TypedReferable {
  enum Kind {
    DATA, CLASS, FUNCTION,
    DEFINED_CONSTRUCTOR { @Override public boolean isConstructor() { return true; } },
    CONSTRUCTOR {
      @Override public boolean isTypecheckable() { return false; }
      @Override public boolean isConstructor() { return true; }
    },
    FIELD { @Override public boolean isTypecheckable() { return false; } },
    OTHER { @Override public boolean isTypecheckable() { return false; } };

    public boolean isTypecheckable() {
      return true;
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

  default @NotNull String getRepresentableName() {
    String alias = getAliasName();
    return alias != null ? alias : textRepresentation();
  }

  default @NotNull Precedence getRepresentablePrecedence() {
    return hasAlias() ? getAliasPrecedence() : getPrecedence();
  }

  default GlobalReferable getTypecheckable() {
    return this;
  }

  @NotNull Kind getKind();
}
