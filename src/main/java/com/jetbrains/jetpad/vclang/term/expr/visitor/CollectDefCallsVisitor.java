package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.BaseDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectDefCallsVisitor implements AbstractExpressionVisitor<Void, Set<BaseDefinition>> {
  private final Set<BaseDefinition> myDependencies = new HashSet<>();

  @Override
  public Set<BaseDefinition> visitApp(Abstract.AppExpression expr, Void ignore) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitDefCall(Abstract.DefCallExpression expr, Void ignore) {
    if (expr.getResolvedDefinition() != null) {
      myDependencies.add(expr.getResolvedDefinition());
    } else if (expr.getExpression() != null) {
      expr.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    if (expr.getModule() != null)
      myDependencies.add(expr.getModule());
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitLam(Abstract.LamExpression expr, Void ignore) {
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
  public Set<BaseDefinition> visitPi(Abstract.PiExpression expr, Void ignore) {
    visitArguments(expr.getArguments());
    expr.getCodomain().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitUniverse(Abstract.UniverseExpression expr, Void ignore) {
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitPolyUniverse(Abstract.PolyUniverseExpression expr, Void ignore) {
    expr.getPLevel().accept(this, null);
    return expr.getHLevel().accept(this, null);
  }

  @Override
  public Set<BaseDefinition> visitInferHole(Abstract.InferHoleExpression expr, Void ignore) {
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitError(Abstract.ErrorExpression expr, Void ignore) {
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitTuple(Abstract.TupleExpression expr, Void ignore) {
    for (Abstract.Expression comp : expr.getFields()) {
      comp.accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitSigma(Abstract.SigmaExpression expr, Void ignore) {
    visitArguments(expr.getArguments());
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitBinOp(Abstract.BinOpExpression expr, Void ignore) {
    if (expr.getResolvedBinOp() != null) {
      myDependencies.add(expr.getResolvedBinOp());
    }
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void ignore) {
    expr.getLeft().accept(this, null);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      visitDefCall(elem.binOp, null);
      elem.argument.accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitElim(Abstract.ElimExpression expr, Void ignore) {
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitCase(Abstract.CaseExpression expr, Void ignore) {
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
  public Set<BaseDefinition> visitProj(Abstract.ProjExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitClassExt(Abstract.ClassExtExpression expr, Void ignore) {
    expr.getBaseClassExpression().accept(this, null);
    for (Abstract.ImplementStatement statement : expr.getStatements()) {
      statement.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitNew(Abstract.NewExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<BaseDefinition> visitLet(Abstract.LetExpression letExpression, Void ignore) {
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
  public Set<BaseDefinition> visitNumericLiteral(Abstract.NumericLiteral expr, Void ignore) {
    return myDependencies;
  }
}
