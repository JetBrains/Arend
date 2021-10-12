package org.arend.typechecking.patternmatching;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.*;
import org.arend.core.pattern.*;
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
      FunCallExpression funCall = (FunCallExpression) data;
      newArgs.addAll(0, funCall.getDefCallArguments());
      return funCall.getDefinition() == Prelude.IDP ? expression : FunCallExpression.make(funCall.getDefinition(), funCall.getLevels(), newArgs);
    }

    if (data instanceof ConCallExpression && ((ConCallExpression) data).getDefinition() != Prelude.SUC) {
      ConCallExpression conCall = (ConCallExpression) data;
      return ConCallExpression.make(conCall.getDefinition(), conCall.getLevels(), conCall.getDataTypeArguments(), newArgs);
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
    Map<ClassField, Expression> newImpls = new LinkedHashMap<>();
    for (ClassField field : ((ClassCallExpression) data).getDefinition().getFields()) {
      if (((ClassCallExpression) data).isImplemented(field)) {
        newImpls.put(field, newExpr.getImplementation(field));
      } else {
        newImpls.put(field, newArgs.get(i++));
      }
    }

    ClassCallExpression classCall = newExpr.getClassCall();
    return new NewExpression(null, new ClassCallExpression(classCall.getDefinition(), classCall.getLevels(), newImpls, classCall.getSort(), classCall.getUniverseKind()));
  }

  /**
   * If {@code computeData} is true, then {@code pattern} must be {@link ExpressionPattern}.
   */
  public static Expression matchExpression(Expression expr, Pattern pattern, boolean computeData, List<MatchResult> result) {
    if (computeData && !(pattern instanceof ExpressionPattern)) {
      throw new IllegalArgumentException();
    }
    if (pattern.isAbsurd()) {
      return null;
    }

    if (pattern instanceof BindingPattern) {
      result.add(new MatchResult(expr, pattern, pattern.getBinding()));
      return computeData ? new ReferenceExpression(pattern.getBinding()) : expr;
    }

    if (!(pattern instanceof ConstructorPattern)) {
      return null;
    }

    expr = expr.normalize(NormalizationMode.WHNF);
    if (!(pattern instanceof ConstructorExpressionPattern)) {
      if (expr instanceof TupleExpression) {
        return matchExpressions(((TupleExpression) expr).getFields(), pattern.getSubPatterns(), false, result) != null ? expr : null;
      }
      if (expr instanceof ConCallExpression && pattern.getDefinition() == ((ConCallExpression) expr).getDefinition()) {
        return matchExpressions(((ConCallExpression) expr).getDefCallArguments(), pattern.getSubPatterns(), false, result) != null ? expr : null;
      }
      result.add(new MatchResult(expr, pattern, new TypedBinding(Renamer.UNNAMED, expr.computeType())));
      return expr;
    }

    ConstructorExpressionPattern conPattern = (ConstructorExpressionPattern) pattern;
    List<? extends Expression> args = conPattern.getMatchingExpressionArguments(expr, true);
    if (args == null) {
      Binding binding = new TypedBinding(Renamer.UNNAMED, expr.computeType());
      result.add(new MatchResult(expr, conPattern, binding));
      return computeData ? new ReferenceExpression(binding) : expr;
    }
    if (args.isEmpty()) {
      return expr;
    }

    List<Expression> newArgs = matchExpressions(args, pattern.getSubPatterns(), computeData, result);
    return newArgs == null ? null : computeData ? replaceMatchingExpressionArguments(conPattern, expr, newArgs) : expr;
  }

  /**
   * If {@code computeData} is true, then {@code patterns} must consist of {@link ExpressionPattern}.
   */
  public static List<Expression> matchExpressions(List<? extends Expression> exprs, List<? extends Pattern> patterns, boolean computeData, List<MatchResult> result) {
    assert exprs.size() == patterns.size();
    List<Expression> newExprs = computeData ? new ArrayList<>() : Collections.emptyList();
    for (int i = 0; i < exprs.size(); i++) {
      Expression newExpr = matchExpression(exprs.get(i), patterns.get(i), computeData, result);
      if (newExpr == null) {
        return null;
      }
      if (computeData) newExprs.add(newExpr);
    }
    return newExprs;
  }

  public static class MatchResult {
    public final Expression expression;
    public final Pattern pattern;
    public final Binding binding;

    public MatchResult(Expression expression, Pattern pattern, Binding binding) {
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

    List<Expression> newArgs = matchExpressions(dataCall.getDefCallArguments(), constructor.getPatterns(), computeData, matchResults);
    return computeData ? new DataCallExpression(dataCall.getDefinition(), dataCall.getLevels(), newArgs) : dataCall;
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
      boolean isRef = expr instanceof ReferenceExpression;
      if (isRef && (matchResult.pattern.getDefinition() == Prelude.ZERO || matchResult.pattern.getDefinition() == Prelude.SUC)) {
        Expression type = expr.computeType().normalize(NormalizationMode.WHNF);
        if (type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.FIN) {
          isRef = false;
        }
      }
      if (!isRef) {
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

      ExpressionPattern prevPattern = result.get(binding);
      ExpressionPattern newPattern = (ExpressionPattern) matchResult.pattern;
      if (prevPattern != null) {
        newPattern = prevPattern.intersect(newPattern);
        if (newPattern == null) {
          return null;
        }
      }
      result.put(binding, newPattern);
    }

    List<Expression> args = new ArrayList<>();
    for (DependentLink link = Pattern.getFirstBinding(constructor.getPatterns()); link.hasNext(); link = link.getNext()) {
      Expression expr = conSubst.get(link);
      args.add(expr != null ? expr : new ReferenceExpression(link));
    }
    return new ConCallExpression(constructor, dataCall.getLevels(), args, Collections.emptyList());
  }
}
