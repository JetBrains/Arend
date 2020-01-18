package org.arend.typechecking.visitor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CorrespondedSubExprVisitor implements ConcreteExpressionVisitor<Expression, Expression> {
  private final Concrete.Expression subExpr;

  public CorrespondedSubExprVisitor(Concrete.Expression subExpr) {
    this.subExpr = subExpr;
  }

  private boolean matchesSubExpr(Concrete.Expression expr) {
    return Objects.equals(expr.getData(), subExpr.getData());
  }

  private Expression atomicExpr(Concrete.Expression expr, Expression coreExpr) {
    return matchesSubExpr(expr) ? coreExpr : null;
  }

  @Override
  public Expression visitHole(Concrete.HoleExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Expression visitReference(Concrete.ReferenceExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Expression visitThis(Concrete.ThisExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Expression visitInferenceReference(Concrete.InferenceReferenceExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Expression visitNumericLiteral(Concrete.NumericLiteral expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Expression visitUniverse(Concrete.UniverseExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Expression visitGoal(Concrete.GoalExpression expr, Expression coreExpr) {
    return atomicExpr(expr, coreExpr);
  }

  @Override
  public Expression visitProj(Concrete.ProjExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    ProjExpression coreProjExpr = coreExpr.cast(ProjExpression.class);
    if (coreProjExpr == null) return null;
    return expr.getExpression().accept(this, coreProjExpr.getExpression());
  }

  @Override
  public Expression visitNew(Concrete.NewExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    NewExpression coreNewExpr = coreExpr.cast(NewExpression.class);
    if (coreNewExpr == null) return null;
    return expr.getExpression().accept(this, coreNewExpr.getClassCall());
  }

  @Override
  public Expression visitTuple(Concrete.TupleExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    TupleExpression coreTupleExpr = coreExpr.cast(TupleExpression.class);
    if (coreTupleExpr == null) return null;
    return visitExprs(coreTupleExpr.getFields(), expr.getFields());
  }

  private Expression visitExprs(List<? extends Expression> coreExpr, List<? extends Concrete.Expression> expr) {
    for (int i = 0; i < expr.size(); i++) {
      Expression accepted = expr.get(i).accept(this, coreExpr.get(i));
      if (accepted != null) return accepted;
    }
    return null;
  }

  @Override
  public Expression visitLet(Concrete.LetExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    LetExpression coreLetExpr = coreExpr.cast(LetExpression.class);
    if (coreLetExpr == null) return null;
    List<Concrete.LetClause> exprClauses = expr.getClauses();
    List<LetClause> coreClauses = coreLetExpr.getClauses();
    for (int i = 0; i < exprClauses.size(); i++) {
      LetClause coreLetClause = coreClauses.get(i);
      Concrete.LetClause exprLetClause = exprClauses.get(i);

      Expression accepted = exprLetClause.getTerm().accept(this, coreLetClause.getExpression());
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
  public Expression visitTyped(Concrete.TypedExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    return expr.expression.accept(this, coreExpr);
  }

  @Override
  public Expression visitApp(Concrete.AppExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    AppExpression coreAppExpr = coreExpr.cast(AppExpression.class);
    if (coreAppExpr == null) return null;
    List<Expression> coreArguments = new ArrayList<>();
    coreAppExpr.getArguments(coreArguments);
    return visitExprs(coreArguments, expr
        .getArguments()
        .stream()
        .map(Concrete.Argument::getExpression)
        .collect(Collectors.toList()));
  }

  @Override
  public Expression visitLam(Concrete.LamExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    Expression body = coreExpr;
    for (Concrete.Parameter parameter : expr.getParameters()) {
      if (body instanceof LamExpression) {
        LamExpression coreLamExpr = (LamExpression) body;
        Concrete.Expression type = parameter.getType();
        if (type != null) {
          Expression ty = type.accept(this, coreLamExpr.getParameters().getTypeExpr());
          if (ty != null) return ty;
        }
        body = coreLamExpr.getBody();
      } else return null;
    }
    return expr.getBody().accept(this, body);
  }

  protected Expression visitParameter(Concrete.Parameter parameter, DependentLink link) {
    Concrete.Expression type = parameter.getType();
    if (type == null) return null;
    return type.accept(this, link.getTypeExpr());
  }

  protected Expression visitPiParameters(List<? extends Concrete.Parameter> parameters, PiExpression pi) {
    for (Concrete.Parameter parameter : parameters) {
      DependentLink link = pi.getParameters();
      Expression expression = visitParameter(parameter, link);
      if (expression != null) return expression;
      Expression codomain = pi.getCodomain();
      if (codomain instanceof PiExpression) pi = (PiExpression) codomain;
      else return null;
    }
    return null;
  }

  protected Expression visitSigmaParameters(List<? extends Concrete.Parameter> parameters, DependentLink sig) {
    for (Concrete.Parameter parameter : parameters) {
      Expression expression = visitParameter(parameter, sig);
      if (expression != null) return expression;
      sig = sig.getNextTyped(null).getNext();
    }
    return null;
  }

  @Override
  public Expression visitPi(Concrete.PiExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    PiExpression corePiExpr = coreExpr.cast(PiExpression.class);
    if (corePiExpr == null) return null;
    Expression expression = visitPiParameters(expr.getParameters(), corePiExpr);
    if (expression != null) return expression;
    return expr.getCodomain().accept(this, corePiExpr.getCodomain());
  }

  @Override
  public Expression visitSigma(Concrete.SigmaExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    SigmaExpression coreSigmaExpr = coreExpr.cast(SigmaExpression.class);
    if (coreSigmaExpr == null) return null;
    return visitSigmaParameters(expr.getParameters(), coreSigmaExpr.getParameters());
  }

  @Override
  public Expression visitCase(Concrete.CaseExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    CaseExpression coreCaseExpr = coreExpr.cast(CaseExpression.class);
    if (coreCaseExpr == null) return null;
    Expression expression = visitExprs(coreCaseExpr.getArguments(), expr
        .getArguments()
        .stream()
        .map(i -> i.expression)
        .collect(Collectors.toList()));
    if (expression != null) return expression;
    Concrete.Expression resultType = expr.getResultType();
    if (resultType != null) {
      Expression accepted = resultType.accept(this, coreCaseExpr.getResultType());
      if (accepted != null) return accepted;
    }
    // Case trees and clauses? They are unlikely to be isomorphic.
    return null;
  }

  @Override
  public Expression visitEval(Concrete.EvalExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    throw new IllegalStateException("Eval shouldn't appear");
  }

  @Override
  public Expression visitClassExt(Concrete.ClassExtExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    ClassCallExpression coreClassExpr = coreExpr.cast(ClassCallExpression.class);
    if (coreClassExpr == null) return null;
    // How about the other subexpressions?
    return expr.getBaseClassExpression().accept(this, coreClassExpr.getExpr());
  }

  @Override
  public Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Expression coreExpr) {
    if (matchesSubExpr(expr)) return coreExpr;
    throw new IllegalStateException("BinOpSequence shouldn't appear");
  }
}
