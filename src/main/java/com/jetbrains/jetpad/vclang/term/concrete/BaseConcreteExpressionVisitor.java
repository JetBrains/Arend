package com.jetbrains.jetpad.vclang.term.concrete;

import java.util.Collection;
import java.util.List;

public class BaseConcreteExpressionVisitor<P> implements ConcreteExpressionVisitor<P, Concrete.Expression>, ConcreteDefinitionVisitor<P, Void> {
  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, P params) {
    // It is important that we process arguments first since setFunction modifies the list of arguments.
    for (Concrete.Argument argument : expr.getArguments()) {
      argument.expression = argument.expression.accept(this, null);
    }
    expr.setFunction(expr.getFunction().accept(this, null));
    return expr;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, P params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitInferenceReference(Concrete.InferenceReferenceExpression expr, P params) {
    return expr;
  }

  protected void visitParameter(Concrete.Parameter parameter) {
    if (parameter instanceof Concrete.TypeParameter) {
      ((Concrete.TypeParameter) parameter).type = ((Concrete.TypeParameter) parameter).type.accept(this, null);
    }
  }

  public void visitParameters(List<? extends Concrete.Parameter> parameters) {
    for (Concrete.Parameter parameter : parameters) {
      visitParameter(parameter);
    }
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, P params) {
    visitParameters(expr.getParameters());
    expr.body = expr.body.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, P params) {
    visitParameters(expr.getParameters());
    expr.codomain = expr.codomain.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, P params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, P params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, P params) {
    if (expr.expression != null) {
      expr.expression = expr.expression.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, P params) {
    for (int i = 0; i < expr.getFields().size(); i++) {
      expr.getFields().set(i, expr.getFields().get(i).accept(this, null));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, P params) {
    visitParameters(expr.getParameters());
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, P params) {
    if (expr.getSequence().size() == 1) {
      return expr.getSequence().get(0).expression.accept(this, null);
    }

    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      elem.expression = elem.expression.accept(this, null);
    }
    return expr;
  }

  protected void visitClause(Concrete.FunctionClause clause) {
    if (clause.expression != null) {
      clause.expression = clause.expression.accept(this, null);
    }
  }

  public void visitClauses(Collection<? extends Concrete.FunctionClause> clauses) {
    for (Concrete.FunctionClause clause : clauses) {
      visitClause(clause);
    }
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, P params) {
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      expr.getExpressions().set(i, expr.getExpressions().get(i).accept(this, null));
    }
    visitClauses(expr.getClauses());
    return expr;
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, P params) {
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, P params) {
    expr.baseClassExpression = expr.baseClassExpression.accept(this, null);
    for (Concrete.ClassFieldImpl classFieldImpl : expr.getStatements()) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, P params) {
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, P params) {
    for (Concrete.LetClause clause : expr.getClauses()) {
      if (clause.resultType != null) {
        clause.resultType = clause.resultType.accept(this, null);
      }
      clause.term = clause.term.accept(this, null);
    }
    expr.expression = expr.expression.accept(this, null);
    return expr;
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, P params) {
    return expr;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, P params) {
    visitParameters(def.getParameters());

    if (def.getResultType() != null) {
      def.setResultType(def.getResultType().accept(this, null));
    }

    Concrete.FunctionBody body = def.getBody();
    if (body instanceof Concrete.TermFunctionBody) {
      ((Concrete.TermFunctionBody) body).setTerm(((Concrete.TermFunctionBody) body).getTerm().accept(this, null));
    }
    if (body instanceof Concrete.ElimFunctionBody) {
      visitClauses(((Concrete.ElimFunctionBody) body).getClauses());
    }

    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, P params) {
    visitParameters(def.getParameters());
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        visitParameters(constructor.getParameters());
        visitClauses(constructor.getClauses());
      }
    }
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, P params) {
    for (Concrete.ClassField field : def.getFields()) {
      field.setResultType(field.getResultType().accept(this, null));
    }
    for (Concrete.ClassFieldImpl classFieldImpl : def.getImplementations()) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, P params) {
    visitParameters(def.getParameters());
    for (Concrete.ClassFieldImpl classFieldImpl : def.getClassFieldImpls()) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
    }
    def.setResultType(def.getResultType().accept(this, params));
    return null;
  }
}
