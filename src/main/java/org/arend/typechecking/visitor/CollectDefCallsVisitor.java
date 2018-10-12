package org.arend.typechecking.visitor;

import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionVisitor;

import java.util.Collection;
import java.util.List;

public class CollectDefCallsVisitor implements ConcreteExpressionVisitor<Void, Void> {
  private final Collection<TCReferable> myDependencies;

  public CollectDefCallsVisitor(Collection<TCReferable> dependencies) {
    myDependencies = dependencies;
  }

  public Collection<TCReferable> getDependencies() {
    return myDependencies;
  }

  @Override
  public Void visitApp(Concrete.AppExpression expr, Void ignore) {
    expr.getFunction().accept(this, null);
    for (Concrete.Argument argument : expr.getArguments()) {
      argument.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void ignore) {
    if (expr.getReferent() instanceof TCReferable) {
      TCReferable ref = ((TCReferable) expr.getReferent()).getUnderlyingTypecheckable();
      if (ref != null) {
        myDependencies.add(ref);
      }
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitLam(Concrete.LamExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    expr.getBody().accept(this, null);
    return null;
  }

  private void visitParameters(List<? extends Concrete.Parameter> params) {
    for (Concrete.Parameter param : params) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).getType().accept(this, null);
      }
    }
  }

  @Override
  public Void visitPi(Concrete.PiExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitUniverse(Concrete.UniverseExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitHole(Concrete.HoleExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression expr, Void ignore) {
    for (Concrete.Expression comp : expr.getFields()) {
      comp.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    return null;
  }

  @Override
  public Void visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void ignore) {
    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      elem.expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitCase(Concrete.CaseExpression expr, Void ignore) {
    for (Concrete.CaseArgument caseArg : expr.getArguments()) {
      caseArg.expression.accept(this, null);
      if (caseArg.type != null) {
        caseArg.type.accept(this, null);
      }
    }
    if (expr.getResultType() != null) {
      expr.getResultType().accept(this, null);
    }
    for (Concrete.FunctionClause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitProj(Concrete.ProjExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression expr, Void ignore) {
    expr.getBaseClassExpression().accept(this, null);
    visitClassFieldImpls(expr.getStatements());
    return null;
  }

  public void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls) {
    for (Concrete.ClassFieldImpl classFieldImpl : classFieldImpls) {
      if (classFieldImpl.implementation != null) {
        classFieldImpl.implementation.accept(this, null);
      }
      visitClassFieldImpls(classFieldImpl.subClassFieldImpls);
    }
  }

  @Override
  public Void visitNew(Concrete.NewExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Concrete.LetExpression letExpression, Void ignore) {
    for (Concrete.LetClause clause : letExpression.getClauses()) {
      visitParameters(clause.getParameters());
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, null);
      }
      clause.getTerm().accept(this, null);
    }
    letExpression.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitTyped(Concrete.TypedExpression expr, Void params) {
    expr.expression.accept(this, null);
    expr.type.accept(this, null);
    return null;
  }
}
