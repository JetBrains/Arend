package org.arend.core.subst;

import org.arend.core.constructor.ClassConstructor;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.BranchKey;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.ext.core.level.LevelSubstitution;

import java.util.Map;

public class InPlaceLevelSubstVisitor extends VoidExpressionVisitor<Void> {
  private final LevelSubstitution mySubstitution;

  public InPlaceLevelSubstVisitor(LevelSubstitution levelSubstitution) {
    mySubstitution = levelSubstitution;
  }

  public LevelSubstitution getLevelSubstitution() {
    return mySubstitution;
  }

  public boolean isEmpty() {
    return mySubstitution.isEmpty();
  }

  @Override
  public void visitParameters(DependentLink link, Void params) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      link.getType().subst(this);
    }
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    if (expr.getBinding() instanceof EvaluatingBinding) {
      expr.getBinding().subst(this);
    }
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    if (expr instanceof LeveledDefCallExpression) {
      ((LeveledDefCallExpression) expr).substSort(mySubstitution);
    }
    super.visitDefCall(expr, null);
    return null;
  }

  @Override
  protected void processConCall(ConCallExpression expr, Void params) {
    expr.substSort(mySubstitution);
  }

  @Override
  public Void visitPath(PathExpression expr, Void params) {
    expr.substLevels(mySubstitution);
    return super.visitPath(expr, params);
  }

  @Override
  public Void visitAt(AtExpression expr, Void params) {
    expr.substLevels(mySubstitution);
    return super.visitAt(expr, params);
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    expr.substSort(mySubstitution);
    expr.setSort(expr.getSort().subst(mySubstitution));
    super.visitClassCall(expr, null);
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr, Void params) {
    expr.substSort(mySubstitution);
    super.visitLam(expr, null);
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, Void params) {
    expr.substSort(mySubstitution);
    super.visitPi(expr, null);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Void params) {
    expr.substSort(mySubstitution);
    super.visitSigma(expr, null);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Void params) {
    expr.substSort(mySubstitution);
    super.visitUniverse(expr, null);
    return null;
  }

  @Override
  public Void visitSubst(SubstExpression expr, Void params) {
    expr.getExpression().accept(this, params);
    for (Map.Entry<Binding, Expression> entry : expr.getSubstitution().getEntries()) {
      entry.getValue().accept(this, params);
    }
    expr.levelSubstitution = expr.levelSubstitution.subst(mySubstitution);
    return null;
  }

  @Override
  protected void visitElimTree(ElimTree elimTree, Void params) {
    if (elimTree instanceof BranchElimTree) {
      for (Map.Entry<BranchKey, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        if (entry.getKey() instanceof ClassConstructor) {
          ((ClassConstructor) entry.getKey()).substLevels(mySubstitution);
        }
        visitElimTree(entry.getValue(), null);
      }
    }
  }

  @Override
  public Void visitTypeConstructor(TypeConstructorExpression expr, Void params) {
    expr.substSort(mySubstitution);
    return super.visitTypeConstructor(expr, params);
  }

  @Override
  public Void visitArray(ArrayExpression expr, Void params) {
    expr.substLevels(mySubstitution);
    return super.visitArray(expr, params);
  }
}
