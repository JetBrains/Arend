package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

public class InferenceLevelVariable implements Variable {
  private final String myName;
  private final Abstract.SourceNode mySourceNode;
  private Type myType;

  public InferenceLevelVariable(String name, Expression type, Abstract.SourceNode sourceNode) {
    myName = name;
    myType = type;
    mySourceNode = sourceNode;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public Type getType() {
    return myType;
  }

  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public String toString() {
    return "?" + myName;
  }
}
