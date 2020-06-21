package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.definition.ParametersLevel;
import org.arend.core.definition.UniverseKind;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.expr.CoreDefCallExpression;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression implements CoreDefCallExpression {
  private final Definition myDefinition;
  private Sort mySortArgument;

  public DefCallExpression(Definition definition, Sort sortArgument) {
    myDefinition = definition;
    mySortArgument = sortArgument;
  }

  @Override
  public @NotNull List<? extends Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public List<? extends Expression> getConCallArguments() {
    return getDefCallArguments();
  }

  public ExprSubstitution addArguments(ExprSubstitution substitution) {
    DependentLink link = myDefinition.getParameters();
    for (Expression argument : getDefCallArguments()) {
      substitution.add(link, argument);
      link = link.getNext();
    }
    return substitution;
  }

  @NotNull
  public Sort getSortArgument() {
    return mySortArgument;
  }

  public void substSort(LevelSubstitution substitution) {
    mySortArgument = mySortArgument.subst(substitution);
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
