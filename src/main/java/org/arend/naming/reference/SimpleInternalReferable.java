package org.arend.naming.reference;

import org.arend.term.group.Group;

public class SimpleInternalReferable implements Group.InternalReferable {
  private final LocatedReferable myReferable;
  private final boolean myVisible;

  public SimpleInternalReferable(LocatedReferable referable, boolean isVisible) {
    myReferable = referable;
    myVisible = isVisible;
  }

  @Override
  public LocatedReferable getReferable() {
    return myReferable;
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }
}
