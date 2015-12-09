package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectDefCallsVisitor implements AbstractExpressionVisitor<Boolean, Set<ResolvedName>> {
  private final Set<ResolvedName> myDependencies = new HashSet<>();

  @Override
  public Set<ResolvedName> visitApp(Abstract.AppExpression expr, Boolean countTypeChecked) {
    expr.getFunction().accept(this, countTypeChecked);
    expr.getArgument().getExpression().accept(this, countTypeChecked);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitDefCall(Abstract.DefCallExpression expr, Boolean countTypeChecked) {
    if (expr.getResolvedName() != null) {
      if (countTypeChecked || expr.getResolvedName().toDefinition() == null)
        myDependencies.add(expr.getResolvedName());
    } else if (expr.getExpression() != null) {
        expr.getExpression().accept(this, countTypeChecked);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitIndex(Abstract.IndexExpression expr, Boolean countTypeChecked) {
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitLam(Abstract.LamExpression expr, Boolean countTypeChecked) {
    visitArguments(expr.getArguments(), countTypeChecked);
    expr.getBody().accept(this, countTypeChecked);
    return myDependencies;
  }

  private void visitArguments(List<? extends Abstract.Argument> args, Boolean countTypeChecked) {
    for (Abstract.Argument arg : args) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(this, countTypeChecked);
      }
    }
  }

  @Override
  public Set<ResolvedName> visitPi(Abstract.PiExpression expr, Boolean countTypeChecked) {
    visitArguments(expr.getArguments(), countTypeChecked);
    expr.getCodomain().accept(this, countTypeChecked);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitUniverse(Abstract.UniverseExpression expr, Boolean countTypeChecked) {
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitInferHole(Abstract.InferHoleExpression expr, Boolean countTypeChecked) {
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitError(Abstract.ErrorExpression expr, Boolean countTypeChecked) {
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitTuple(Abstract.TupleExpression expr, Boolean countTypeChecked) {
    for (Abstract.Expression comp : expr.getFields()) {
      comp.accept(this, countTypeChecked);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitSigma(Abstract.SigmaExpression expr, Boolean countTypeChecked) {
    visitArguments(expr.getArguments(), countTypeChecked);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitBinOp(Abstract.BinOpExpression expr, Boolean countTypeChecked) {
    if (countTypeChecked || expr.getResolvedBinOpName().toDefinition() == null) {
      myDependencies.add(expr.getResolvedBinOpName());
    }
    expr.getLeft().accept(this, countTypeChecked);
    expr.getRight().accept(this, countTypeChecked);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Boolean countTypeChecked) {
    expr.getLeft().accept(this, countTypeChecked);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      visitDefCall(elem.binOp, countTypeChecked);
      elem.argument.accept(this, countTypeChecked);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitElim(Abstract.ElimExpression expr, Boolean countTypeChecked) {
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, countTypeChecked);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitCase(Abstract.CaseExpression expr, Boolean countTypeChecked) {
    for (Abstract.Expression caseExpr : expr.getExpressions()) {
      caseExpr.accept(this, countTypeChecked);
    }
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, countTypeChecked);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitProj(Abstract.ProjExpression expr, Boolean countTypeChecked) {
    expr.getExpression().accept(this, countTypeChecked);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitClassExt(Abstract.ClassExtExpression expr, Boolean countTypeChecked) {
    expr.getBaseClassExpression().accept(this, countTypeChecked);
    for (Abstract.ImplementStatement statement : expr.getStatements()) {
      statement.getExpression().accept(this, countTypeChecked);
    }
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitNew(Abstract.NewExpression expr, Boolean countTypeChecked) {
    expr.getExpression().accept(this, countTypeChecked);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitLet(Abstract.LetExpression letExpression, Boolean countTypeChecked) {
    for (Abstract.LetClause clause : letExpression.getClauses()) {
      visitArguments(clause.getArguments(), countTypeChecked);
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, countTypeChecked);
      }
      clause.getTerm().accept(this, countTypeChecked);
    }
    letExpression.getExpression().accept(this, countTypeChecked);
    return myDependencies;
  }

  @Override
  public Set<ResolvedName> visitNumericLiteral(Abstract.NumericLiteral expr, Boolean countTypeChecked) {
    return myDependencies;
  }
}
