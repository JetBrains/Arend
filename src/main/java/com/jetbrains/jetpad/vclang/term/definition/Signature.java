package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Signature {
  private final List<TypeArgument> myArguments;
  private final Expression myResultType;

  private void addArgs(TypeArgument argument) {
    if (argument instanceof TelescopeArgument) {
      int i = 0;
      for (String name : ((TelescopeArgument) argument).getNames()) {
        myArguments.add(Tele(argument.getExplicit(), vars(name), argument.getType().liftIndex(0, i++)));
      }
    } else {
      myArguments.add(TypeArg(argument.getExplicit(), argument.getType()));
    }
  }

  private void addArgs(List<TypeArgument> arguments) {
    for (TypeArgument argument : arguments) {
      addArgs(argument);
    }
  }

  public Signature(List<TypeArgument> arguments, Expression resultType) {
    myArguments = new ArrayList<>();
    myResultType = resultType;
    addArgs(arguments);
  }

  public Signature(Expression type) {
    myArguments = new ArrayList<>();
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    while (type instanceof PiExpression) {
      PiExpression pi = (PiExpression)type;
      addArgs(pi.getArguments());
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

  /*
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), (byte) 0);
    return builder.toString();
  }

  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    for (Arg argument : myArguments) {
      Tele(argument.isExplicit, vars(argument.name), argument.type).prettyPrint(builder, names, Abstract.VarExpression.PREC);
      builder.append(' ');
    }
    if (myResultType != null) {
      builder.append(": ");
      myResultType.prettyPrint(builder, names, (byte) 0);
    }
  }
  */
}
