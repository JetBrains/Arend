package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseImplicitArgsInference implements ImplicitArgsInference {
  protected final CheckTypeVisitor myVisitor;

  protected BaseImplicitArgsInference(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  protected CheckTypeVisitor.Result inferTail(Expression fun, List<Expression> argTypes, Expression actualType, Expression expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.OKResult fun, Expression expectedType, Abstract.Expression expr) {
    List<Expression> argTypes = new ArrayList<>();
    Expression actualType = splitActualType(fun.type, expectedType, argTypes);
    CheckTypeVisitor.Result result = inferTail(fun.expression, argTypes, actualType, expectedType);
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
