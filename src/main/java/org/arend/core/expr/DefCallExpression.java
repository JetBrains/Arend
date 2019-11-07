package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.definition.ParametersLevel;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.util.Decision;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;

  public DefCallExpression(Definition definition) {
    myDefinition = definition;
  }

  public List<? extends Expression> getDefCallArguments() {
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

  public abstract Sort getSortArgument();

  public Definition getDefinition() {
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

  public boolean hasUniverses() {
    return myDefinition.hasUniverses();
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
