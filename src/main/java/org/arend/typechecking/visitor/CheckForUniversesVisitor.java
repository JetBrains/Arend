package org.arend.typechecking.visitor;

import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Sort;

public class CheckForUniversesVisitor extends ProcessDefCallsVisitor<Void> {
  private boolean myHasPLevels = false;
  private boolean myHasHLevels = false;

  public boolean hasPLevels() {
    return myHasPLevels;
  }

  public boolean hasHLevels() {
    return myHasHLevels;
  }

  public void setHasPLevels() {
    myHasPLevels = true;
  }

  public void setHasHLevels() {
    myHasHLevels = true;
  }

  public boolean visitSort(Sort sort) {
    if (!sort.getPLevel().isClosed()) {
      myHasPLevels = true;
    }
    if (!sort.getHLevel().isClosed()) {
      myHasHLevels = true;
    }
    return myHasPLevels && myHasHLevels;
  }

  @Override
  public boolean processDefCall(DefCallExpression expression, Void param) {
    if (expression.getDefinition() instanceof ClassField) {
      return false;
    }
    if (expression.getPLevelKind() != UniverseKind.NO_UNIVERSES && !expression.getSortArgument().getPLevel().isClosed()) {
      myHasPLevels = true;
    }
    if (expression.getHLevelKind() != UniverseKind.NO_UNIVERSES && !expression.getSortArgument().getHLevel().isClosed()) {
      myHasHLevels = true;
    }
    return myHasPLevels && myHasHLevels;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expression, Void param) {
    return visitSort(expression.getSort());
  }
}
