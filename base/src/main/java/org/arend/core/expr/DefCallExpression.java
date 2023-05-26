package org.arend.core.expr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.CallableDefinition;
import org.arend.core.definition.ParametersLevel;
import org.arend.core.definition.UniverseKind;
import org.arend.ext.core.expr.CoreDefCallExpression;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression implements CoreDefCallExpression {
  private final CallableDefinition myDefinition;

  public DefCallExpression(CallableDefinition definition) {
    myDefinition = definition;
  }

  @Override
  public @NotNull List<Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public List<? extends Expression> getConCallArguments() {
    return getDefCallArguments();
  }

  @Override
  public @NotNull CallableDefinition getDefinition() {
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

  public void fixBoxes() {
    DependentLink param = myDefinition.getParameters();
    List<Expression> args = getDefCallArguments();
    for (int i = 0; i < args.size(); i++) {
      if (!param.hasNext()) break;
      if (param.isProperty()) {
        args.set(i, BoxExpression.make(args.get(i)));
      }
      param = param.getNext();
    }
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }

  @Override
  public @NotNull TypedExpression makeTypedExpression() {
    DependentLink param1 = myDefinition.getParameters();
    DependentLink param2 = param1.hasNext() ? param1.getNext() : param1;
    if (param2.hasNext() && param2.getTypeExpr() instanceof ReferenceExpression refExpr && refExpr.getBinding() == param1) {
      return new TypecheckingResult(getDefCallArguments().get(1), getDefCallArguments().get(0));
    } else {
      throw new IllegalStateException();
    }
  }
}
