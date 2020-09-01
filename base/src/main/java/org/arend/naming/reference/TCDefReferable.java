package org.arend.naming.reference;

import org.arend.core.definition.Definition;
import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TCDefReferable extends TCReferable {
  void setTypechecked(@Nullable Definition definition);
  Definition getTypechecked();

  default void setTypecheckedIfAbsent(@NotNull Definition definition) {
    if (getTypechecked() == null) {
      setTypechecked(definition);
    }
  }

  @Override
  default boolean isTypechecked() {
    Definition def = getTypechecked();
    return def != null && !def.status().needsTypeChecking();
  }

  @Override
  default @NotNull TCDefReferable getTypecheckable() {
    return this;
  }

  TCDefReferable NULL_REFERABLE = new TCDefReferable() {
    @Nullable
    @Override
    public Object getData() {
      return null;
    }

    @Override
    public @NotNull Kind getKind() {
      return Kind.OTHER;
    }

    @Nullable
    @Override
    public ModuleLocation getLocation() {
      return null;
    }

    @Nullable
    @Override
    public LocatedReferable getLocatedReferableParent() {
      return null;
    }

    @NotNull
    @Override
    public Precedence getPrecedence() {
      return Precedence.DEFAULT;
    }

    @NotNull
    @Override
    public String textRepresentation() {
      return "_";
    }

    @Override
    public void setTypechecked(@Nullable Definition definition) {}

    @Override
    public Definition getTypechecked() {
      return null;
    }
  };
}
