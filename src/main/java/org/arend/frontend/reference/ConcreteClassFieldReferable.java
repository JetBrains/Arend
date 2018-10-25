package org.arend.frontend.reference;

import org.arend.frontend.parser.Position;
import org.arend.naming.reference.TCFieldReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.Precedence;

import javax.annotation.Nonnull;

public class ConcreteClassFieldReferable extends InternalConcreteLocatedReferable implements TCFieldReferable {
  private final boolean myExplicit;
  private final boolean myParameter;

  public ConcreteClassFieldReferable(Position position, @Nonnull String name, Precedence precedence, boolean isVisible, boolean isExplicit, boolean isParameter, TCReferable parent, Kind kind) {
    super(position, name, precedence, isVisible, parent, kind);
    myExplicit = isExplicit;
    myParameter = isParameter;
  }

  @Override
  public boolean isExplicitField() {
    return myExplicit;
  }

  @Override
  public boolean isParameterField() {
    return myParameter;
  }
}
