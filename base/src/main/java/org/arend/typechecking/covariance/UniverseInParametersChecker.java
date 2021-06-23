package org.arend.typechecking.covariance;

import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.*;
import org.arend.core.sort.Level;
import org.arend.core.subst.Levels;
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
  protected boolean checkLevels(Levels levels, DefCallExpression defCall) {
    return defCall != null && defCall.getUniverseKind() == UniverseKind.WITH_UNIVERSES;
  }
}
