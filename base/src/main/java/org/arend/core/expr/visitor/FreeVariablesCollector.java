package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.pattern.Pattern;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FreeVariablesCollector extends VoidExpressionVisitor<Void> {
  private final Set<Binding> myResult = new HashSet<>();

  public Set<Binding> getResult() {
    return myResult;
  }

  public static Set<Binding> getFreeVariables(Expression expression) {
    FreeVariablesCollector collector = new FreeVariablesCollector();
    expression.accept(collector, null);
    return collector.myResult;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    myResult.add(expr.getBinding());
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    visitDefCall(expr, params);
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      entry.getValue().accept(this, params);
    }
    myResult.remove(expr.getThisBinding());
    return null;
  }

  @Override
  public Void visitSubst(SubstExpression expr, Void params) {
    expr.getExpression().accept(this, params);
    myResult.removeAll(expr.getSubstitution().getKeys());

    for (Map.Entry<Binding, Expression> entry : expr.getSubstitution().getEntries()) {
      entry.getValue().accept(this, params);
    }
    return null;
  }

  private void freeParams(DependentLink param) {
    for (; param.hasNext(); param = param.getNext()) {
      myResult.remove(param);
    }
  }

  @Override
  public Void visitLam(LamExpression expr, Void params) {
    super.visitLam(expr, null);
    freeParams(expr.getParameters());
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, Void params) {
    super.visitPi(expr, null);
    freeParams(expr.getParameters());
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Void params) {
    super.visitSigma(expr, null);
    freeParams(expr.getParameters());
    return null;
  }

  @Override
  public Void visitLet(LetExpression expr, Void params) {
    super.visitLet(expr, null);
    for (HaveClause clause : expr.getClauses()) {
      myResult.remove(clause);
    }
    return null;
  }

  @Override
  protected void visitElimBody(ElimBody elimBody, Void params) {
    super.visitElimBody(elimBody, params);
    for (ElimClause<Pattern> clause : elimBody.getClauses()) {
      freeParams(clause.getParameters());
    }
  }

  @Override
  public Void visitCase(CaseExpression expr, Void params) {
    super.visitCase(expr, null);
    freeParams(expr.getParameters());
    return null;
  }
}
