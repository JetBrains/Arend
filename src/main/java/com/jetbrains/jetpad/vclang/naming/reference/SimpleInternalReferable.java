package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.group.Group;

public class SimpleInternalReferable implements Group.InternalReferable {
  private final GlobalReferable myReferable;
  private final boolean myVisible;

  public SimpleInternalReferable(GlobalReferable referable, boolean isVisible) {
    myReferable = referable;
    myVisible = isVisible;
  }

  @Override
  public GlobalReferable getReferable() {
    return myReferable;
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }
}
