package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.List;

public interface ImplicitArgsInference {
  CheckTypeVisitor.Result infer(CheckTypeVisitor.OKResult fun, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression funExpr, Abstract.Expression expr);
  CheckTypeVisitor.OKResult inferTail(CheckTypeVisitor.OKResult fun, Expression expectedType);
  CheckTypeVisitor.OKResult inferTail(Expression fun, List<Expression> argTypes, Expression actualType, Expression expectedType);
}
