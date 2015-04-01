package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class Signature {
  private final TypeArgument[] myArguments;
  private final Expression myResultType;

  public Signature(TypeArgument[] arguments, Expression resultType) {
    myArguments = arguments;
    myResultType = resultType;
  }

  public Signature(Expression type) {
    List<TypeArgument> args = new ArrayList<>();
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    while (type instanceof PiExpression) {
      PiExpression pi = (PiExpression)type;
      args.addAll(pi.getArguments());
      type = pi.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
    }
    myArguments = args.toArray(new TypeArgument[args.size()]);
    myResultType = type;
  }

  public TypeArgument[] getArguments() {
    return myArguments;
  }

  public TypeArgument getArgument(int index) {
    return myArguments[index];
  }

  public Expression getResultType() {
    return myResultType;
  }

  public Expression getType() {
    return myArguments.length == 0 ? myResultType : Pi(Arrays.asList(myArguments), myResultType);
  }

  @Override
  public String toString() {
    return getType().toString();
  }
}
