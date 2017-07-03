package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;

import java.util.Set;

public class FindDefCallVisitor extends ProcessDefCallsVisitor<Void> {
  private Definition myFoundDefinition;
  private final Set<? extends Definition> myDefinitions;

  public FindDefCallVisitor(Set<? extends Definition> definitions) {
    myDefinitions = definitions;
  }

  public Definition getFoundDefinition() {
    return myFoundDefinition;
  }

  @Override
  protected boolean processDefCall(DefCallExpression expression, Void param) {
    if (myDefinitions.contains(expression.getDefinition())) {
      myFoundDefinition = expression.getDefinition();
      return true;
    } else {
      return false;
    }
  }
}
