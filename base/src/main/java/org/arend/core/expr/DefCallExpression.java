package org.arend.core.expr;

import org.arend.core.definition.Definition;
import org.arend.core.definition.ParametersLevel;
import org.arend.core.definition.UniverseKind;
import org.arend.ext.core.expr.CoreDefCallExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression implements CoreDefCallExpression {
  private final Definition myDefinition;

  public DefCallExpression(Definition definition) {
    myDefinition = definition;
  }

  @Override
  public @NotNull List<? extends Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public List<? extends Expression> getConCallArguments() {
    return getDefCallArguments();
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
