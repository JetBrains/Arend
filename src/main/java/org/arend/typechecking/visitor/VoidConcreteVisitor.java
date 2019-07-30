package org.arend.typechecking.visitor;

import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.concrete.ConcreteExpressionVisitor;

import java.util.List;

public class VoidConcreteVisitor<P, R> implements ConcreteExpressionVisitor<P,Void>, ConcreteDefinitionVisitor<P,R> {
  @Override
  public R visitFunction(Concrete.FunctionDefinition def, P params) {
    visitParameters(def.getParameters(), params);

    if (def.getResultType() != null) {
      def.getResultType().accept(this, null);
    }
    if (def.getResultTypeLevel() != null) {
      def.getResultTypeLevel().accept(this, null);
    }

    Concrete.FunctionBody body = def.getBody();
    if (body instanceof Concrete.TermFunctionBody) {
      ((Concrete.TermFunctionBody) body).getTerm().accept(this, null);
    }
    visitClassFieldImpls(body.getClassFieldImpls(), null);
    visitClauses(body.getClauses(), null);

    return null;
  }

  @Override
  public R visitData(Concrete.DataDefinition def, P params) {
    visitParameters(def.getParameters(), null);
    Concrete.Expression universe = def.getUniverse();
    if (universe != null) {
      universe.accept(this, null);
    }

    for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
      if (clause.getPatterns() != null) {
        for (Concrete.Pattern pattern : clause.getPatterns()) {
          visitPattern(pattern, null);
        }
      }
      for (Concrete.Constructor constructor : clause.getConstructors()) {
        visitConstructor(constructor);
      }
    }

    return null;
  }

  protected void visitConstructor(Concrete.Constructor def) {
    visitParameters(def.getParameters(), null);
    if (def.getResultType() != null) {
      def.getResultType().accept(this, null);
    }
    if (!def.getEliminatedReferences().isEmpty()) {
      visitClauses(def.getClauses(), null);
    }
  }

  @Override
  public R visitClass(Concrete.ClassDefinition def, P params) {
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      visitReference(superClass, null);
    }

    for (Concrete.ClassField field : def.getFields()) {
      visitClassField(field);
    }

    visitClassFieldImpls(def.getImplementations(), null);
    return null;
  }

  protected void visitClassField(Concrete.ClassField field) {
    visitParameters(field.getParameters(), null);
    field.getResultType().accept(this, null);
    if (field.getResultTypeLevel() != null) {
      field.getResultTypeLevel().accept(this, null);
    }
  }

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

  protected void visitPattern(Concrete.Pattern pattern, P params) {
    for (Concrete.TypedReferable typedReferable : pattern.getAsReferables()) {
      if (typedReferable.type != null) {
        typedReferable.type.accept(this, params);
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

  protected void visitClauses(List<Concrete.FunctionClause> clauses, P params) {
    for (Concrete.FunctionClause clause : clauses) {
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
