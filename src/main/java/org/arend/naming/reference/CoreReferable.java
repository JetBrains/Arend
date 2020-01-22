package org.arend.naming.reference;

import org.arend.ext.typechecking.CheckedExpression;

import javax.annotation.Nonnull;

public class CoreReferable implements Referable {
  public final CheckedExpression expression;

  public CoreReferable(CheckedExpression expression) {
    this.expression = expression;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return "unnamedCoreExpression";
  }
}
