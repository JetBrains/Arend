package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class ConstructorPattern extends Pattern implements Abstract.ConstructorPattern {
  private final Constructor myConstructor;
  private final Patterns myArguments;

  public ConstructorPattern(Constructor constructor, Patterns arguments) {
    assert !constructor.hasErrors();
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
  public Expression toExpression(ExprSubstitution subst) {
    List<Expression> params = new ArrayList<>();
    for (DependentLink link = myConstructor.getDataTypeParameters(); link.hasNext(); link = link.getNext()) {
      Expression param = subst.get(link);
      params.add(param == null ? Reference(link) : param);
    }

    DependentLink constructorParameters = myConstructor.getParameters();
    DependentLink link = constructorParameters;
    List<Expression> arguments = new ArrayList<>();
    for (PatternArgument patternArgument : myArguments.getPatterns()) {
      assert link.hasNext();
      if (patternArgument.getPattern() instanceof ConstructorPattern) {
        Expression type = link.getType().subst(subst).normalize(NormalizeVisitor.Mode.WHNF);
        List<? extends Expression> args = type.getArguments();
        type = type.getFunction();
        assert type.toDataCall() != null && type.toDataCall().getDefinition() == ((ConstructorPattern) patternArgument.getPattern()).getConstructor().getDataType();
        ExprSubstitution subSubst = ((ConstructorPattern) patternArgument.getPattern()).getMatchedArguments(new ArrayList<>(args));
        for (Binding binding : subSubst.getDomain()) {
          subst.add(binding, subSubst.get(binding));
        }
      }
      Expression param = patternArgument.getPattern().toExpression(subst);
      if (patternArgument.getPattern() instanceof ConstructorPattern) {
        DependentLink.Helper.freeSubsts(getParameters(), subst);
      }

      arguments.add(param);
      subst.add(link, param);
      link = link.getNext();
    }
    DependentLink.Helper.freeSubsts(constructorParameters, subst);
    return Apps(ConCall(myConstructor, params), arguments);
  }

  public ExprSubstitution getMatchedArguments(List<Expression> dataTypeArguments) {
    return DependentLink.Helper.toSubstitution(myConstructor.getDataTypeParameters(), myConstructor.matchDataTypeArguments(dataTypeArguments));
  }

  @Override
  public MatchResult match(Expression expr, boolean normalize) {
    if (normalize) {
      expr = expr.normalize(NormalizeVisitor.Mode.WHNF);
    }

    List<? extends Expression> constructorArgs = expr.getArguments();
    expr = expr.getFunction();
    ConCallExpression conCall = expr.toConCall();
    if (conCall == null) {
      return new MatchMaybeResult(this, expr);
    }
    if (conCall.getDefinition() != myConstructor) {
      return new MatchFailedResult(this, expr);
    }
    if (constructorArgs.size() != myArguments.getPatterns().size()) {
      throw new IllegalStateException();
    }
    return myArguments.match(constructorArgs, normalize);
  }
}
