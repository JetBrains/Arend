package org.arend.frontend.reference;

import org.arend.ext.reference.Precedence;
import org.arend.frontend.parser.Position;
import org.arend.naming.reference.TCFieldReferable;
import org.arend.naming.reference.TCReferable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConcreteClassFieldReferable extends InternalConcreteLocatedReferable implements TCFieldReferable {
  private final boolean myExplicit;
  private final boolean myParameter;

  public ConcreteClassFieldReferable(Position position, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, boolean isVisible, boolean isExplicit, boolean isParameter, TCReferable parent, Kind kind) {
    super(position, name, precedence, aliasName, aliasPrecedence, isVisible, parent, kind);
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
