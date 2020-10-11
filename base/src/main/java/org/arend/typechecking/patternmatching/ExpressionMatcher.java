package org.arend.typechecking.patternmatching;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.*;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.naming.renamer.Renamer;
import org.arend.prelude.Prelude;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ExpressionMatcher {
  private static Expression replaceMatchingExpressionArguments(ConstructorExpressionPattern pattern, Expression expression, List<Expression> newArgs) {
    Expression data = pattern.getDataExpression();
    if (data instanceof SigmaExpression) {
      return new TupleExpression(newArgs, (SigmaExpression) data);
    }

    if (data instanceof FunCallExpression) {
      return expression;
    }

    if (data instanceof ConCallExpression || data instanceof SmallIntegerExpression) {
      ConCallExpression conCall = expression.cast(ConCallExpression.class);
      IntegerExpression intExpr = expression.cast(IntegerExpression.class);
      return conCall != null && conCall.getDefinition() == Prelude.SUC || intExpr != null && !intExpr.isZero() ? ExpressionFactory.Suc(newArgs.get(0)) : expression;
    }

    NewExpression newExpr = expression.cast(NewExpression.class);
    if (newExpr == null) {
      return expression;
    }

    int i = 0;
    Map<ClassField, Expression> newImpls = new HashMap<>();
    for (ClassField field : ((ClassCallExpression) data).getDefinition().getFields()) {
      if (((ClassCallExpression) data).isImplemented(field)) {
        newImpls.put(field, newExpr.getImplementation(field));
      } else {
        newImpls.put(field, newArgs.get(i++));
      }
    }

    ClassCallExpression classCall = newExpr.getClassCall();
    return new NewExpression(null, new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), newImpls, classCall.getSort(), classCall.getUniverseKind()));
  }

  private static Expression matchExpression(Expression expr, ExpressionPattern pattern, boolean computeData, List<MatchResult> result) {
    if (pattern.isAbsurd()) {
      return null;
    }

    if (pattern.getBinding() != null) {
      result.add(new MatchResult(expr, pattern, pattern.getBinding()));
      return computeData ? new ReferenceExpression(pattern.getBinding()) : expr;
    }

    if (!(pattern instanceof ConstructorExpressionPattern)) {
      return null;
    }

    ConstructorExpressionPattern conPattern = (ConstructorExpressionPattern) pattern;
    expr = expr.normalize(NormalizationMode.WHNF);
    List<? extends Expression> args = conPattern.getMatchingExpressionArguments(expr, true);
    if (args == null) {
      Binding binding = new TypedBinding(Renamer.UNNAMED, expr.computeType());
      result.add(new MatchResult(expr, conPattern, binding));
      return computeData ? new ReferenceExpression(binding) : expr;
    }
    if (args.isEmpty()) {
      return expr;
    }

    assert args.size() == pattern.getSubPatterns().size();
    List<Expression> newArgs = computeData ? new ArrayList<>() : null;
    for (int i = 0; i < args.size(); i++) {
      Expression arg = matchExpression(args.get(i), pattern.getSubPatterns().get(i), computeData, result);
      if (arg == null) {
        return null;
      }
      if (computeData) newArgs.add(arg);
    }

    return computeData ? replaceMatchingExpressionArguments(conPattern, expr, newArgs) : expr;
  }

  public static class MatchResult {
    public final Expression expression;
    public final ExpressionPattern pattern;
    public final Binding binding;

    public MatchResult(Expression expression, ExpressionPattern pattern, Binding binding) {
      this.expression = expression;
      this.pattern = pattern;
      this.binding = binding;
    }
  }

  /**
   * If the result is not null, then there exists a data call D such that the following conditions hold.
   * {@code constructor} has type D[subst], where subst is the substitution that
   * maps {@link MatchResult#binding} to {@link MatchResult#pattern} for each match result from {@code matchResults}.
   *
   * Also, D[subst'] is equal to {@code dataCall}, where subst' is the substitution that
   * maps {@link MatchResult#binding} to {@link MatchResult#expression} for each match result from {@code matchResults}.
   *
   * Returns {@code null} for constructors without patterns or if the result satisfying these conditions does not exist.
   * If {@code computeData} is {@code true}, returns either D or {@code null}.
   * Otherwise, returns either {@code dataCall} or {@code null}.
   */
  public static @Nullable DataCallExpression computeMatchingExpressions(DataCallExpression dataCall, Constructor constructor, boolean computeData, List<MatchResult> matchResults) {
    if (constructor.getPatterns() == null) {
      return null;
    }

    assert constructor.getPatterns().size() == dataCall.getDefCallArguments().size();
    List<Expression> newArgs = computeData ? new ArrayList<>() : null;
    for (int i = 0; i < constructor.getPatterns().size(); i++) {
      Expression arg = matchExpression(dataCall.getDefCallArguments().get(i), constructor.getPatterns().get(i), computeData, matchResults);
      if (arg == null) {
        return null;
      }
      if (computeData) newArgs.add(arg);
    }

    return computeData ? new DataCallExpression(dataCall.getDefinition(), dataCall.getSortArgument(), newArgs) : dataCall;
  }

  public static @Nullable ConCallExpression computeMatchingPatterns(DataCallExpression dataCall, Constructor constructor, @Nullable ExprSubstitution substitution, Map<Binding, ExpressionPattern> result) {
    List<MatchResult> matchResults = new ArrayList<>();
    if (computeMatchingExpressions(dataCall, constructor, false, matchResults) == null) {
      return null;
    }

    ExprSubstitution conSubst = new ExprSubstitution();
    for (MatchResult matchResult : matchResults) {
      Expression expr = matchResult.expression.normalize(NormalizationMode.WHNF);
      if (matchResult.pattern.getBinding() != null) {
        conSubst.add(matchResult.binding, expr);
      }
      if (!(expr instanceof ReferenceExpression)) {
        if (matchResult.pattern.getBinding() != null) {
          continue;
        }
        return null;
      }
      Binding binding = ((ReferenceExpression) expr).getBinding();
      Expression subst = substitution == null ? null : substitution.get(binding);
      if (subst != null && !(subst instanceof ReferenceExpression)) {
        return null;
      }
      if (subst != null) {
        binding = ((ReferenceExpression) subst).getBinding();
      }
      result.put(binding, matchResult.pattern);
    }

    List<Expression> args = new ArrayList<>();
    for (DependentLink link = Pattern.getFirstBinding(constructor.getPatterns()); link.hasNext(); link = link.getNext()) {
      Expression expr = conSubst.get(link);
      args.add(expr != null ? expr : new ReferenceExpression(link));
    }
    return new ConCallExpression(constructor, dataCall.getSortArgument(), args, Collections.emptyList());
  }
}
