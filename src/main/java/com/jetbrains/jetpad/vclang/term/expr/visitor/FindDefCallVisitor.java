package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

public class FindDefCallVisitor implements ExpressionVisitor<Void> {
  private final Definition myDef;
  private final List<List<Expression>> myArguments;

  public FindDefCallVisitor(Definition def, List<List<Expression>> arguments) {
    myDef = def;
    myArguments = arguments;
  }

  @Override
  public Void visitApp(AppExpression expr) {
    List<Expression> arguments = new ArrayList<>();
    Expression fun = expr.getFunction(arguments);
    if (fun instanceof BinOpExpression) {
      arguments.add(((BinOpExpression) fun).getRight());
      arguments.add(((BinOpExpression) fun).getLeft());
    }
    if (fun instanceof DefCallExpression && ((DefCallExpression) fun).getDefinition().equals(myDef) || fun instanceof BinOpExpression && ((BinOpExpression) fun).getBinOp().equals(myDef)) {
      myArguments.add(arguments);
    } else {
      fun.accept(this);
    }
    for (Expression argument : arguments) {
      argument.accept(this);
    }
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr) {
    if (expr.getDefinition().equals(myDef)) {
      myArguments.add(new ArrayList<Expression>());
    }
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
  public Void visitBinOp(BinOpExpression expr) {
    List<Expression> arguments = new ArrayList<>();
    if (expr.getBinOp().equals(myDef)) {
      arguments.add(expr.getRight());
      arguments.add(expr.getLeft());
      myArguments.add(arguments);
    }
    expr.getLeft().accept(this);
    expr.getRight().accept(this);
    return null;
  }

  @Override
  public Void visitElim(ElimExpression expr) {
    expr.getExpression().accept(this);
    for (Clause clause : expr.getClauses()) {
      for (Argument argument : clause.getArguments()) {
        if (argument instanceof TypeArgument) {
          ((TypeArgument) argument).getType().accept(this);
        }
      }
      clause.getExpression().accept(this);
    }
    return null;
  }
}
