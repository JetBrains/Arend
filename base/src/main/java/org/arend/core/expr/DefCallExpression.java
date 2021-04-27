package org.arend.core.expr;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.Definition;
import org.arend.core.definition.ParametersLevel;
import org.arend.core.definition.UniverseKind;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.expr.CoreDefCallExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression implements CoreDefCallExpression {
  private final Definition myDefinition;
  private LevelPair myLevels;

  public DefCallExpression(Definition definition, LevelPair levels) {
    myDefinition = definition;
    myLevels = levels;
  }

  @Override
  public @NotNull List<? extends Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public List<? extends Expression> getConCallArguments() {
    return getDefCallArguments();
  }

  @NotNull
  public LevelPair getLevels() {
    return myLevels;
  }

  @Override
  public @NotNull Level getPLevel() {
    return myLevels.get(LevelVariable.PVAR);
  }

  @Override
  public @NotNull Level getHLevel() {
    return myLevels.get(LevelVariable.HVAR);
  }

  public void substSort(LevelSubstitution substitution) {
    myLevels = myLevels.subst(substitution);
  }

  @Override
  public @NotNull Definition getDefinition() {
    return myDefinition;
  }

  public Integer getUseLevel() {
    for (ParametersLevel parametersLevel : myDefinition.getParametersLevels()) {
      if (parametersLevel.checkExpressionsTypes(getDefCallArguments())) {
        return parametersLevel.level;
      }
    }
    return null;
  }

  public UniverseKind getUniverseKind() {
    return myDefinition.getUniverseKind();
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
