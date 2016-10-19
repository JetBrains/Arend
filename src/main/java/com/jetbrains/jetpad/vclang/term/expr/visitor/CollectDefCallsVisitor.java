package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;
import java.util.Set;

public class CollectDefCallsVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final Set<Abstract.Definition> myDependencies;

  public CollectDefCallsVisitor(Set<Abstract.Definition> dependencies) {
    myDependencies = dependencies;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void ignore) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitAppLevel(Abstract.ApplyLevelExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Void ignore) {
    if (expr.getReferent() != null) {
      myDependencies.add(expr.getReferent());
    } else if (expr.getExpression() != null) {
      expr.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    if (expr.getModule() != null)
      myDependencies.add(expr.getModule());
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void ignore) {
    visitArguments(expr.getArguments());
    expr.getBody().accept(this, null);
    return null;
  }

  private void visitArguments(List<? extends Abstract.Argument> args) {
    for (Abstract.Argument arg : args) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(this, null);
      }
    }
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void ignore) {
    visitArguments(expr.getArguments());
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitPolyUniverse(Abstract.PolyUniverseExpression expr, Void ignore) {
    expr.getPLevel().accept(this, null);
    return expr.getHLevel().accept(this, null);
  }

  @Override
  public Void visitTypeOmega(Abstract.TypeOmegaExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Void ignore) {
    for (Abstract.Expression comp : expr.getFields()) {
      comp.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Void ignore) {
    visitArguments(expr.getArguments());
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Void ignore) {
    if (expr.getResolvedBinOp() != null) {
      myDependencies.add(expr.getResolvedBinOp());
    }
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void ignore) {
    expr.getLeft().accept(this, null);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      visitDefCall(elem.binOp, null);
      elem.argument.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Void ignore) {
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void ignore) {
    for (Abstract.Expression caseExpr : expr.getExpressions()) {
      caseExpr.accept(this, null);
    }
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Void ignore) {
    expr.getBaseClassExpression().accept(this, null);
    for (Abstract.ImplementStatement statement : expr.getStatements()) {
      statement.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Abstract.LetExpression letExpression, Void ignore) {
    for (Abstract.LetClause clause : letExpression.getClauses()) {
      visitArguments(clause.getArguments());
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, null);
      }
      clause.getTerm().accept(this, null);
    }
    letExpression.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Abstract.NumericLiteral expr, Void ignore) {
    return null;
  }
}
