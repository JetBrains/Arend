package org.arend.term.concrete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BaseConcreteExpressionVisitor<P> implements ConcreteExpressionVisitor<P, Concrete.Expression>, ConcreteResolvableDefinitionVisitor<P, Void> {
  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, P params) {
    // It is important that we process arguments first since setFunction modifies the list of arguments.
    for (Concrete.Argument argument : expr.getArguments()) {
      argument.expression = argument.expression.accept(this, params);
    }
    expr.setFunction(expr.getFunction().accept(this, params));
    return expr;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, P params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitThis(Concrete.ThisExpression expr, P params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitInferenceReference(Concrete.InferenceReferenceExpression expr, P params) {
    return expr;
  }

  protected void visitParameter(Concrete.Parameter parameter, P params) {
    if (parameter instanceof Concrete.TypeParameter) {
      ((Concrete.TypeParameter) parameter).type = ((Concrete.TypeParameter) parameter).type.accept(this, params);
    }
  }

  public void visitParameters(List<? extends Concrete.Parameter> parameters, P params) {
    for (Concrete.Parameter parameter : parameters) {
      visitParameter(parameter, params);
    }
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    expr.body = expr.body.accept(this, params);
    return expr;
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    expr.codomain = expr.codomain.accept(this, params);
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
  public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, P params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, P params) {
    if (expr.expression != null) {
      expr.expression = expr.expression.accept(this, params);
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, P params) {
    for (int i = 0; i < expr.getFields().size(); i++) {
      expr.getFields().set(i, expr.getFields().get(i).accept(this, params));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, P params) {
    if (expr.getSequence().size() == 1 && expr.getClauses() == null) {
      return expr.getSequence().get(0).expression.accept(this, params);
    }

    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      elem.expression = elem.expression.accept(this, params);
    }
    if (expr.getClauses() != null) {
      visitClauses(expr.getClauseList(), params);
    }
    return expr;
  }

  protected void visitPattern(Concrete.Pattern pattern, P params) {
    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      if (namePattern.type != null) {
        namePattern.type = namePattern.type.accept(this, params);
      }
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      for (Concrete.Pattern subPattern : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
        visitPattern(subPattern, params);
      }
    } else if (pattern instanceof Concrete.TuplePattern) {
      for (Concrete.Pattern subPattern : ((Concrete.TuplePattern) pattern).getPatterns()) {
        visitPattern(subPattern, params);
      }
    }

    for (Concrete.TypedReferable typedReferable : pattern.getAsReferables()) {
      if (typedReferable.type != null) {
        typedReferable.type = typedReferable.type.accept(this, params);
      }
    }
  }

  protected void visitClause(Concrete.Clause clause, P params) {
    if (clause.getPatterns() != null) {
      for (Concrete.Pattern pattern : clause.getPatterns()) {
        visitPattern(pattern, params);
      }
    }
    if (clause instanceof Concrete.FunctionClause && ((Concrete.FunctionClause) clause).expression != null) {
      ((Concrete.FunctionClause) clause).expression = ((Concrete.FunctionClause) clause).expression.accept(this, params);
    }
  }

  public void visitClauses(Collection<? extends Concrete.FunctionClause> clauses, P params) {
    for (Concrete.FunctionClause clause : clauses) {
      visitClause(clause, params);
    }
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, P params) {
    for (Concrete.CaseArgument caseArg : expr.getArguments()) {
      caseArg.expression = caseArg.expression.accept(this, params);
      if (caseArg.type != null) {
        caseArg.type = caseArg.type.accept(this, params);
      }
    }
    if (expr.getResultType() != null) {
      expr.setResultType(expr.getResultType().accept(this, params));
    }
    if (expr.getResultTypeLevel() != null) {
      expr.setResultTypeLevel(expr.getResultTypeLevel().accept(this, params));
    }
    visitClauses(expr.getClauses(), params);
    return expr;
  }

  @Override
  public Concrete.Expression visitEval(Concrete.EvalExpression expr, P params) {
    expr.setExpression(expr.getExpression().accept(this, params));
    return expr;
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, P params) {
    expr.expression = expr.expression.accept(this, params);
    return expr;
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, P params) {
    expr.setBaseClassExpression(expr.getBaseClassExpression().accept(this, params));
    visitClassElements(expr.getStatements(), params);
    return expr;
  }

  private void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, P params) {
    if (classFieldImpl.implementation != null) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, params);
    }
    visitClassElements(classFieldImpl.getSubCoclauseList(), params);
  }

  // except for fields
  protected void visitClassElement(Concrete.ClassElement element, P params) {
    if (element instanceof Concrete.ClassFieldImpl) {
      visitClassFieldImpl((Concrete.ClassFieldImpl) element, params);
    } else if (element instanceof Concrete.OverriddenField) {
      Concrete.OverriddenField field = (Concrete.OverriddenField) element;
      visitParameters(field.getParameters(), params);
      field.setResultType(field.getResultType().accept(this, params));
      if (field.getResultTypeLevel() != null) {
        field.setResultTypeLevel(field.getResultTypeLevel().accept(this, params));
      }
    }
  }

  // except for fields
  protected <T extends Concrete.ClassElement> void visitClassElements(List<T> elements, P params) {
    for (Concrete.ClassElement element : elements) {
      visitClassElement(element, params);
    }
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, P params) {
    expr.expression = expr.expression.accept(this, params);
    return expr;
  }

  private void visitLetClause(Concrete.LetClause clause, P params) {
    visitParameters(clause.getParameters(), params);
    if (clause.resultType != null) {
      clause.resultType = clause.resultType.accept(this, params);
    }
    clause.term = clause.term.accept(this, params);
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, P params) {
    for (Concrete.LetClause clause : expr.getClauses()) {
      visitLetClause(clause, params);
    }
    expr.expression = expr.expression.accept(this, params);
    return expr;
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, P params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitStringLiteral(Concrete.StringLiteral expr, P params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitTyped(Concrete.TypedExpression expr, P params) {
    expr.expression = expr.expression.accept(this, params);
    expr.type = expr.type.accept(this, params);
    return expr;
  }

  private void visitFunctionBody(Concrete.FunctionBody body, P params) {
    if (body instanceof Concrete.TermFunctionBody) {
      ((Concrete.TermFunctionBody) body).setTerm(((Concrete.TermFunctionBody) body).getTerm().accept(this, params));
    }
    visitClauses(body.getClauses(), params);
    visitClassElements(body.getCoClauseElements(), params);
  }

  @Override
  public Void visitFunction(Concrete.BaseFunctionDefinition def, P params) {
    visitParameters(def.getParameters(), params);

    if (def.getResultType() != null) {
      def.setResultType(def.getResultType().accept(this, params));
    }
    if (def.getResultTypeLevel() != null) {
      def.setResultTypeLevel(def.getResultTypeLevel().accept(this, params));
    }

    visitFunctionBody(def.getBody(), params);
    return null;
  }

  @Override
  public Void visitMeta(DefinableMetaDefinition def, P params) {
    visitParameters(def.getParameters(), params);
    def.body = def.body.accept(this, params);
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, P params) {
    visitParameters(def.getParameters(), params);
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      visitClause(clause, params);
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        visitParameters(constructor.getParameters(), params);
        if (constructor.getResultType() != null) {
          constructor.setResultType(constructor.getResultType().accept(this, params));
        }
        visitClauses(constructor.getClauses(), params);
      }
    }
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, P params) {
    List<Concrete.ClassField> classFields = new ArrayList<>();
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassField) {
        classFields.add((Concrete.ClassField) element);
      }
    }

    Concrete.Expression previousType = null;
    for (int i = 0; i < classFields.size(); i++) {
      Concrete.ClassField field = classFields.get(i);
      Concrete.Expression fieldType = field.getResultType();
      if (fieldType == previousType && field.getParameters().isEmpty()) {
        field.setResultType(classFields.get(i - 1).getResultType());
        field.setResultTypeLevel(classFields.get(i - 1).getResultTypeLevel());
      } else {
        previousType = field.getParameters().isEmpty() ? fieldType : null;
        visitParameters(field.getParameters(), params);
        field.setResultType(fieldType.accept(this, params));
        if (field.getResultTypeLevel() != null) {
          field.setResultTypeLevel(field.getResultTypeLevel().accept(this, params));
        }
      }
    }
    visitClassElements(def.getElements(), params);
    return null;
  }
}
