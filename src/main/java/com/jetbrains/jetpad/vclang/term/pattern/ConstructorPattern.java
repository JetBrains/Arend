package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.PatternMatchFailedResult;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.PatternMatchMaybeResult;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.PatternMatchOKResult;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.PatternMatchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstructorPattern extends Pattern implements Abstract.ConstructorPattern {
  private final Constructor myConstructor;
  private final List<PatternArgument> myArguments;

  public ConstructorPattern(Constructor constructor, List<PatternArgument> arguments) {
    myConstructor = constructor;
    myArguments = arguments;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }
  @Override
  public Name getConstructorName() {
    return myConstructor.getName();
  }

  @Override
  public List<PatternArgument> getArguments() {
    return myArguments;
  }

  @Override
  public PatternMatchResult match(Expression expr, List<Binding> context) {
    List<Expression> constructorArgs = new ArrayList<>();
    expr = (context == null ? expr : expr.normalize(NormalizeVisitor.Mode.WHNF, context)).getFunction(constructorArgs);
    Collections.reverse(constructorArgs);
    if (!(expr instanceof DefCallExpression && ((DefCallExpression) expr).getDefinition() instanceof Constructor)) {
      return new PatternMatchMaybeResult(this, expr);
    }
    if (((DefCallExpression) expr).getDefinition() != myConstructor) {
      return new PatternMatchFailedResult(this, expr);
    }
    if (constructorArgs.size() != myArguments.size()) {
      throw new IllegalStateException();
    }
    List<Expression> result = new ArrayList<>();
    PatternMatchMaybeResult maybe = null;
    for (int i  = 0; i < constructorArgs.size(); i++) {
      PatternMatchResult subMatch = myArguments.get(i).getPattern().match(constructorArgs.get(i), context);
      if (subMatch instanceof PatternMatchFailedResult)
        return subMatch;
      if (subMatch instanceof PatternMatchMaybeResult) {
        if (maybe == null)
          maybe = (PatternMatchMaybeResult) subMatch;
      } else if (subMatch instanceof PatternMatchOKResult)
      result.addAll(((PatternMatchOKResult) subMatch).expressions);
    }
    return maybe != null ? maybe : new PatternMatchOKResult(result);
  }
}
