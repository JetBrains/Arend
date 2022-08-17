package org.arend.typechecking.visitor;

import org.arend.term.concrete.*;

import java.util.List;

public class VoidConcreteVisitor<P> implements ConcreteExpressionVisitor<P,Void>, ConcreteResolvableDefinitionVisitor<P,Void>, ConcreteLevelExpressionVisitor<P,Void>  {
  protected void visitFunctionHeader(Concrete.BaseFunctionDefinition def, P params) {
    visitParameters(def.getParameters(), params);
    if (def.getResultType() != null) {
      def.getResultType().accept(this, params);
    }
    if (def.getResultTypeLevel() != null) {
      def.getResultTypeLevel().accept(this, params);
    }
  }

  protected Void visitFunctionBody(Concrete.BaseFunctionDefinition def, P params) {
    Concrete.FunctionBody body = def.getBody();
    if (body instanceof Concrete.TermFunctionBody) {
      ((Concrete.TermFunctionBody) body).getTerm().accept(this, params);
    }
    visitElements(body.getCoClauseElements(), params);
    visitClauses(body.getClauses(), params);
    return null;
  }

  @Override
  public Void visitFunction(Concrete.BaseFunctionDefinition def, P params) {
    visitFunctionHeader(def, params);
    return visitFunctionBody(def, params);
  }

  @Override
  public Void visitMeta(DefinableMetaDefinition def, P params) {
    visitParameters(def.getParameters(), params);
    if (def.body != null) {
      def.body.accept(this, params);
    }
    return null;
  }

  protected void visitDataHeader(Concrete.DataDefinition def, P params) {
    visitParameters(def.getParameters(), params);
    Concrete.Expression universe = def.getUniverse();
    if (universe != null) {
      universe.accept(this, params);
    }
  }

