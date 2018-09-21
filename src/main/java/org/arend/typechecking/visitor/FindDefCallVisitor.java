package org.arend.typechecking.visitor;

import org.arend.core.definition.Definition;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;

import java.util.Set;

public class FindDefCallVisitor extends ProcessDefCallsVisitor<Void> {
  private Definition myFoundDefinition;
  private final Set<? extends Definition> myDefinitions;

  private FindDefCallVisitor(Set<? extends Definition> definitions) {
    myDefinitions = definitions;
  }

  public static Definition findDefinition(Expression expression, Set<? extends Definition> definitions) {
    FindDefCallVisitor visitor = new FindDefCallVisitor(definitions);
    expression.accept(visitor, null);
    return visitor.myFoundDefinition;
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
