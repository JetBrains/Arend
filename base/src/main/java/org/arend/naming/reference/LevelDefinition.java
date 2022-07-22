package org.arend.naming.reference;

import java.util.List;

public class LevelDefinition {
  private final List<? extends TCLevelReferable> myReferables;
  private final boolean myPLevels;
  private boolean myIncreasing;
  private final LocatedReferable myParent;

  public LevelDefinition(boolean isPLevels, boolean isIncreasing, List<? extends TCLevelReferable> refs, LocatedReferable parent) {
    myReferables = refs;
    myPLevels = isPLevels;
    myIncreasing = isIncreasing;
    myParent = parent;
  }

  public List<? extends TCLevelReferable> getReferables() {
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

  public LocatedReferable getParent() {
    return myParent;
  }
}
