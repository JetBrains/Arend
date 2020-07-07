package org.arend.naming.reference;

import org.arend.core.definition.Definition;
import org.arend.ext.reference.DataContainer;
import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TCReferable extends LocatedReferable, DataContainer {
  @NotNull TCReferable getTypecheckable();
  void setTypechecked(@Nullable Definition definition);
  Definition getTypechecked();

  default void setTypecheckedIfAbsent(@NotNull Definition definition) {
    if (getTypechecked() == null) {
      setTypechecked(definition);
    }
  }

  TCReferable NULL_REFERABLE = new TCReferable() {
    @Nullable
    @Override
    public Object getData() {
      return null;
    }

    @Override
    public @NotNull TCReferable getTypecheckable() {
      return this;
    }

    @Override
    public void setTypechecked(Definition definition) {}

    @Override
    public Definition getTypechecked() {
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
  };
}
