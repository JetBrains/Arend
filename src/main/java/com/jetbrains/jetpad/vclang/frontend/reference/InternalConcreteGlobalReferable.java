package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class InternalConcreteGlobalReferable extends ConcreteGlobalReferable implements Group.InternalReferable {
  private final boolean myVisible;

  public InternalConcreteGlobalReferable(Position position, @Nonnull String name, Precedence precedence, boolean isVisible) {
    super(position, name, precedence);
    myVisible = isVisible;
  }

  @Override
  public GlobalReferable getReferable() {
    return this;
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }
}
