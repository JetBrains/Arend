package org.arend.typechecking.subexpr;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.Pattern;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FindBinding {
  public static @Nullable DependentLink visitLam(
      Referable referable, Concrete.LamExpression expr, LamExpression lam) {
    return parameters(lam.getParameters(), referable, new LambdaParam(lam), expr.getParameters());
  }

  private static class LambdaParam implements Function<DependentLink, DependentLink> {
    public LambdaParam(@NotNull LamExpression lambda) {
      this.lambda = lambda;
    }

    private LamExpression lambda;

    @Override
    public DependentLink apply(DependentLink link) {
      DependentLink next = link.getNext();
      if (next instanceof EmptyDependentLink) {
        lambda = lambda.getBody().cast(LamExpression.class);
        if (lambda == null) return null;
        return lambda.getParameters();
      } else return next;
    }
  }

  public static @Nullable DependentLink visitPi(
      Referable referable, Concrete.PiExpression expr, PiExpression pi) {
    return parameters(pi.getBinding(), referable, new Function<>() {
      private PiExpression piExpr = pi;

      @Override
      public DependentLink apply(DependentLink link) {
        DependentLink next = link.getNext();
        if (next instanceof EmptyDependentLink) {
          piExpr = piExpr.getCodomain().cast(PiExpression.class);
          if (piExpr == null) return null;
          return piExpr.getBinding();
        } else return next;
      }
    }, expr.getParameters());
  }

  public static @Nullable DependentLink visitSigma(
      Referable referable,
      Concrete.SigmaExpression expr,
      SigmaExpression sigma
  ) {
    return parameters(sigma.getParameters(), referable, DependentLink::getNext, expr.getParameters());
  }

  public static @Nullable DependentLink visitClauses(
      Object data,
      List<? extends Concrete.Clause> clauses,
      List<? extends ElimClause<Pattern>> coreClauses
  ) {
    return CorrespondedSubExprVisitor.visitElimBody(clauses, coreClauses, (coreClause, clause) ->
        visitPattern(data, coreClause.getPatterns().iterator(), clause.getPatterns().iterator()));
  }

  public static @Nullable DependentLink visitCase(
      Object data, Concrete.CaseExpression caseExpr, CaseExpression coreCaseExpr) {
    return visitClauses(data, caseExpr.getClauses(), coreCaseExpr.getElimBody().getClauses());
  }

  /**
   * @param data Both {@link Referable} and {@link Object} are supported.
   */
  private static @Nullable DependentLink visitPattern(
      Object data, Iterator<? extends Pattern> corePatterns, Iterator<Concrete.Pattern> patterns) {
    findBinding:
    while (corePatterns.hasNext() && patterns.hasNext()) {
      Pattern corePattern = corePatterns.next();
      Concrete.Pattern pattern = patterns.next();
      while (corePattern instanceof BindingPattern && pattern instanceof Concrete.NamePattern) {
        DependentLink binding = ((BindingPattern) corePattern).getBinding();
        if (binding.isExplicit() != pattern.isExplicit()) {
          if (!corePatterns.hasNext()) break findBinding;
          corePattern = corePatterns.next();
          continue;
        }
        if (Objects.equals(Referable.getUnderlyingReferable(((Concrete.NamePattern) pattern).getReferable()), data)
            || Objects.equals(pattern.getData(), data)) return binding;
          // Go to next binding
        else continue findBinding;
      }
      Iterator<? extends Pattern> coreSubPatterns = corePattern.getSubPatterns().iterator();
      if (pattern instanceof Concrete.ConstructorPattern) {
        DependentLink link = visitPattern(data, coreSubPatterns,
            ((Concrete.ConstructorPattern) pattern).getPatterns().iterator());
        if (link != null) return link;
      } else if (pattern instanceof Concrete.TuplePattern) {
        DependentLink link = visitPattern(data, coreSubPatterns,
            ((Concrete.TuplePattern) pattern).getPatterns().iterator());
        if (link != null) return link;
      }
    }
    return null;
  }

  /**
   * @param patternData Both {@link Referable} and {@link Object} are supported.
   */
  public static @Nullable Expression visitLetBind(
      Object patternData, Concrete.LetExpression expr, LetExpression let) {
    return visitLet(expr, let, (coreLetClause, exprLetClause) ->
        Objects.equals(exprLetClause.getPattern().getData(), patternData)
            || Objects.equals(Referable.getUnderlyingReferable(exprLetClause.getPattern().getReferable()), patternData)
            ? coreLetClause.getTypeExpr() : null);
  }

  /**
   * @param patternData Both {@link Referable} and {@link Object} are supported.
   */
  public static @Nullable Expression visitLet(
      Object patternData, Concrete.LetExpression expr, LetExpression let) {
    Expression expression = visitLetBind(patternData, expr, let);
    if (expression != null) return expression;
    if (patternData instanceof Referable) {
      DependentLink link = visitLetParam((Referable) patternData, expr, let);
      if (link != null) return link.getTypeExpr();
    }
    return null;
  }

  public static @Nullable DependentLink visitLetParam(
      Referable patternParam, Concrete.LetExpression expr, LetExpression let) {
    return visitLet(expr, let, (coreLetClause, exprLetClause) -> {
      LamExpression coreLamExpr = coreLetClause.getExpression().cast(LamExpression.class);
      List<Concrete.Parameter> parameters = exprLetClause.getParameters();
      return coreLamExpr != null && !parameters.isEmpty()
          ? parameters(coreLamExpr.getParameters(), patternParam, new LambdaParam(coreLamExpr), parameters) : null;
    });
  }

  private static <T> @Nullable T visitLet(
      Concrete.LetExpression expr,
      LetExpression let,
      BiFunction<LetClause, Concrete.LetClause, @Nullable T> function
  ) {
    List<Concrete.LetClause> exprClauses = expr.getClauses();
    List<LetClause> coreClauses = let.getClauses();
    for (int i = 0; i < exprClauses.size(); i++) {
      T apply = function.apply(coreClauses.get(i), exprClauses.get(i));
      if (apply != null) return apply;
    }
    return null;
  }

  private static @Nullable DependentLink parameters(
      DependentLink core,
      Referable referable,
      Function<DependentLink, DependentLink> next,
      List<? extends Concrete.Parameter> parameters
  ) {
    for (Concrete.Parameter concrete : parameters)
      for (Referable ref : concrete.getReferableList()) {
        if (Referable.getUnderlyingReferable(ref) == referable) return core;
        if (concrete.isExplicit() != core.isExplicit()) continue;
        core = next.apply(core);
        if (core == null) return null;
      }
    return null;
  }
}
