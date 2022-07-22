package org.arend.naming.reference;

import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.ext.reference.Precedence;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GlobalReferable extends Referable {
  enum Kind {
    DATA, CLASS, FUNCTION, COCLAUSE_FUNCTION, INSTANCE,
    DEFINED_CONSTRUCTOR { @Override public boolean isConstructor() { return true; } },
    CONSTRUCTOR {
      @Override public boolean isTypecheckable() { return false; }
      @Override public boolean isConstructor() { return true; }
    },
    FIELD { @Override public boolean isTypecheckable() { return false; } },
    LEVEL { @Override public boolean isTypecheckable() { return false; } },
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

  @NotNull Kind getKind();

  default Concrete.ResolvableDefinition getDefaultConcrete() {
    return null;
  }

  @Override
  default boolean isLocalRef() {
    return false;
  }

  static Kind kindFromFunction(FunctionKind kind) {
    switch (kind) {
      case FUNC_COCLAUSE: case CLASS_COCLAUSE: return Kind.COCLAUSE_FUNCTION;
      case CONS: return Kind.DEFINED_CONSTRUCTOR;
      case INSTANCE: return Kind.INSTANCE;
      default: return Kind.FUNCTION;
    }
  }
}
