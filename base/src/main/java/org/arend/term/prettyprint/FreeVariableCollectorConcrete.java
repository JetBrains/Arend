package org.arend.term.prettyprint;

import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.VoidConcreteVisitor;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FreeVariableCollectorConcrete extends VoidConcreteVisitor<Void> {
  private final Set<Referable> myReferables;

  public FreeVariableCollectorConcrete(Set<Referable> referables) {
    myReferables = referables;
  }


  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (expr instanceof Concrete.LongReferenceExpression && ((Concrete.LongReferenceExpression) expr).getQualifier() != null) {
      Objects.requireNonNull(((Concrete.LongReferenceExpression) expr).getQualifier()).accept(this, null);
    }
    myReferables.add(expr.getReferent());
    return null;
  }

  @Override
  public Void visitLam(Concrete.LamExpression expr, Void params) {
    super.visitLam(expr, params);
    removeParameters(expr.getParameters());
    return null;
  }

  private void removeParameters(List<? extends Concrete.Parameter> parameters) {
    parameters.stream().flatMap(a -> a.getRefList().stream()).collect(Collectors.toList()).forEach(myReferables::remove);
  }

  @Override
  public Void visitPi(Concrete.PiExpression expr, Void params) {
    super.visitPi(expr, params);
    removeParameters(expr.getParameters());
    return null;
  }

  @Override
  public Void visitLet(Concrete.LetExpression expr, Void params) {
    super.visitLet(expr, params);
    expr.getClauses().forEach(clause -> {
      removeParameters(clause.getParameters());
      var typedReferable = clause.getPattern().getAsReferable();
      if (typedReferable != null) {
        myReferables.remove(typedReferable.referable);
      }
    });
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression expr, Void params) {
    super.visitSigma(expr, params);
    removeParameters(expr.getParameters());
    return null;
  }
}
