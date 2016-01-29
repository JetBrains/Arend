package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class TailImplicitArgsInference extends BaseImplicitArgsInference {
  protected TailImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  protected CheckTypeVisitor.Result inferTail(Expression fun, List<Expression> argTypes, Expression actualType, Expression expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result fun, Expression expectedType, Abstract.Expression expr) {
    List<DependentLink> actualParams = new ArrayList<>();
    Expression actualType = fun.type.getPiParameters(actualParams, false);
    List<DependentLink> expectedParams = new ArrayList<>(actualParams.size());
    expectedType = expectedType.getPiParameters(expectedParams, false);
    if (expectedParams.size() > actualParams.size()) {
      return null;
    }

    int argsNumber = actualParams.size() - expectedParams.size();
    for (int i = 0; i < expectedParams.size(); ++i) {
      if (expectedParams.get(i).isExplicit() != actualParams.get(argsNumber + i).isExplicit()) {
        return null;
      }
    }

    List<Expression> argTypes = new ArrayList<>(argsNumber);
    for (int i = 0; i < argsNumber; ++i) {
      if (actualParams.get(i).isExplicit()) {
        return null;
      }
      argTypes.add(actualParams.get(i).getType());
    }

    // TODO: add Pi to expectedType and actualType and replace indices with inference variables in actual type, compare them and return result.
    CheckTypeVisitor.Result result = inferTail(fun.expression, argTypes, actualType, expectedType);
    if (result != null) {
      result.equations.add(fun.equations);
    }
    return result;
  }
}
