package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public abstract class TailImplicitArgsInference extends BaseImplicitArgsInference {
  protected TailImplicitArgsInference(CheckTypeVisitor visitor) {
    super(visitor);
  }

  protected CheckTypeVisitor.Result inferTail(Expression fun, List<Expression> argTypes, Expression actualType, Expression expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.OKResult fun, Expression expectedType, Abstract.Expression expr) {
    List<TypeArgument> actualArgs = new ArrayList<>();
    Expression actualType = splitArguments(fun.type, actualArgs, myVisitor.getLocalContext());
    List<TypeArgument> expectedArgs = new ArrayList<>(actualArgs.size());
    expectedType = splitArguments(expectedType, expectedArgs, myVisitor.getLocalContext());
    if (expectedArgs.size() > actualArgs.size()) {
      return null;
    }

    int argsNumber = actualArgs.size() - expectedArgs.size();
    for (int i = 0; i < expectedArgs.size(); ++i) {
      if (expectedArgs.get(i).getExplicit() != actualArgs.get(argsNumber + i).getExplicit()) {
        return null;
      }
    }

    List<Expression> argTypes = new ArrayList<>(argsNumber);
    for (int i = 0; i < argsNumber; ++i) {
      if (actualArgs.get(i).getExplicit()) {
        return null;
      }
      argTypes.add(actualArgs.get(i).getType());
    }

    // TODO: add Pi to expectedType and actualType and replace indices with inference variables in actual type, compare them and return result.
    CheckTypeVisitor.Result result = inferTail(fun.expression, argTypes, actualType, expectedType);
    if (result.equations != null && fun.equations != null) {
      result.equations.addAll(fun.equations);
    }
    return result;
  }
}
