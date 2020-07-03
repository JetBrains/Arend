package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;
import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TCReferable extends LocatedReferable, DataContainer {
  TCReferable getTypecheckable();

  TCReferable NULL_REFERABLE = new TCReferable() {
    @Nullable
    @Override
    public Object getData() {
      return null;
    }

    @Override
    public TCReferable getTypecheckable() {
      return this;
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
