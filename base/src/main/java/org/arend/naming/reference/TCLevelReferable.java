package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TCLevelReferable implements TCReferable, LevelReferable {
  private final Object myData;
  private final String myName;
  private final LevelDefReferable myParent;

  public TCLevelReferable(Object data, String name, LevelDefReferable parent) {
    myData = data;
    myName = name;
    myParent = parent;
  }

  @Override
  public @Nullable Object getData() {
    return myData;
  }

  @Override
  public @NotNull TCReferable getTypecheckable() {
    return myParent;
  }

  public LevelDefReferable getDefParent() {
    return myParent;
  }

  @Override
  public @NotNull Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }

  @Override
  public @NotNull Kind getKind() {
    return Kind.LEVEL;
  }

  @Override
  public @Nullable ModuleLocation getLocation() {
    return myParent.getLocation();
  }

  @Override
  public @Nullable LocatedReferable getLocatedReferableParent() {
    return myParent.getLocatedReferableParent();
  }

  @Override
  public @NotNull String textRepresentation() {
    return myName;
  }

  @Override
  public boolean isTypechecked() {
    return true;
  }
}
