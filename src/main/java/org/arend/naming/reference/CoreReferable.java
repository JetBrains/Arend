package org.arend.naming.reference;

import org.arend.ext.typechecking.CheckedExpression;

import javax.annotation.Nonnull;

public class CoreReferable implements Referable {
  private final String myName;
  public final CheckedExpression expression;

  public CoreReferable(String name, CheckedExpression expression) {
    myName = name;
    this.expression = expression;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myName;
  }
}
