package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseImplicitArgsInference implements ImplicitArgsInference {
  @Override
  public CheckTypeVisitor.OKResult inferTail(CheckTypeVisitor.OKResult fun, Expression expectedType) {
    List<Expression> argTypes = new ArrayList<>();
    Expression actualType = splitActualType(fun.type, expectedType, argTypes);
    CheckTypeVisitor.OKResult result = inferTail(fun.expression, argTypes, actualType, expectedType);
    if (result.equations != null && fun.equations != null) {
      result.equations.addAll(fun.equations);
    }
    return result;
  }

  static public Expression splitActualType(Expression actualType, Expression expectedType, List<Expression> argTypes) {
    // TODO
    return null;
  }
}
