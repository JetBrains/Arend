package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class Signature implements PrettyPrintable {
  private final List<TypeArgument> myArguments;
  private final Expression myResultType;

  public Signature(List<TypeArgument> arguments, Expression resultType) {
    myArguments = arguments;
    myResultType = resultType;
  }

  public Signature(Expression type) {
    myArguments = new ArrayList<>();
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    while (type instanceof PiExpression) {
      PiExpression pi = (PiExpression)type;
      myArguments.addAll(pi.getArguments());
      type = pi.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
    }
    myResultType = type;
  }

  public List<TypeArgument> getArguments() {
    return myArguments;
  }

  public TypeArgument getArgument(int index) {
    return myArguments.get(index);
  }

  public Expression getResultType() {
    return myResultType;
  }

  public Expression getType() {
    return myArguments.size() == 0 ? myResultType : Pi(myArguments, myResultType);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), (byte) 0);
    return builder.toString();
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    for (TypeArgument argument : myArguments) {
      argument.prettyPrint(builder, names, Abstract.VarExpression.PREC);
      builder.append(' ');
    }
    builder.append(": ");
    myResultType.prettyPrint(builder, names, (byte) 0);
  }
}
