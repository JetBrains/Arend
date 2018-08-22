package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.TCFieldReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class ConcreteClassFieldReferable extends InternalConcreteLocatedReferable implements TCFieldReferable {
  private final boolean myExplicit;

  public ConcreteClassFieldReferable(Position position, @Nonnull String name, Precedence precedence, boolean isVisible, boolean isExplicit, TCReferable parent, Kind kind) {
    super(position, name, precedence, isVisible, parent, kind);
    myExplicit = isExplicit;
  }

  @Override
  public boolean isExplicitField() {
    return myExplicit;
  }
}
