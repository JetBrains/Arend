package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LevelDefReferable implements TCReferable {
  private final LocatedReferable myParent;
  private final String myName;
  private final List<? extends LevelReferable> myReferables;
  private final boolean myPLevels;
  private boolean myIncreasing;

  public LevelDefReferable(boolean isPLevels, boolean isIncreasing, List<? extends LevelReferable> refs, LocatedReferable parent) {
    myParent = parent;
    StringBuilder name = new StringBuilder(isPLevels ? "(p)" : "(h)");
    for (Referable ref : refs) {
      name.append(ref.textRepresentation());
    }
    myName = name.toString();
    myReferables = refs;
    myPLevels = isPLevels;
    myIncreasing = isIncreasing;
  }

  public List<? extends LevelReferable> getReferables() {
    return myReferables;
  }

  public boolean isPLevels() {
    return myPLevels;
  }

  public boolean isIncreasing() {
    return myIncreasing;
  }

  public void setIsIncreasing(boolean isIncreasing) {
    myIncreasing = isIncreasing;
  }

  @Override
  public @Nullable Object getData() {
    return null;
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
  public @Nullable ModuleLocation getLocation() {
    return myParent == null ? null : myParent.getLocation();
  }

  @Override
  public @Nullable LocatedReferable getLocatedReferableParent() {
    return myParent;
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
