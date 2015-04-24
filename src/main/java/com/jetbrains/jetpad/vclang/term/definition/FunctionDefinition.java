package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeFromList;

public class FunctionDefinition extends Definition {
  private final Expression myTerm;
  private final Abstract.Definition.Arrow myArrow;
  private final List<TelescopeArgument> myArguments;
  private final Expression myResultType;

  protected FunctionDefinition(int id, String name, Precedence precedence, Fixity fixity, List<TelescopeArgument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
    super(id, name, precedence, fixity, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public FunctionDefinition(String name, Precedence precedence, Fixity fixity, List<TelescopeArgument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
    super(name, precedence, fixity, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public FunctionDefinition(String name, Fixity fixity, List<TelescopeArgument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
    super(name, fixity, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public FunctionDefinition(String name, List<TelescopeArgument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
    super(name, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public Abstract.Definition.Arrow getArrow() {
    return myArrow;
  }

  public Expression getTerm() {
    return myTerm;
  }

  public List<TelescopeArgument> getArguments() {
    return myArguments;
  }

  public TelescopeArgument getArgument(int index) {
    return myArguments.get(index);
  }

  public Expression getResultType() {
    return myResultType;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    builder.append("\\function\n");
    if (getFixity() == Fixity.PREFIX) {
      builder.append(getName());
    } else {
      builder.append('(').append(getName()).append(')');
    }
    for (TelescopeArgument argument : myArguments) {
      builder.append(' ');
      argument.prettyPrint(builder, names, Abstract.VarExpression.PREC);
    }
    if (myResultType != null) {
      builder.append(" : ");
      myResultType.prettyPrint(builder, names, Abstract.Expression.PREC);
    }
    builder.append(myArrow == Abstract.Definition.Arrow.RIGHT ? " => " : " <= ");
    myTerm.prettyPrint(builder, names, Abstract.Expression.PREC);
    removeFromList(names, myArguments);
  }

  @Override
  public Expression getType() {
    return myArguments.isEmpty() ? myResultType : Pi(new ArrayList<TypeArgument>(myArguments), myResultType);
  }
}
