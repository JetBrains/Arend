package org.arend.naming.reference;

import org.arend.ext.typechecking.CheckedExpression;
import org.jetbrains.annotations.NotNull;

public class CoreReferable implements Referable {
  private final String myName;
  public final CheckedExpression expression;

  public CoreReferable(String name, CheckedExpression expression) {
    myName = name;
    this.expression = expression;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myName;
  }
}
