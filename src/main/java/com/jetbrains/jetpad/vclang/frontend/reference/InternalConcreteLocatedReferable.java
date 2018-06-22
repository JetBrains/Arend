package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InternalConcreteLocatedReferable extends ConcreteLocatedReferable implements Group.InternalReferable {
  private final boolean myVisible;

  public InternalConcreteLocatedReferable(Position position, @Nonnull String name, Precedence precedence, boolean isVisible, TCReferable parent) {
    super(position, name, precedence, parent, false);
    myVisible = isVisible;
  }

  @Override
  public LocatedReferable getReferable() {
    return this;
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }

  @Nullable
  @Override
  public TCReferable getUnderlyingReference() {
    Concrete.ReferableDefinition def = getDefinition();
    if (!(def instanceof Concrete.ClassFieldSynonym)) {
      return null;
    }
    Referable ref = ((Concrete.ClassFieldSynonym) def).getUnderlyingField().getReferent();
    return ref instanceof TCReferable ? (TCReferable) ref : null;
  }
}