  protected void visitDataBody(Concrete.DataDefinition def, P params) {
    visitClauses(def.getConstructorClauses(), params);
    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        visitConstructor(constructor, params);
      }
    }
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, P params) {
    visitDataHeader(def, params);
    visitDataBody(def, params);
    return null;
  }

  protected void visitConstructor(Concrete.Constructor def, P params) {
    visitParameters(def.getParameters(), params);
    if (def.getResultType() != null) {
      def.getResultType().accept(this, params);
    }
    if (!def.getEliminatedReferences().isEmpty()) {
      visitClauses(def.getClauses(), params);
    }
  }

  protected void visitClassHeader(Concrete.ClassDefinition def, P params) {
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      visitReference(superClass, params);
    }
  }

  protected void visitClassBody(Concrete.ClassDefinition def, P params) {
    visitElements(def.getElements(), params);
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, P params) {
    visitClassHeader(def, params);
    visitClassBody(def, params);
    return null;
  }

  protected void visitClassField(Concrete.ClassField field, P params) {
    visitParameters(field.getParameters(), params);
    field.getResultType().accept(this, params);
    if (field.getResultTypeLevel() != null) {
      field.getResultTypeLevel().accept(this, params);
    }
  }

  @Override
  public Void visitApp(Concrete.AppExpression expr, P params) {
    expr.getFunction().accept(this, params);
    for (Concrete.Argument argument : expr.getArguments()) {
      argument.getExpression().accept(this, params);
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

  protected void visitParameters(List<? extends Concrete.Parameter> parameters, P params) {
    for (Concrete.Parameter parameter : parameters) {
      if (parameter.getType() != null) {
        parameter.getType().accept(this, params);
      }
    }
  }

  @Override
  public Void visitLam(Concrete.LamExpression expr, P params) {
    visitParameters(expr.getParameters(), params);
    if (expr instanceof Concrete.PatternLamExpression) {
      for (Concrete.Pattern pattern : ((Concrete.PatternLamExpression) expr).getPatterns()) {
        if (pattern != null) visitPattern(pattern, params);
      }
    }
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
  public Void visitApplyHole(Concrete.ApplyHoleExpression expr, P params) {
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
    for (Concrete.BinOpSequenceElem<Concrete.Expression> elem : expr.getSequence()) {
      elem.getComponent().accept(this, params);
    }
    if (expr.getClauses() != null) {
      visitClauses(expr.getClauseList(), params);
    }
    return null;
  }

  protected void visitPattern(Concrete.Pattern pattern, P params) {
    if (pattern.getAsReferable() != null) {
      if (pattern.getAsReferable().type != null) {
        pattern.getAsReferable().type.accept(this, params);
      }
    }

    if (pattern instanceof Concrete.NamePattern) {
      Concrete.Expression type = ((Concrete.NamePattern) pattern).type;
      if (type != null) {
        type.accept(this, params);
      }
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      for (Concrete.Pattern patternArg : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
        visitPattern(patternArg, params);
      }
    } else if (pattern instanceof Concrete.TuplePattern) {
      for (Concrete.Pattern patternArg : ((Concrete.TuplePattern) pattern).getPatterns()) {
        visitPattern(patternArg, params);
      }
    }
  }

  protected void visitClauses(List<? extends Concrete.Clause> clauses, P params) {
    for (Concrete.Clause clause : clauses) {
      if (clause.getPatterns() != null) {
        for (Concrete.Pattern pattern : clause.getPatterns()) {
          visitPattern(pattern, params);
        }
      }
      if (clause.getExpression() != null) {
        clause.getExpression().accept(this, params);
      }
    }
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
    visitClauses(expr.getClauses(), params);
    return null;
  }

  @Override
  public Void visitEval(Concrete.EvalExpression expr, P params) {
    expr.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitProj(Concrete.ProjExpression expr, P params) {
    expr.getExpression().accept(this, params);
    return null;
  }

  protected void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, P params) {
    if (classFieldImpl.implementation != null) {
      classFieldImpl.implementation.accept(this, params);
    }
    visitElements(classFieldImpl.getSubCoclauseList(), params);
  }

  protected void visitElements(List<? extends Concrete.ClassElement> elements, P params) {
    for (Concrete.ClassElement element : elements) {
      if (element instanceof Concrete.ClassField) {
        visitClassField((Concrete.ClassField) element, params);
      } else if (element instanceof Concrete.ClassFieldImpl) {
        visitClassFieldImpl((Concrete.ClassFieldImpl) element, params);
      } else if (element instanceof Concrete.OverriddenField) {
        Concrete.OverriddenField field = (Concrete.OverriddenField) element;
        visitParameters(field.getParameters(), params);
        field.getResultType().accept(this, params);
        if (field.getResultTypeLevel() != null) {
          field.getResultTypeLevel().accept(this, params);
        }
      } else {
        throw new IllegalStateException();
      }
    }
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression expr, P params) {
    expr.getBaseClassExpression().accept(this, params);
    visitElements(expr.getStatements(), params);
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
      visitPattern(clause.getPattern(), params);
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
  public Void visitStringLiteral(Concrete.StringLiteral expr, P params) {
    return null;
  }

  @Override
  public Void visitTyped(Concrete.TypedExpression expr, P params) {
    expr.expression.accept(this, params);
    expr.type.accept(this, params);
    return null;
  }

  @Override
  public Void visitInf(Concrete.InfLevelExpression expr, P param) {
    return null;
  }

  @Override
  public Void visitLP(Concrete.PLevelExpression expr, P param) {
    return null;
  }

  @Override
  public Void visitLH(Concrete.HLevelExpression expr, P param) {
    return null;
  }

  @Override
  public Void visitNumber(Concrete.NumberLevelExpression expr, P param) {
    return null;
  }

  @Override
  public Void visitId(Concrete.IdLevelExpression expr, P param) {
    return null;
  }

  @Override
  public Void visitSuc(Concrete.SucLevelExpression expr, P param) {
    return expr.getExpression().accept(this, param);
  }

  @Override
  public Void visitMax(Concrete.MaxLevelExpression expr, P param) {
    expr.getLeft().accept(this, param);
    expr.getRight().accept(this, param);
    return null;
  }

  @Override
  public Void visitVar(Concrete.VarLevelExpression expr, P param) {
    return null;
  }
}
