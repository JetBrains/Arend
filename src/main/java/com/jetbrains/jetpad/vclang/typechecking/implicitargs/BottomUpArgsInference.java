package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.List;

public class BottomUpArgsInference extends BaseImplicitArgsInference {
  @Override
  public CheckTypeVisitor.OKResult infer(CheckTypeVisitor.OKResult fun, List<Abstract.ArgumentExpression> args, Expression expectedType) {
    // TODO
    return null;
  }

  @Override
  public CheckTypeVisitor.OKResult inferTail(Expression fun, List<Expression> argTypes, Expression actualType, Expression expectedType) {
    // TODO
    return null;
  }
}
