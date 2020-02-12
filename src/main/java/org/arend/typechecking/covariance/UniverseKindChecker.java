package org.arend.typechecking.covariance;

import org.arend.core.definition.Definition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.elimtree.*;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Sort;
import org.arend.ext.core.elimtree.CoreBranchKey;
import org.arend.typechecking.visitor.CheckForUniversesVisitor;
import org.arend.util.Pair;

import java.util.Map;

public class UniverseKindChecker extends CovarianceChecker {
  private final CheckForUniversesVisitor myVisitor = new CheckForUniversesVisitor();
  private UniverseKind myPLevelKind = UniverseKind.NO_UNIVERSES;
  private UniverseKind myHLevelKind = UniverseKind.NO_UNIVERSES;

  public UniverseKind getPLevelKind() {
    return myPLevelKind;
  }

  public UniverseKind getHLevelKind() {
    return myHLevelKind;
  }

  public boolean checkBody(Body body) {
    if (body instanceof Expression) {
      check((Expression) body);
    } else if (body instanceof IntervalElim) {
      if (checkBody(((IntervalElim) body).getOtherwise())) {
        return true;
      }
      for (Pair<Expression, Expression> pair : ((IntervalElim) body).getCases()) {
        if (pair.proj1 != null) {
          checkBody(pair.proj1);
        }
        if (pair.proj2 != null) {
          checkBody(pair.proj2);
        }
        if (myPLevelKind == UniverseKind.WITH_UNIVERSES && myHLevelKind == UniverseKind.WITH_UNIVERSES) {
          return true;
        }
      }
    } else if (body instanceof LeafElimTree) {
      check(((LeafElimTree) body).getExpression());
    } else if (body instanceof BranchElimTree) {
      for (Map.Entry<CoreBranchKey, ElimTree> entry : ((BranchElimTree) body).getChildren()) {
        if (checkBody(entry.getValue())) {
          return true;
        }
      }
    } else if (body != null) {
      throw new IllegalStateException();
    }

    return false;
  }

  @Override
  protected boolean checkNonCovariant(Expression expr) {
    if (expr.accept(myVisitor, null)) {
      if (myVisitor.hasPLevels()) {
        myPLevelKind = UniverseKind.WITH_UNIVERSES;
      }
      if (myVisitor.hasHLevels()) {
        myHLevelKind = UniverseKind.WITH_UNIVERSES;
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected boolean checkSort(Sort sort, Definition definition) {
    if (!myVisitor.visitSort(sort)) {
      return false;
    }

    myPLevelKind = myPLevelKind.max(definition == null ? UniverseKind.ONLY_COVARIANT : definition.getPLevelKind());
    myHLevelKind = myHLevelKind.max(definition == null ? UniverseKind.ONLY_COVARIANT : definition.getHLevelKind());
    return myPLevelKind == UniverseKind.WITH_UNIVERSES && myHLevelKind == UniverseKind.WITH_UNIVERSES;
  }
}
