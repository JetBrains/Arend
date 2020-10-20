package org.arend.typechecking.covariance;

import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Sort;
import org.arend.typechecking.visitor.CheckForUniversesVisitor;

public class UniverseInParametersChecker extends CovarianceChecker {
  private final CheckForUniversesVisitor myVisitor = new CheckForUniversesVisitor();

  @Override
  protected boolean allowData() {
    return false;
  }

  @Override
  protected boolean checkNonCovariant(Expression expr) {
    return expr.accept(myVisitor, null);
  }

  @Override
  protected boolean checkSort(Sort sort, DefCallExpression defCall) {
    return defCall != null && defCall.getUniverseKind() == UniverseKind.WITH_UNIVERSES;
  }
}
