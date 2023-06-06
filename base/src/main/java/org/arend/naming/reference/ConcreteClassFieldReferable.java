package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.term.group.AccessModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConcreteClassFieldReferable extends InternalConcreteLocatedReferable implements TCFieldReferable {
  private final boolean myExplicit;
  private final boolean myParameter;

  public ConcreteClassFieldReferable(Object data, AccessModifier accessModifier, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, boolean isVisible, boolean isExplicit, boolean isParameter, TCDefReferable parent) {
    super(data, accessModifier, name, precedence, aliasName, aliasPrecedence, isVisible, parent, Kind.FIELD);
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
