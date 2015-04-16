package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Universe;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeFromList;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public final class FunctionDefinition extends Definition {
  private final Expression myTerm;
  private final Arrow myArrow;
  private final List<TelescopeArgument> myArguments;
  private final Expression myResultType;

  protected FunctionDefinition(int id, String name, Precedence precedence, Fixity fixity, List<TelescopeArgument> arguments, Expression resultType, Arrow arrow, Expression term) {
    super(id, name, precedence, fixity, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public FunctionDefinition(String name, Precedence precedence, Fixity fixity, List<TelescopeArgument> arguments, Expression resultType, Arrow arrow, Expression term) {
    super(name, precedence, fixity, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public FunctionDefinition(String name, Fixity fixity, List<TelescopeArgument> arguments, Expression resultType, Arrow arrow, Expression term) {
    super(name, fixity, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public FunctionDefinition(String name, List<TelescopeArgument> arguments, Expression resultType, Arrow arrow, Expression term) {
    super(name, null);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTerm = term;
  }

  public Arrow getArrow() {
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
      myResultType.prettyPrint(builder, names, (byte) 0);
    }
    builder.append(myArrow == Arrow.RIGHT ? " => " : " <= ");
    myTerm.prettyPrint(builder, names, (byte) 0);
    removeFromList(names, myArguments);
  }

  @Override
  public FunctionDefinition checkTypes(Map<String, Definition> globalContext, List<Binding> localContext, List<TypeCheckingError> errors) {
    List<TelescopeArgument> arguments = new ArrayList<>(myArguments.size());
    int origSize = localContext.size();
    for (TelescopeArgument argument : myArguments) {
      CheckTypeVisitor.OKResult result = argument.getType().checkType(globalContext, localContext, Universe(), errors);
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }

      arguments.add(Tele(argument.getExplicit(), argument.getNames(), result.expression));
      for (String name : argument.getNames()) {
        localContext.add(new TypedBinding(name, result.expression));
      }
    }

    Expression expectedType;
    if (myResultType != null) {
      CheckTypeVisitor.OKResult typeResult = myResultType.checkType(globalContext, localContext, Universe(), errors);
      if (typeResult == null) {
        trimToSize(localContext, origSize);
        return null;
      }
      expectedType = typeResult.expression;
    } else {
      expectedType = null;
    }

    CheckTypeVisitor.OKResult termResult = myTerm.checkType(globalContext, localContext, expectedType, errors);
    trimToSize(localContext, origSize);
    return termResult == null ? null : new FunctionDefinition(myID, getName(), getPrecedence(), getFixity(), arguments, termResult.type, myArrow, termResult.expression);
  }

  @Override
  public Expression getType() {
    return myArguments.isEmpty() ? myResultType : Pi(new ArrayList<TypeArgument>(myArguments), myResultType);
  }
}
