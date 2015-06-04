package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class LiftIndexVisitor implements ExpressionVisitor<Expression> {
  private final int myFrom;
  private final int myOn;

  public LiftIndexVisitor(int from, int on) {
    myFrom = from;
    myOn = on;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    return Apps(expr.getFunction().accept(this), new ArgumentExpression(expr.getArgument().getExpression().accept(this), expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return expr;
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    if (expr.getIndex() < myFrom) return expr;
    if (expr.getIndex() + myOn >= myFrom) return Index(expr.getIndex() + myOn);
    throw new NegativeIndexException();
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    int from = myFrom;
    List<Argument> arguments = new ArrayList<>(expr.getArguments().size());
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof NameArgument) {
        arguments.add(argument);
        ++from;
      } else
      if (argument instanceof TelescopeArgument) {
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        arguments.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), teleArgument.getType().liftIndex(from, myOn)));
        from += teleArgument.getNames().size();
      } else {
        throw new IllegalStateException();
      }
    }
    return Lam(arguments, expr.getBody().liftIndex(from, myOn));
  }

  private int visitArguments(List<TypeArgument> arguments, List<TypeArgument> result) {
    int from = myFrom;
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        result.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), teleArgument.getType().liftIndex(from, myOn)));
        from += teleArgument.getNames().size();
      } else {
        result.add(new TypeArgument(argument.getExplicit(), argument.getType().liftIndex(from, myOn)));
        ++from;
      }
    }
    return from;
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    int from = visitArguments(expr.getArguments(), result);
    return Pi(result, expr.getCodomain().liftIndex(from, myOn));
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr) {
    return expr;
  }

  @Override
  public Expression visitVar(VarExpression expr) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr) {
    return expr.getExpr() == null ? expr : new ErrorExpression(expr.accept(this), expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr) {
    return expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this));
    }
    return Tuple(fields);
  }

  @Override
  public Expression visitSigma(SigmaExpression expr) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    visitArguments(expr.getArguments(), result);
    return Sigma(result);
  }

  @Override
  public Expression visitElim(ElimExpression expr) {
    throw new IllegalStateException();
  }

  @Override
  public Expression visitFieldAcc(FieldAccExpression expr) {
    return FieldAcc(expr.getExpression().accept(this), expr.getDefinition(), expr.getIndex());
  }

  public static class NegativeIndexException extends RuntimeException {}
}
