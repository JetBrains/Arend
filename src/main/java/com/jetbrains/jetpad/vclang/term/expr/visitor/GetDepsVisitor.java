package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetDepsVisitor implements AbstractExpressionVisitor<Void, Set<ResolvedName>> {
  private final Set<ResolvedName> myDependencies = new HashSet<>();

  @Override
  public Set<ResolvedName> visitApp(Abstract.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitDefCall(Abstract.DefCallExpression expr, Void params) {
    if (expr.getResolvedName() != null) {
      if (expr.getResolvedName().toDefinition() == null)
        myDependencies.add(expr.getResolvedName());
    } else if (expr.getExpression() != null) {
        expr.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitIndex(Abstract.IndexExpression expr, Void params) {
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitLam(Abstract.LamExpression expr, Void params) {
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
  public Set<ResolvedName> visitPi(Abstract.PiExpression expr, Void params) {
    visitArguments(expr.getArguments());
    expr.getCodomain().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitUniverse(Abstract.UniverseExpression expr, Void params) {
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitInferHole(Abstract.InferHoleExpression expr, Void params) {
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitError(Abstract.ErrorExpression expr, Void params) {
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitTuple(Abstract.TupleExpression expr, Void params) {
    for (Abstract.Expression comp : expr.getFields()) {
      comp.accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitSigma(Abstract.SigmaExpression expr, Void params) {
    visitArguments(expr.getArguments());
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitBinOp(Abstract.BinOpExpression expr, Void params) {
    if (expr.getResolvedBinOpName().toDefinition() == null) {
      myDependencies.add(expr.getResolvedBinOpName());
    }
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void params) {
    expr.getLeft().accept(this, null);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      visitDefCall(elem.binOp, null);
      elem.argument.accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitElim(Abstract.ElimExpression expr, Void params) {
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitCase(Abstract.CaseExpression expr, Void params) {
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
  public Set<ResolvedName> visitProj(Abstract.ProjExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitClassExt(Abstract.ClassExtExpression expr, Void params) {
    expr.getBaseClassExpression().accept(this, null);
    for (Abstract.ImplementStatement statement : expr.getStatements()) {
      statement.getExpression().accept(this, null);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitNew(Abstract.NewExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitLet(Abstract.LetExpression letExpression, Void params) {
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
  public Set<ResolvedName> visitNumericLiteral(Abstract.NumericLiteral expr, Void params) {
    return myDependencies;
  }
}
