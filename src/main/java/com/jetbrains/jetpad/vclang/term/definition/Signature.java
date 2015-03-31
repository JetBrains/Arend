package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
    Expression type = myResultType;
    List<ArrayList<TypeArgument>> arguments = new ArrayList<>();
    ArrayList<TypeArgument> list = new ArrayList<>();
    for (TypeArgument argument : myArguments) {
      if (argument instanceof TelescopeArgument) {
        list.add(argument);
      } else {
        if (!list.isEmpty()) {
          arguments.add(list);
          list = new ArrayList<>();
        }
        list.add(argument);
        arguments.add(list);
        list = new ArrayList<>();
      }
    }
    ListIterator<ArrayList<TypeArgument>> it = arguments.listIterator(arguments.size());
    while (it.hasPrevious()) {
      type = new PiExpression(it.previous(), type);
    }
    return type;
  }

  @Override
  public String toString() {
    return getType().toString();
  }
}
