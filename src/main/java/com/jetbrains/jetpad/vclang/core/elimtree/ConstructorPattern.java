package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.ArrayList;
import java.util.List;

public class ConstructorPattern implements Pattern {
  private final ConCallExpression myConCall;
  private final Patterns myPatterns;

  public ConstructorPattern(ConCallExpression conCall, Patterns patterns) {
    myConCall = conCall;
    myPatterns = patterns;
  }

  public Constructor getConstructor() {
    return myConCall.getDefinition();
  }

  public List<Pattern> getArguments() {
    return myPatterns.getPatternList();
  }

  public Sort getSortArgument() {
    return myConCall.getSortArgument();
  }

  public List<? extends Expression> getDataTypeArguments() {
    return myConCall.getDataTypeArguments();
  }

  public ConCallExpression getConCall() {
    return myConCall;
  }

  @Override
  public ConCallExpression toExpression() {
    List<Expression> arguments = new ArrayList<>(myPatterns.getPatternList().size());
    for (Pattern pattern : myPatterns.getPatternList()) {
      Expression argument = pattern.toExpression();
      if (argument == null) {
        return null;
      }
      arguments.add(argument);
    }
    return new ConCallExpression(myConCall.getDefinition(), myConCall.getSortArgument(), myConCall.getDataTypeArguments(), arguments);
  }

  @Override
  public DependentLink getFirstBinding() {
    return myPatterns.getFirstBinding();
  }

  @Override
  public MatchResult match(Expression expression, List<Expression> result) {
    expression = expression.normalize(NormalizeVisitor.Mode.WHNF);
    ConCallExpression conCall = expression.toConCall();
    if (conCall == null) {
      return MatchResult.MAYBE;
    }
    if (conCall.getDefinition() != myConCall.getDefinition()) {
      return MatchResult.FAIL;
    }
    return myPatterns.match(conCall.getDefCallArguments(), result);
  }
}
