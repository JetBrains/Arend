package org.arend.core.expr;

import org.arend.core.definition.Definition;
import org.arend.core.sort.Sort;
import org.arend.util.Decision;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;

  public DefCallExpression(Definition definition) {
    if (definition.status() == Definition.TypeCheckingStatus.HEADER_HAS_ERRORS) {
      throw new IllegalStateException("Reference to a definition with a header error");
    }
    myDefinition = definition;
  }

  public List<? extends Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public abstract Sort getSortArgument();

  public Definition getDefinition() {
    return myDefinition;
  }

  public Integer getUseLevel() {
    for (Definition.ParametersLevel parametersLevel : myDefinition.getParametersLevels()) {
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
  public Decision isWHNF(boolean normalizing) {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
