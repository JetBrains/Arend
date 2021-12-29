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
    return !myAllowedDefinition.contains(expr.getDefinition()) && checkLevels(expr.getLevels()) ? CoreExpression.FindAction.STOP : CoreExpression.FindAction.CONTINUE;
  }

  @Override
  public Boolean visitUniverse(UniverseExpression expr, Void param) {
    return checkSort(expr.getSort());
  }

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
