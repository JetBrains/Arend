package org.arend.typechecking.visitor;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.Levels;
import org.arend.ext.core.expr.CoreExpression;

import java.util.Set;

public class FindLevelParameters extends SearchVisitor<Void> {
  private final Set<? extends Definition> myAllowedDefinition;
  public boolean hasPLevels;
  public boolean hasHLevels;

  public FindLevelParameters(Set<? extends Definition> allowedDefinition) {
    myAllowedDefinition = allowedDefinition;
  }

  private void checkLevel(Level level) {
    if (level.getVar() != null && level.getVar().getType() == LevelVariable.LvlType.PLVL) {
      hasPLevels = true;
    }
    if (level.getVar() != null && level.getVar().getType() == LevelVariable.LvlType.HLVL) {
      hasHLevels = true;
    }
  }

  private boolean checkLevels(Levels levels) {
    for (Level level : levels.toList()) {
      checkLevel(level);
      if (hasPLevels && hasHLevels) {
        return true;
      }
    }
    return false;
  }

  private boolean checkSort(Sort sort) {
    checkLevel(sort.getPLevel());
    checkLevel(sort.getHLevel());
    return hasPLevels && hasHLevels;
  }

  @Override
  public CoreExpression.FindAction processDefCall(DefCallExpression expr, Void param) {
    return expr instanceof LeveledDefCallExpression && !myAllowedDefinition.contains(expr.getDefinition()) && checkLevels(((LeveledDefCallExpression) expr).getLevels()) ? CoreExpression.FindAction.STOP : CoreExpression.FindAction.CONTINUE;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr, Void param) {
    return checkSort(expr.getSort());
  }

  /*
  @Override
  public Boolean visitLam(LamExpression expression, Void param) {
    if (super.visitLam(expression, param)) {
      return true;
    }
    boolean oldHasPLevels = hasPLevels;
    boolean oldHasHLevels = hasHLevels;
    if (!checkSort(expression.getResultSort())) {
      return false;
    }
    Sort sort = expression.getBody().getType().getSortOfType();
    if (sort == null) return true;
    hasPLevels = oldHasPLevels;
    hasHLevels = oldHasHLevels;
    expression.setResultSort(expression.getParameters().getType().getSortOfType().max(sort));
    return checkSort(expression.getResultSort());
  }

  @Override
  public Boolean visitPi(PiExpression expression, Void param) {
    if (super.visitPi(expression, param)) {
      return true;
    }
    boolean oldHasPLevels = hasPLevels;
    boolean oldHasHLevels = hasHLevels;
    if (!checkSort(expression.getResultSort())) {
      return false;
    }
    Sort sort = expression.getCodomain().getSortOfType();
    if (sort == null) return true;
    hasPLevels = oldHasPLevels;
    hasHLevels = oldHasHLevels;
    expression.setResultSort(expression.getParameters().getType().getSortOfType().max(sort));
    return checkSort(expression.getResultSort());
  }

  @Override
  public Boolean visitSigma(SigmaExpression expression, Void ignored) {
    if (super.visitSigma(expression, null)) {
      return true;
    }
    boolean oldHasPLevels = hasPLevels;
    boolean oldHasHLevels = hasHLevels;
    if (!checkSort(expression.getSort())) {
      return false;
    }
    hasPLevels = oldHasPLevels;
    hasHLevels = oldHasHLevels;
    Sort sort = Sort.PROP;
    for (DependentLink param = expression.getParameters(); param.hasNext(); param = param.getNext()) {
      param = param.getNextTyped(null);
      sort = sort.max(param.getType().getSortOfType());
    }
    expression.setSort(sort);
    return checkSort(expression.getSort());
  }

  @Override
  public Boolean visitArray(ArrayExpression expr, Void params) {
    if (super.visitArray(expr, params)) {
      return true;
    }
    boolean oldHasPLevels = hasPLevels;
    boolean oldHasHLevels = hasHLevels;
    if (!checkLevels(expr.getLevels())) {
      return false;
    }
    Sort sort = AppExpression.make(expr.getElementsType(), new ReferenceExpression(new TypedDependentLink(true, "j", Fin(expr.getLength()), EmptyDependentLink.getInstance())), true).getSortOfType();
    if (sort == null) return true;
    hasPLevels = oldHasPLevels;
    hasHLevels = oldHasHLevels;
    expr.setLevels(new LevelPair(sort.getPLevel(), sort.getHLevel()));
    return checkLevels(expr.getLevels());
  }
  */

  @Override
  public Boolean visitData(DataDefinition def, Void params) {
    return checkSort(def.getSort()) || super.visitData(def, params);
  }

  @Override
  public Boolean visitClass(ClassDefinition def, Void params) {
    for (ClassDefinition superClass : def.getSuperClasses()) {
      Levels levels = def.getSuperLevels().get(superClass);
      if (levels == null) {
        if (superClass.getLevelParameters() != null && superClass.getLevelParameters().isEmpty()) {
          continue;
        } else {
          hasPLevels = hasHLevels = true;
          return true;
        }
      }
      if (checkLevels(levels)) return true;
    }
    return super.visitClass(def, params);
  }
}
