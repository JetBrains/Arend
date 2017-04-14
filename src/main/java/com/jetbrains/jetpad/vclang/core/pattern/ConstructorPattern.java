package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.StdLevelSubstitution;

import java.util.ArrayList;
import java.util.List;

public class ConstructorPattern extends Pattern {
  private final Constructor myConstructor;
  private final Patterns myArguments;

  public ConstructorPattern(Constructor constructor, Patterns arguments) {
    assert constructor.status().headerIsOK();
    myConstructor = constructor;
    myArguments = arguments;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  public Patterns getPatterns() {
    return myArguments;
  }

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
      params.add(param == null ? new ReferenceExpression(link) : param);
    }

    DependentLink constructorParameters = myConstructor.getParameters();
    DependentLink link = constructorParameters;
    List<Expression> arguments = new ArrayList<>();
    for (PatternArgument patternArgument : myArguments.getPatterns()) {
      assert link.hasNext();
      LevelSubstitution levelSubst;
      if (patternArgument.getPattern() instanceof ConstructorPattern) {
        Expression type = link.getType().getExpr().subst(subst).normalize(NormalizeVisitor.Mode.WHNF);
        assert type.toDataCall() != null && type.toDataCall().getDefinition() == ((ConstructorPattern) patternArgument.getPattern()).getConstructor().getDataType();
        ExprSubstitution subSubst = ((ConstructorPattern) patternArgument.getPattern()).getMatchedArguments(new ArrayList<>(type.toDataCall().getDefCallArguments()));
        levelSubst = new StdLevelSubstitution(type.toDataCall().getSortArgument().getPLevel(), type.toDataCall().getSortArgument().getHLevel());
        subst.addAll(subSubst);
      } else {
        levelSubst = LevelSubstitution.EMPTY;
      }
      Expression param = patternArgument.getPattern().toExpression(subst).subst(levelSubst);
      if (patternArgument.getPattern() instanceof ConstructorPattern) {
        DependentLink.Helper.freeSubsts(getParameters(), subst);
      }

      arguments.add(param);
      subst.add(link, param);
      link = link.getNext();
    }
    DependentLink.Helper.freeSubsts(constructorParameters, subst);
    return new ConCallExpression(myConstructor, Sort.STD, params, arguments);
  }

  public ExprSubstitution getMatchedArguments(List<Expression> dataTypeArguments) {
    return DependentLink.Helper.toSubstitution(myConstructor.getDataTypeParameters(), myConstructor.matchDataTypeArguments(dataTypeArguments));
  }

  @Override
  public MatchResult match(Expression expr, boolean normalize) {
    if (normalize) {
      expr = expr.normalize(NormalizeVisitor.Mode.WHNF);
    }

    ConCallExpression conCall = expr.toConCall();
    if (conCall == null) {
      return new MatchMaybeResult(this, expr);
    }
    if (conCall.getDefinition() != myConstructor) {
      return new MatchFailedResult(this, expr);
    }

    if (conCall.getDefCallArguments().size() != myArguments.getPatterns().size()) {
      throw new IllegalStateException();
    }
    return myArguments.match(conCall.getDefCallArguments(), normalize);
  }
}
