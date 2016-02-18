package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
  public Expression toExpression(Substitution subst) {
    List<Expression> params = new ArrayList<>();
    for (DependentLink link = myConstructor.getDataTypeParameters(); link.hasNext(); link = link.getNext()) {
      Expression param = subst.get(link);
      params.add(param == null ? Reference(link) : param);
    }
    Expression result = ConCall(myConstructor, params);
    DependentLink constructorParameters = myConstructor.getParameters();
    DependentLink link = constructorParameters;
    for (PatternArgument patternArgument : myArguments.getPatterns()) {
      assert link.hasNext();
      if (patternArgument.getPattern() instanceof ConstructorPattern) {
        List<Expression> args = new ArrayList<>();
        Expression type = link.getType().subst(subst).normalize(NormalizeVisitor.Mode.WHNF).getFunction(args);
        assert type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == ((ConstructorPattern) patternArgument.getPattern()).getConstructor().getDataType();
        Collections.reverse(args);
        Substitution subSubst = ((ConstructorPattern) patternArgument.getPattern()).getMatchedArguments(args);
        for (Binding binding : subSubst.getDomain()) {
          subst.add(binding, subSubst.get(binding));
        }
      }
      Expression param = patternArgument.getPattern().toExpression(subst);
      if (patternArgument.getPattern() instanceof ConstructorPattern) {
        DependentLink.Helper.freeSubsts(getParameters(), subst);
      }

      result = Apps(result, param);
      subst.add(link, param);
      link = link.getNext();
    }
    DependentLink.Helper.freeSubsts(constructorParameters, subst);
    return result;
  }

  public Substitution getMatchedArguments(List<Expression> dataTypeArguments) {
    return DependentLink.Helper.toSubstitution(myConstructor.getDataTypeParameters(), myConstructor.matchDataTypeArguments(dataTypeArguments));
  }

  @Override
  public MatchResult match(Expression expr, boolean normalize) {
    List<Expression> constructorArgs = new ArrayList<>();
    expr = (normalize ? expr.normalize(NormalizeVisitor.Mode.WHNF) : expr).getFunction(constructorArgs);
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
    return myArguments.match(constructorArgs, normalize);
  }
}
