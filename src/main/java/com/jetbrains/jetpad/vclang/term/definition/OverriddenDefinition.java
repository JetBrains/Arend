package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;

import java.util.List;

public class OverriddenDefinition extends FunctionDefinition {
  private FunctionDefinition myOverriddenFunction;

  public OverriddenDefinition(String name, Definition parent, Precedence precedence, Fixity fixity, Arrow arrow) {
    super(name, parent, precedence, fixity, arrow);
    myOverriddenFunction = null;
  }

  public OverriddenDefinition(String name, Definition parent, Precedence precedence, Fixity fixity, List<Argument> arguments, Expression resultType, Arrow arrow, Expression term, FunctionDefinition overriddenFunction) {
    super(name, parent, precedence, fixity, arguments, resultType, arrow, term);
    myOverriddenFunction = overriddenFunction;
  }

  public FunctionDefinition getOverriddenFunction() {
    return myOverriddenFunction;
  }

  public void setOverriddenFunction(FunctionDefinition overriddenFunction) {
    myOverriddenFunction = overriddenFunction;
  }

  @Override
  public boolean isOverridden() {
    return true;
  }
}
