package org.arend.naming.reference;

import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.ext.reference.Precedence;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.AccessModifier;
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

  default @NotNull AccessModifier getAccessModifier() {
    return AccessModifier.PUBLIC;
  }

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
    return switch (kind) {
      case FUNC_COCLAUSE, CLASS_COCLAUSE -> Kind.COCLAUSE_FUNCTION;
      case CONS -> Kind.DEFINED_CONSTRUCTOR;
      case INSTANCE -> Kind.INSTANCE;
      default -> Kind.FUNCTION;
    };
  }
}
