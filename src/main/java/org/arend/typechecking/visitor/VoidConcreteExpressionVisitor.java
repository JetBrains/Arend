package org.arend.typechecking.visitor;

import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;

import java.util.List;

public class VoidConcreteExpressionVisitor<P> implements ConcreteExpressionVisitor<P,Void> {
  @Override
  public Void visitApp(Concrete.AppExpression expr, P params) {
    expr.getFunction().accept(this, params);
    for (Concrete.Argument argument : expr.getArguments()) {
      argument.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, P params) {
    return null;
  }

  @Override
  public Void visitThis(Concrete.ThisExpression expr, P params) {
    return null;
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, P params) {
    return null;
  }

  protected void visitParameters(List<? extends Concrete.Parameter> parameters, P params) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) parameter).getType().accept(this, params);
      }
    }
  }

  @Override
  public Void visitLam(Concrete.LamExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    expr.getBody().accept(this, params);
    return null;
  }

  @Override
  public Void visitPi(Concrete.PiExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    expr.getCodomain().accept(this, params);
    return null;
  }

  @Override
  public Void visitUniverse(Concrete.UniverseExpression expr, P params) {
    return null;
  }

  @Override
  public Void visitHole(Concrete.HoleExpression expr, P params) {
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, P params) {
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression expr, P params) {
    for (Concrete.Expression comp : expr.getFields()) {
      comp.accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    return null;
  }

  @Override
  public Void visitBinOpSequence(Concrete.BinOpSequenceExpression expr, P params) {
    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      elem.expression.accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitCase(Concrete.CaseExpression expr, P params) {
    for (Concrete.CaseArgument caseArg : expr.getArguments()) {
      caseArg.expression.accept(this, params);
      if (caseArg.type != null) {
        caseArg.type.accept(this, params);
      }
    }
    if (expr.getResultType() != null) {
      expr.getResultType().accept(this, params);
    }
    if (expr.getResultTypeLevel() != null) {
      expr.getResultTypeLevel().accept(this, params);
    }
    for (Concrete.FunctionClause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, params);
    }
    return null;
  }

  @Override
  public Void visitProj(Concrete.ProjExpression expr, P params) {
    expr.getExpression().accept(this, params);
    return null;
  }

  protected void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls, P params) {
    for (Concrete.ClassFieldImpl classFieldImpl : classFieldImpls) {
      if (classFieldImpl.implementation != null) {
        classFieldImpl.implementation.accept(this, params);
      }
      visitClassFieldImpls(classFieldImpl.subClassFieldImpls, params);
    }
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression expr, P params) {
    expr.getBaseClassExpression().accept(this, params);
    visitClassFieldImpls(expr.getStatements(), params);
    return null;
  }

  @Override
  public Void visitNew(Concrete.NewExpression expr, P params) {
    expr.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitLet(Concrete.LetExpression expr, P params) {
    for (Concrete.LetClause clause : expr.getClauses()) {
      visitParameters(clause.getParameters(), params);
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, params);
      }
      clause.getTerm().accept(this, params);
    }
    expr.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, P params) {
    return null;
  }

  @Override
  public Void visitTyped(Concrete.TypedExpression expr, P params) {
    expr.expression.accept(this, params);
    expr.type.accept(this, params);
    return null;
  }
}
