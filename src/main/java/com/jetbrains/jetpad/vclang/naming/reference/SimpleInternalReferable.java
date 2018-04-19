package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.group.Group;

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
