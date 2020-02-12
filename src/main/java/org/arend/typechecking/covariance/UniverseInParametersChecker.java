package org.arend.typechecking.covariance;

import org.arend.core.definition.Definition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Sort;
import org.arend.typechecking.visitor.CheckForUniversesVisitor;

public class UniverseInParametersChecker extends CovarianceChecker {
  private final CheckForUniversesVisitor myVisitor = new CheckForUniversesVisitor();

  public boolean hasPLevel() {
    return myVisitor.hasPLevels();
  }

  public boolean hasHLevel() {
    return myVisitor.hasHLevels();
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
  protected boolean checkSort(Sort sort, Definition definition) {
    if (definition == null) {
      return false;
    }
    if (definition.getPLevelKind() == UniverseKind.WITH_UNIVERSES) {
      myVisitor.setHasPLevels();
    }
    if (definition.getHLevelKind() == UniverseKind.WITH_UNIVERSES) {
      myVisitor.setHasHLevels();
    }
    return myVisitor.hasPLevels() && myVisitor.hasHLevels();
  }
}
