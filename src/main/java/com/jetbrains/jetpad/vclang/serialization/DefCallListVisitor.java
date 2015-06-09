package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.HashSet;
import java.util.Set;

public class DefCallListVisitor implements ExpressionVisitor<Void> {
  private final Set<Definition> myDefinitions = new HashSet<>();

  public Set<Definition> getDefinitions() {
    return myDefinitions;
  }

  @Override
  public Void visitApp(AppExpression expr) {
    expr.getFunction().accept(this);
    expr.getArgument().getExpression().accept(this);
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr) {
    myDefinitions.add(expr.getDefinition());
    return null;
  }

  @Override
  public Void visitIndex(IndexExpression expr) {
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr) {
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof TypeArgument) {
        ((TypeArgument) argument).getType().accept(this);
      }
    }
    expr.getBody().accept(this);
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr) {
    for (TypeArgument argument : expr.getArguments()) {
      argument.getType().accept(this);
    }
    expr.getCodomain().accept(this);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr) {
    return null;
  }

  @Override
  public Void visitVar(VarExpression expr) {
    return null;
  }

  @Override
  public Void visitInferHole(InferHoleExpression expr) {
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr) {
    if (expr.getExpr() != null) {
      expr.getExpr().accept(this);
    }
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr) {
    for (Expression field : expr.getFields()) {
      field.accept(this);
    }
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr) {
    for (TypeArgument argument : expr.getArguments()) {
      argument.getType().accept(this);
    }
    return null;
  }

  @Override
  public Void visitElim(ElimExpression expr) {
    expr.getExpression().accept(this);
    for (Clause clause : expr.getClauses()) {
      visitClause(clause);
    }
    if (expr.getOtherwise() != null) {
      visitClause(expr.getOtherwise());
    }
    return null;
  }

  private void visitClause(Clause clause) {
    for (Argument argument : clause.getArguments()) {
      if (argument instanceof TypeArgument) {
        ((TypeArgument) argument).getType().accept(this);
      }
    }
    clause.getExpression().accept(this);
  }

  @Override
  public Void visitFieldAcc(FieldAccExpression expr) {
    if (expr.getDefinition() != null) {
      myDefinitions.add(expr.getDefinition());
    }
    expr.getExpression().accept(this);
    return null;
  }
}
