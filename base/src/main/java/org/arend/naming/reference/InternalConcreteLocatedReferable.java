package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.term.group.Group;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InternalConcreteLocatedReferable extends ConcreteLocatedReferable implements Group.InternalReferable {
  private final boolean myVisible;

  public InternalConcreteLocatedReferable(Object data, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, boolean isVisible, TCDefReferable parent, Kind kind) {
    super(data, name, precedence, aliasName, aliasPrecedence, parent, kind);
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
