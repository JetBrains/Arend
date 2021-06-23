package org.arend.core.expr;

import org.arend.core.definition.Definition;
import org.arend.core.definition.ParametersLevel;
import org.arend.core.definition.UniverseKind;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.core.subst.Levels;
import org.arend.ext.core.expr.CoreDefCallExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression implements CoreDefCallExpression {
  private final Definition myDefinition;
  private Levels myLevels;

  public DefCallExpression(Definition definition, Levels levels) {
    assert (definition.getLevelParameters() == null) == (levels instanceof LevelPair);
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

  @Override
  @NotNull
  public Levels getLevels() {
    return myLevels;
  }

  public LevelSubstitution getLevelSubstitution() {
    return myLevels.makeSubstitution(getDefinition());
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
