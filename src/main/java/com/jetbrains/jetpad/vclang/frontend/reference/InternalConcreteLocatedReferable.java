package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.group.Group;

import javax.annotation.Nonnull;

public class InternalConcreteLocatedReferable extends ConcreteLocatedReferable implements Group.InternalReferable {
  private final boolean myVisible;

  public InternalConcreteLocatedReferable(Position position, @Nonnull String name, Precedence precedence, boolean isVisible, LocatedReferable parent) {
    super(position, name, precedence, parent, false);
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
