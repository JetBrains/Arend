package org.arend.typechecking.visitor;

import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Sort;

public class CheckForUniversesVisitor extends SearchVisitor<Void> {
  public static boolean visitSort(Sort sort) {
    return !sort.getPLevel().isClosed() || !sort.getHLevel().isClosed();
  }

  @Override
  public boolean processDefCall(DefCallExpression expression, Void param) {
    if (expression.getDefinition() instanceof ClassField) {
      return false;
    }
    return expression.getUniverseKind() != UniverseKind.NO_UNIVERSES && visitSort(expression.getSortArgument());
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    return visitSort(expression.getSort());
  }
}
