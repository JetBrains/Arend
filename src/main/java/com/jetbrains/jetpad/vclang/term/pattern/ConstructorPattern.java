package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.PatternMatchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstructorPattern extends Pattern implements Abstract.ConstructorPattern {
  private final Constructor myConstructor;
  private final List<Pattern> myArguments;

  public ConstructorPattern(Constructor constructor, List<Pattern> arguments, boolean isExplicit) {
    super(isExplicit);
    myConstructor = constructor;
    myArguments = arguments;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }
  @Override
  public Utils.Name getConstructorName() {
    return myConstructor.getName();
  }

  @Override
  public List<Pattern> getArguments() {
    return myArguments;
  }

  @Override
  public PatternMatchResult match(Expression expr, List<Binding> context) {
    List<Expression> constructorArgs = new ArrayList<>();
    expr = expr.normalize(NormalizeVisitor.Mode.WHNF, context).getFunction(constructorArgs);
    Collections.reverse(constructorArgs);
    if (!(expr instanceof DefCallExpression && ((DefCallExpression) expr).getDefinition() instanceof Constructor) || ((DefCallExpression) expr).getDefinition() != myConstructor) {
      return new PatternMatchResult(this, expr);
    }
    if (constructorArgs.size() != myArguments.size()) {
      return new PatternMatchResult(this, expr);
    }
    List<Expression> result = new ArrayList<>();
    for (int i  = 0; i < constructorArgs.size(); i++) {
      PatternMatchResult subMatch = myArguments.get(i).match(constructorArgs.get(i), context);
      if (subMatch.expressions == null)
        return subMatch;
      result.addAll(subMatch.expressions);
    }
    return new PatternMatchResult(result);
  }
}
