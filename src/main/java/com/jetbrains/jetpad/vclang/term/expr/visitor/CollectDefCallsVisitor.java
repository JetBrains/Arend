package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectDefCallsVisitor implements AbstractExpressionVisitor<Void, Set<Referable>> {
  private final Set<Referable> myDependencies = new HashSet<>();

  @Override
  public Set<Referable> visitApp(Abstract.AppExpression expr, Void ignore) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<Referable> visitDefCall(Abstract.DefCallExpression expr, Void ignore) {
    if (expr.getReferent() != null) {
      myDependencies.add(expr.getReferent());
    } else if (expr.getExpression() != null) {
      expr.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<Referable> visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    if (expr.getModule() != null)
      myDependencies.add(expr.getModule());
    return myDependencies;
  }

  @Override
  public Set<Referable> visitLam(Abstract.LamExpression expr, Void ignore) {
    visitArguments(expr.getArguments());
    expr.getBody().accept(this, null);
    return myDependencies;
  }

  private void visitArguments(List<? extends Abstract.Argument> args) {
    for (Abstract.Argument arg : args) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(this, null);
      }
    }
  }

  @Override
  public Set<Referable> visitPi(Abstract.PiExpression expr, Void ignore) {
    visitArguments(expr.getArguments());
    expr.getCodomain().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<Referable> visitUniverse(Abstract.UniverseExpression expr, Void ignore) {
    return myDependencies;
  }

  @Override
  public Set<Referable> visitPolyUniverse(Abstract.PolyUniverseExpression expr, Void ignore) {
    expr.getPLevel().accept(this, null);
    return expr.getHLevel().accept(this, null);
  }

  @Override
  public Set<Referable> visitInferHole(Abstract.InferHoleExpression expr, Void ignore) {
    return myDependencies;
  }

  @Override
  public Set<Referable> visitError(Abstract.ErrorExpression expr, Void ignore) {
    return myDependencies;
  }

  @Override
  public Set<Referable> visitTuple(Abstract.TupleExpression expr, Void ignore) {
    for (Abstract.Expression comp : expr.getFields()) {
      comp.accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<Referable> visitSigma(Abstract.SigmaExpression expr, Void ignore) {
    visitArguments(expr.getArguments());
    return myDependencies;
  }

  @Override
  public Set<Referable> visitBinOp(Abstract.BinOpExpression expr, Void ignore) {
    if (expr.getResolvedBinOp() != null) {
      myDependencies.add(expr.getResolvedBinOp());
    }
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<Referable> visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void ignore) {
    expr.getLeft().accept(this, null);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      visitDefCall(elem.binOp, null);
      elem.argument.accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<Referable> visitElim(Abstract.ElimExpression expr, Void ignore) {
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<Referable> visitCase(Abstract.CaseExpression expr, Void ignore) {
    for (Abstract.Expression caseExpr : expr.getExpressions()) {
      caseExpr.accept(this, null);
    }
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<Referable> visitProj(Abstract.ProjExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<Referable> visitClassExt(Abstract.ClassExtExpression expr, Void ignore) {
    expr.getBaseClassExpression().accept(this, null);
    for (Abstract.ImplementStatement statement : expr.getStatements()) {
      statement.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<Referable> visitNew(Abstract.NewExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<Referable> visitLet(Abstract.LetExpression letExpression, Void ignore) {
    for (Abstract.LetClause clause : letExpression.getClauses()) {
      visitArguments(clause.getArguments());
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, null);
      }
      clause.getTerm().accept(this, null);
    }
    letExpression.getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<Referable> visitNumericLiteral(Abstract.NumericLiteral expr, Void ignore) {
    return myDependencies;
  }
}
