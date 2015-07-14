package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;

public class FunctionDefinition extends Definition implements Abstract.FunctionDefinition {
  private Arrow myArrow;
  private List<Argument> myArguments;
  private Expression myResultType;
  private Expression myTerm;
  private boolean myTypeHasErrors;

  public FunctionDefinition(String name, Definition parent, Precedence precedence, Fixity fixity, Arrow arrow) {
    super(name, parent, precedence, fixity);
    myArrow = arrow;
    myTypeHasErrors = true;
  }

  public FunctionDefinition(String name, Definition parent, Precedence precedence, Fixity fixity, List<Argument> arguments, Expression resultType, Arrow arrow, Expression term) {
    super(name, parent, precedence, fixity);
    setUniverse(new Universe.Type(0, Universe.Type.PROP));
    hasErrors(false);
    myArguments = arguments;
    myResultType = resultType;
    myArrow = arrow;
    myTypeHasErrors = false;
    myTerm = term;
  }

  @Override
  public Arrow getArrow() {
    return myArrow;
  }

  public void setArrow(Arrow arrow) {
    myArrow = arrow;
  }

  @Override
  public boolean isAbstract() {
    return myArrow == null;
  }

  @Override
  public boolean isOverridden() {
    return false;
  }

  @Override
  public Expression getTerm() {
    return myTerm;
  }

  public void setTerm(Expression term) {
    myTerm = term;
  }

  @Override
  public List<Argument> getArguments() {
    return myArguments;
  }

  public void setArguments(List<Argument> arguments) {
    myArguments = arguments;
  }

  @Override
  public Expression getResultType() {
    return myResultType;
  }

  public void setResultType(Expression resultType) {
    myResultType = resultType;
  }

  public boolean typeHasErrors() {
    return myTypeHasErrors;
  }

  public void typeHasErrors(boolean has) {
    myTypeHasErrors = has;
  }

  @Override
  public Expression getType() {
    if (typeHasErrors() || myResultType == null) return null;
    if (myArguments.isEmpty()) return myResultType;
    List<TypeArgument> arguments = new ArrayList<>(myArguments.size());
    for (Argument argument : myArguments) {
      arguments.add((TypeArgument) argument);
    }
    return Pi(arguments, myResultType);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFunction(this, params);
  }
}
