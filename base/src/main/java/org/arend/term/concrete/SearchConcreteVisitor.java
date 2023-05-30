package org.arend.term.concrete;

import java.util.List;

public class SearchConcreteVisitor<P,R> implements ConcreteExpressionVisitor<P,R>, ConcreteDefinitionVisitor<P,R> {
  @Override
  public R visitApp(Concrete.AppExpression expr, P params) {
    for (Concrete.Argument arg : expr.getArguments()) {
      R result = arg.expression.accept(this, params);
      if (result != null) return result;
    }
    return expr.getFunction().accept(this, params);
  }

  @Override
  public R visitReference(Concrete.ReferenceExpression expr, P params) {
    return null;
  }

  @Override
  public R visitThis(Concrete.ThisExpression expr, P params) {
    return null;
  }

  public R visitParameter(Concrete.Parameter parameter, P params) {
    return parameter.getType() == null ? null : parameter.getType().accept(this, params);
  }

  public R visitParameters(List<? extends Concrete.Parameter> parameters, P params) {
    for (Concrete.Parameter parameter : parameters) {
      R result = visitParameter(parameter, params);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public R visitLam(Concrete.LamExpression expr, P params) {
    R result = visitParameters(expr.getParameters(), params);
    return result != null ? result : expr.getBody().accept(this, params);
  }

  @Override
  public R visitPi(Concrete.PiExpression expr, P params) {
    R result = visitParameters(expr.getParameters(), params);
    return result != null ? result : expr.codomain.accept(this, params);
  }

  @Override
  public R visitUniverse(Concrete.UniverseExpression expr, P params) {
    return null;
  }

  @Override
  public R visitHole(Concrete.HoleExpression expr, P params) {
    return null;
  }

  @Override
  public R visitApplyHole(Concrete.ApplyHoleExpression expr, P params) {
    return null;
  }

  @Override
  public R visitCore(Concrete.CoreExpression expr, P params) {
    return null;
  }

  @Override
  public R visitGoal(Concrete.GoalExpression expr, P params) {
    return expr.expression != null ? expr.expression.accept(this, params) : null;
  }

  @Override
  public R visitTuple(Concrete.TupleExpression expr, P params) {
    for (Concrete.Expression field : expr.getFields()) {
      R result = field.accept(this, params);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public R visitSigma(Concrete.SigmaExpression expr, P params) {
    return visitParameters(expr.getParameters(), params);
  }

  @Override
  public R visitBinOpSequence(Concrete.BinOpSequenceExpression expr, P params) {
    for (Concrete.BinOpSequenceElem<Concrete.Expression> elem : expr.getSequence()) {
      R result = elem.getComponent().accept(this, params);
      if (result != null) return result;
    }
    return visitClauses(expr.getClauseList(), params);
  }

  public R visitPatterns(List<? extends Concrete.Pattern> patterns, P params) {
    for (Concrete.Pattern pattern : patterns) {
      R result = visitPattern(pattern, params);
      if (result != null) return result;
    }
    return null;
  }

  public R visitPattern(Concrete.Pattern pattern, P params) {
    if (pattern.getAsReferable() != null && pattern.getAsReferable().type != null) {
      R result = pattern.getAsReferable().type.accept(this, params);
      if (result != null) return result;
    }

    if (pattern instanceof Concrete.NamePattern namePattern) {
      return namePattern.type != null ? namePattern.type.accept(this, params) : null;
    }
    if (pattern instanceof Concrete.ConstructorPattern) {
      return visitPatterns(((Concrete.ConstructorPattern) pattern).getPatterns(), params);
    }
    if (pattern instanceof Concrete.TuplePattern) {
      return visitPatterns(((Concrete.TuplePattern) pattern).getPatterns(), params);
    }
    return null;
  }

  public R visitClause(Concrete.Clause clause, P params) {
    R result = clause.getPatterns() == null ? null : visitPatterns(clause.getPatterns(), params);
    return result != null ? result : clause.getExpression() != null ? clause.getExpression().accept(this, params) : null;
  }

  public R visitClauses(List<? extends Concrete.Clause> clauses, P params) {
    for (Concrete.Clause clause : clauses) {
      R result = visitClause(clause, params);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public R visitCase(Concrete.CaseExpression expr, P params) {
    for (Concrete.CaseArgument arg : expr.getArguments()) {
      R result = arg.expression.accept(this, params);
      if (result != null) return result;
      result = arg.type != null ? arg.type.accept(this, params) : null;
      if (result != null) return result;
    }
    if (expr.getResultType() != null) {
      R result = expr.getResultType().accept(this, params);
      if (result != null) return result;
    }
    if (expr.getResultTypeLevel() != null) {
      R result = expr.getResultTypeLevel().accept(this, params);
      if (result != null) return result;
    }
    return visitClauses(expr.getClauses(), params);
  }

  @Override
  public R visitEval(Concrete.EvalExpression expr, P params) {
    return expr.getExpression().accept(this, params);
  }

  @Override
  public R visitBox(Concrete.BoxExpression expr, P params) {
    return expr.getExpression().accept(this, params);
  }

  @Override
  public R visitProj(Concrete.ProjExpression expr, P params) {
    return expr.expression.accept(this, params);
  }

  public R visitClassFieldImpl(Concrete.ClassFieldImpl fieldImpl, P params) {
    R result = fieldImpl.implementation == null ? null : fieldImpl.implementation.accept(this, params);
    if (result != null) return result;
    for (Concrete.ClassFieldImpl subFieldImpl : fieldImpl.getSubCoclauseList()) {
      result = visitClassFieldImpl(subFieldImpl, params);
      if (result != null) return result;
    }
    return null;
  }

  @Override
  public R visitClassExt(Concrete.ClassExtExpression expr, P params) {
    for (Concrete.ClassFieldImpl fieldImpl : expr.getStatements()) {
      R result = visitClassFieldImpl(fieldImpl, params);
      if (result != null) return result;
    }
    return expr.getBaseClassExpression().accept(this, params);
  }

  @Override
  public R visitNew(Concrete.NewExpression expr, P params) {
    return expr.expression.accept(this, params);
  }

  @Override
  public R visitLet(Concrete.LetExpression expr, P params) {
    for (Concrete.LetClause clause : expr.getClauses()) {
      R result = visitPattern(clause.getPattern(), params);
      if (result != null) return result;
      result = visitParameters(clause.getParameters(), params);
      if (result != null) return result;
      if (clause.resultType != null) {
        result = clause.resultType.accept(this, params);
        if (result != null) return result;
      }
      result = clause.term.accept(this, params);
      if (result != null) return result;
    }
    return expr.expression.accept(this, params);
  }

  @Override
  public R visitNumericLiteral(Concrete.NumericLiteral expr, P params) {
    return null;
  }

  @Override
  public R visitStringLiteral(Concrete.StringLiteral expr, P params) {
    return null;
  }

  @Override
  public R visitQNameLiteral(Concrete.QNameLiteral expr, P params) {
    return null;
  }

  @Override
  public R visitTyped(Concrete.TypedExpression expr, P params) {
    R result = expr.expression.accept(this, params);
    return result != null ? result : expr.type.accept(this, params);
  }

  @Override
  public R visitFunction(Concrete.BaseFunctionDefinition def, P params) {
    R result = visitParameters(def.getParameters(), params);
    if (result != null) return result;
    if (def.getResultType() != null) {
      result = def.getResultType().accept(this, params);
      if (result != null) return result;
    }
    if (def.getResultTypeLevel() != null) {
      result = def.getResultTypeLevel().accept(this, params);
      if (result != null) return result;
    }
    Concrete.FunctionBody body = def.getBody();
    if (body instanceof Concrete.TermFunctionBody) {
      result = ((Concrete.TermFunctionBody) body).getTerm().accept(this, params);
      if (result != null) return result;
    }
    result = visitClauses(body.getClauses(), params);
    if (result != null) return result;
    return visitClassElements(body.getCoClauseElements(), params);
  }

  protected R visitClassElements(List<? extends Concrete.ClassElement> elements, P params) {
    for (Concrete.ClassElement element : elements) {
      if (element instanceof Concrete.ClassField field) {
        R result = visitParameters(field.getParameters(), params);
        if (result != null) return null;
        result = field.getResultType().accept(this, params);
        if (result != null) return null;
        if (field.getResultTypeLevel() != null) {
          result = field.getResultTypeLevel().accept(this, params);
          if (result != null) return null;
        }
      } else if (element instanceof Concrete.ClassFieldImpl) {
        return visitClassFieldImpl((Concrete.ClassFieldImpl) element, params);
      } else if (element instanceof Concrete.OverriddenField field) {
        R result = visitParameters(field.getParameters(), params);
        if (result != null) return null;
        result = field.getResultType().accept(this, params);
        if (result != null) return null;
        if (field.getResultTypeLevel() != null) {
          result = field.getResultTypeLevel().accept(this, params);
          if (result != null) return null;
        }
      } else {
        throw new IllegalStateException();
      }
    }
    return null;
  }

  @Override
  public R visitData(Concrete.DataDefinition def, P params) {
    R result = visitParameters(def.getParameters(), params);
    if (result != null) return result;
    if (def.getUniverse() != null) {
      result = def.getUniverse().accept(this, params);
      if (result != null) return result;
    }
    visitClauses(def.getConstructorClauses(), params);
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        result = visitParameters(constructor.getParameters(), params);
        if (result != null) return result;
        if (constructor.getResultType() != null) {
          result = constructor.getResultType().accept(this, params);
          if (result != null) return result;
        }
        if (!constructor.getEliminatedReferences().isEmpty()) {
          result = visitClauses(constructor.getClauses(), params);
          if (result != null) return result;
        }
      }
    }
    return null;
  }

  @Override
  public R visitClass(Concrete.ClassDefinition def, P params) {
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      R result = visitReference(superClass, params);
      if (result != null) return result;
    }
    return visitClassElements(def.getElements(), params);
  }
}
