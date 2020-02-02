package org.arend.typechecking.visitor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CorrespondedSubExprVisitor implements
    ConcreteExpressionVisitor<Expression, Pair<Expression, Concrete.Expression>> {
  private final Concrete.Expression subExpr;

  public CorrespondedSubExprVisitor(Concrete.Expression subExpr) {
    this.subExpr = subExpr;
  }

  private boolean matchesSubExpr(Concrete.Expression expr) {
    return Objects.equals(expr.getData(), subExpr.getData());
  }

  private Pair<Expression, Concrete.Expression> atomicExpr(Concrete.Expression expr, Expression coreExpr) {
    return matchesSubExpr(expr) ? new Pair<>(coreExpr, expr) : null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitHole(Concrete.HoleExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitReference(Concrete.ReferenceExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitThis(Concrete.ThisExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitInferenceReference(Concrete.InferenceReferenceExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitNumericLiteral(Concrete.NumericLiteral expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitUniverse(Concrete.UniverseExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitGoal(Concrete.GoalExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitProj(Concrete.ProjExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    ProjExpression coreProjExpr = coreExpr.cast(ProjExpression.class);
    if (coreProjExpr == null) return null;
    return expr.getExpression().accept(this, coreProjExpr.getExpression());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitNew(Concrete.NewExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    NewExpression coreNewExpr = coreExpr.cast(NewExpression.class);
    if (coreNewExpr == null) return null;
    return expr.getExpression().accept(this, coreNewExpr.getClassCall());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitTuple(Concrete.TupleExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    TupleExpression coreTupleExpr = coreExpr.cast(TupleExpression.class);
    if (coreTupleExpr == null) return null;
    return visitExprs(coreTupleExpr.getFields(), expr.getFields());
  }

  private Pair<Expression, Concrete.Expression> visitExprs(List<? extends Expression> coreExpr, List<? extends Concrete.Expression> expr) {
    for (int i = 0; i < expr.size(); i++) {
      Concrete.Expression expression = expr.get(i);
      Pair<Expression, Concrete.Expression> accepted = expression.accept(this, coreExpr.get(i));
      if (accepted != null) return accepted;
    }
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitLet(Concrete.LetExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    LetExpression coreLetExpr = coreExpr.cast(LetExpression.class);
    if (coreLetExpr == null) return null;
    List<Concrete.LetClause> exprClauses = expr.getClauses();
    List<LetClause> coreClauses = coreLetExpr.getClauses();
    for (int i = 0; i < exprClauses.size(); i++) {
      LetClause coreLetClause = coreClauses.get(i);
      Concrete.LetClause exprLetClause = exprClauses.get(i);

      Pair<Expression, Concrete.Expression> accepted = exprLetClause.getTerm().accept(this, coreLetClause.getExpression());
      if (accepted != null) return accepted;

      Concrete.Expression resultType = exprLetClause.getResultType();
      if (resultType != null) {
        accepted = resultType.accept(this, coreLetClause.getTypeExpr());
        if (accepted != null) return accepted;
      }
    }
    return expr.getExpression().accept(this, coreLetExpr.getExpression());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitTyped(Concrete.TypedExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    return expr.expression.accept(this, coreExpr);
  }

  private Pair<Expression, Concrete.Expression> visitClonedApp(Concrete.AppExpression expr, Expression coreExpr) {
    // This is a mutable reference
    List<Concrete.Argument> arguments = expr.getArguments();
    if (arguments.isEmpty()) return expr.getFunction().accept(this, coreExpr);

    AppExpression coreAppExpr = coreExpr.cast(AppExpression.class);
    LamExpression coreEtaExpr = coreExpr.cast(LamExpression.class);
    ConCallExpression coreConExpr = coreExpr.cast(ConCallExpression.class);
    DefCallExpression coreDefExpr = coreExpr.cast(DefCallExpression.class);
    int lastArgIndex = arguments.size() - 1;
    if (coreAppExpr != null) {
      Concrete.Argument lastArgument = arguments.get(lastArgIndex);
      Expression function = coreAppExpr.getFunction();
      PiExpression type = function.getType().cast(PiExpression.class);
      while (type.getParameters().isExplicit() != lastArgument.isExplicit()) {
        coreAppExpr = (AppExpression) function;
        function = coreAppExpr.getFunction();
        type = function.getType().cast(PiExpression.class);
      }
      Pair<Expression, Concrete.Expression> accepted = lastArgument.getExpression().accept(this, coreAppExpr.getArgument());
      if (accepted != null) return accepted;
      arguments.remove(lastArgIndex);
      return visitClonedApp(expr, function);
    } else if (coreEtaExpr != null) {
      // `f a` (concrete) gets elaborated to `\b -> f a b` (core) if `f` takes 2
      // arguments, so we try to match `f a` (concrete) and `f a b` (core),
      // ignoring the extra argument `b`.
      return visitClonedApp(expr, coreEtaExpr.getBody());
    } else if (coreDefExpr != null) {
      return visitArguments(coreDefExpr, arguments.iterator());
    } else return null;
  }

  private Pair<Expression, Concrete.Expression> visitArguments(
      DefCallExpression expression,
      Iterator<Concrete.Argument> arguments
  ) {
    Iterator<? extends Expression> defCallArgs = expression.getDefCallArguments().iterator();
    Concrete.Argument argument = arguments.next();
    for (DependentLink parameter = expression.getDefinition().getParameters();
         parameter.hasNext() && defCallArgs.hasNext();
         parameter = parameter.getNext()) {
      Expression coreArg = defCallArgs.next();
      // Take care of implicit application
      if (parameter.isExplicit() == argument.isExplicit()) {
        Pair<Expression, Concrete.Expression> accepted = argument.getExpression().accept(this, coreArg);
        if (accepted != null) return accepted;
        argument = arguments.next();
      }
    }
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitApp(Concrete.AppExpression expr, Expression coreExpr) {
    if (subExpr instanceof Concrete.AppExpression && Objects.equals(
        ((Concrete.AppExpression) subExpr).getFunction().getData(),
        expr.getFunction().getData()
    )) return new Pair<>(coreExpr, expr);
    Concrete.Expression cloned = Concrete.AppExpression.make(expr.getData(), expr.getFunction(), new ArrayList<>(expr.getArguments()));
    return visitClonedApp(((Concrete.AppExpression) cloned), coreExpr);
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitLam(Concrete.LamExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    Expression body = coreExpr;
    for (Concrete.Parameter parameter : expr.getParameters()) {
      if (body instanceof LamExpression) {
        LamExpression coreLamExpr = (LamExpression) body;
        Concrete.Expression type = parameter.getType();
        if (type != null) {
          Pair<Expression, Concrete.Expression> ty = type.accept(this, coreLamExpr.getParameters().getTypeExpr());
          if (ty != null) return ty;
        }
        body = coreLamExpr.getBody();
      } else return null;
    }
    return expr.getBody().accept(this, body);
  }

  protected Pair<Expression, Concrete.Expression> visitParameter(Concrete.Parameter parameter, DependentLink link) {
    Concrete.Expression type = parameter.getType();
    if (type == null) return null;
    return type.accept(this, link.getTypeExpr());
  }

  protected Pair<Expression, Concrete.Expression> visitPiParameters(List<? extends Concrete.Parameter> parameters, PiExpression pi) {
    for (Concrete.Parameter parameter : parameters) {
      DependentLink link = pi.getParameters();
      Pair<Expression, Concrete.Expression> expression = visitParameter(parameter, link);
      if (expression != null) return expression;
      Expression codomain = pi.getCodomain();
      if (codomain instanceof PiExpression) pi = (PiExpression) codomain;
      else return null;
    }
    return null;
  }

  protected Pair<Expression, Concrete.Expression> visitSigmaParameters(List<? extends Concrete.Parameter> parameters, DependentLink sig) {
    for (Concrete.Parameter parameter : parameters) {
      Pair<Expression, Concrete.Expression> expression = visitParameter(parameter, sig);
      if (expression != null) return expression;
      sig = sig.getNextTyped(null).getNext();
    }
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitPi(Concrete.PiExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    PiExpression corePiExpr = coreExpr.cast(PiExpression.class);
    if (corePiExpr == null) return null;
    Pair<Expression, Concrete.Expression> expression = visitPiParameters(expr.getParameters(), corePiExpr);
    if (expression != null) return expression;
    return expr.getCodomain().accept(this, corePiExpr.getCodomain());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitSigma(Concrete.SigmaExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    SigmaExpression coreSigmaExpr = coreExpr.cast(SigmaExpression.class);
    if (coreSigmaExpr == null) return null;
    return visitSigmaParameters(expr.getParameters(), coreSigmaExpr.getParameters());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitCase(Concrete.CaseExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    CaseExpression coreCaseExpr = coreExpr.cast(CaseExpression.class);
    if (coreCaseExpr == null) return null;
    Pair<Expression, Concrete.Expression> expression = visitExprs(coreCaseExpr.getArguments(), expr
        .getArguments()
        .stream()
        .map(i -> i.expression)
        .collect(Collectors.toList()));
    if (expression != null) return expression;
    Concrete.Expression resultType = expr.getResultType();
    if (resultType != null) {
      Pair<Expression, Concrete.Expression> accepted = resultType.accept(this, coreCaseExpr.getResultType());
      if (accepted != null) return accepted;
    }
    // Case trees and clauses? They are unlikely to be isomorphic.
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitEval(Concrete.EvalExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    throw new IllegalStateException("Eval shouldn't appear");
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitClassExt(Concrete.ClassExtExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    ClassCallExpression coreClassExpr = coreExpr.cast(ClassCallExpression.class);
    if (coreClassExpr == null) return null;
    // How about the other subexpressions?
    return expr.getBaseClassExpression().accept(this, coreClassExpr.getExpr());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return new Pair<>(coreExpr, expr);
    throw new IllegalStateException("BinOpSequence shouldn't appear");
  }
}
