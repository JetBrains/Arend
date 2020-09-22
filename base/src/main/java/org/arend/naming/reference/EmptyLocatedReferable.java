package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmptyLocatedReferable implements LocatedReferable {
  private final String myName;
  private final LocatedReferable myParent;

  public EmptyLocatedReferable(String name, LocatedReferable parent) {
    myName = name;
    myParent = parent;
  }

  @Override
  public @NotNull Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }

  @Override
  public @NotNull Kind getKind() {
    return Kind.OTHER;
  }

  @Override
  public @NotNull String textRepresentation() {
    return myName;
  }

  @Override
  public @Nullable ModuleLocation getLocation() {
    return myParent == null ? null : myParent.getLocation();
  }

  @Override
  public @Nullable LocatedReferable getLocatedReferableParent() {
    return myParent;
  }
}
