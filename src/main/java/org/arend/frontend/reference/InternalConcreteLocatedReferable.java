package org.arend.frontend.reference;

import org.arend.frontend.parser.Position;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.Precedence;
import org.arend.term.group.Group;

import javax.annotation.Nonnull;

public class InternalConcreteLocatedReferable extends ConcreteLocatedReferable implements Group.InternalReferable {
  private final boolean myVisible;

  public InternalConcreteLocatedReferable(Position position, @Nonnull String name, Precedence precedence, boolean isVisible, TCReferable parent, Kind kind) {
    super(position, name, precedence, parent, kind);
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
}
