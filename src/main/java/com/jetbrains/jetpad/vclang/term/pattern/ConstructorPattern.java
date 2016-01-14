package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class ConstructorPattern extends Pattern implements Abstract.ConstructorPattern {
  private final Constructor myConstructor;
  private final Patterns myArguments;

  public ConstructorPattern(Constructor constructor, Patterns arguments) {
    myConstructor = constructor;
    myArguments = arguments;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  @Override
  public String getConstructorName() {
    return myConstructor.getName();
  }

  @Override
  public List<PatternArgument> getArguments() {
    return myArguments.getPatterns();
  }

  @Override
  public DependentLink getParameters() {
    return myArguments.getParameters();
  }

  @Override
  public Expression toExpression(Map<Binding, Expression> substs) {
    List<Expression> params = new ArrayList<>();
    for (DependentLink link = myConstructor.getParameters(); link != null; link = link.getNext()) {
      Expression param = substs.get(link);
      params.add(param == null ? Reference(link) : param);
    }
    Expression result = ConCall(myConstructor, params);
    DependentLink constructorParameters = myConstructor.getParameters();
    DependentLink link = constructorParameters;
    for (PatternArgument patternArgument : myArguments.getPatterns()) {
      assert link != null;
      if (patternArgument.getPattern() instanceof ConstructorPattern) {
        List<Expression> args = new ArrayList<>();
        Expression type = link.getType().subst(substs).normalize(NormalizeVisitor.Mode.WHNF).getFunction(args);
        assert type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == ((ConstructorPattern) patternArgument.getPattern()).getConstructor().getDataType();
        Collections.reverse(args);
        substs.putAll(getMatchedArguments(args));
      }
      Expression param = patternArgument.getPattern().toExpression(substs);
      if (patternArgument.getPattern() instanceof ConstructorPattern) {
        DependentLink.Helper.freeSubsts(getParameters(), substs);
      }

      result = Apps(result, param);
      substs.put(link, param);
      link = link.getNext();
    }
    DependentLink.Helper.freeSubsts(constructorParameters, substs);
    return result;
  }

  private Map<Binding, Expression> getMatchedArguments(List<Expression> dataTypeArguments) {
    if (myConstructor.getPatterns() != null) {
      dataTypeArguments = ((Pattern.MatchOKResult) myConstructor.getPatterns().match(dataTypeArguments)).expressions;
    }
    return DependentLink.Helper.toSubsts(myConstructor.getDataTypeParameters(), dataTypeArguments);
  }

  @Override
  public MatchResult match(Expression expr) {
    List<Expression> constructorArgs = new ArrayList<>();
    expr = expr.normalize(NormalizeVisitor.Mode.WHNF).getFunction(constructorArgs);
    Collections.reverse(constructorArgs);
    if (!(expr instanceof ConCallExpression)) {
      return new MatchMaybeResult(this, expr);
    }
    if (((ConCallExpression) expr).getDefinition() != myConstructor) {
      return new MatchFailedResult(this, expr);
    }
    if (constructorArgs.size() != myArguments.getPatterns().size()) {
      throw new IllegalStateException();
    }
    return myArguments.match(constructorArgs);
  }
}
