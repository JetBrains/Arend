package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.ArrayList;
import java.util.List;

public class ConstructorPattern implements Pattern {
  private final ConCallExpression myConCall;
  private final List<Pattern> myPatterns;

  public ConstructorPattern(ConCallExpression conCall, List<Pattern> patterns) {
    myConCall = conCall;
    myPatterns = patterns;
  }

  public Constructor getConstructor() {
    return myConCall.getDefinition();
  }

  public List<Pattern> getPatterns() {
    return myPatterns;
  }

  public Sort getSortArgument() {
    return myConCall.getSortArgument();
  }

  public List<? extends Expression> getDataTypeArguments() {
    return myConCall.getDataTypeArguments();
  }

  @Override
  public ConCallExpression toExpression() {
    List<Expression> arguments = new ArrayList<>(myPatterns.size());
    for (Pattern pattern : myPatterns) {
      Expression argument = pattern.toExpression();
      if (argument == null) {
        return null;
      }
      arguments.add(argument);
    }
    return new ConCallExpression(myConCall.getDefinition(), myConCall.getSortArgument(), myConCall.getDataTypeArguments(), arguments);
  }
}
