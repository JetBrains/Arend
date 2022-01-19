package org.arend.typechecking.covariance;

import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.*;
import org.arend.core.sort.Level;
import org.arend.core.subst.Levels;
import org.arend.typechecking.visitor.CheckForUniversesVisitor;

public class UniverseInParametersChecker extends CovarianceChecker {
  private final CheckForUniversesVisitor myVisitor = new CheckForUniversesVisitor();
  private UniverseKind myResult = UniverseKind.NO_UNIVERSES;
  private boolean myOmega;

  public UniverseKind getUniverseKind(Expression expression) {
    myOmega = false;
    myResult = UniverseKind.NO_UNIVERSES;
    return check(expression) ? UniverseKind.WITH_UNIVERSES : myResult;
  }

  public boolean isOmega() {
    return myOmega;
  }

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
    if (levels.isClosed()) {
      return false;
    }

    if (defCall != null && defCall.getUniverseKind() == UniverseKind.WITH_UNIVERSES) {
      return true;
    }

    if ((defCall == null || defCall.getUniverseKind() == UniverseKind.ONLY_COVARIANT) && myResult == UniverseKind.NO_UNIVERSES) {
      boolean ok = true;
      for (Level level : levels.toList()) {
        if (level.getVar() != null && level.getConstant() > 0) {
          ok = false;
          break;
        }
      }
      if (ok) {
        myOmega = true;
      } else {
        myResult = UniverseKind.ONLY_COVARIANT;
      }
    }

    return false;
  }
}
